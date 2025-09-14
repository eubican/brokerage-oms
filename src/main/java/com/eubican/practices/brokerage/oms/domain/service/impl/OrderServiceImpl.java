package com.eubican.practices.brokerage.oms.domain.service.impl;

import com.eubican.practices.brokerage.oms.config.properties.OrderServiceProperties;
import com.eubican.practices.brokerage.oms.domain.exception.OrderNotCancellableException;
import com.eubican.practices.brokerage.oms.domain.exception.ResourceNotFoundException;
import com.eubican.practices.brokerage.oms.domain.model.Asset;
import com.eubican.practices.brokerage.oms.domain.model.Order;
import com.eubican.practices.brokerage.oms.domain.model.OrderSide;
import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import com.eubican.practices.brokerage.oms.domain.service.AssetService;
import com.eubican.practices.brokerage.oms.domain.service.OrderService;
import com.eubican.practices.brokerage.oms.persistence.entity.CustomerEntity;
import com.eubican.practices.brokerage.oms.persistence.entity.OrderEntity;
import com.eubican.practices.brokerage.oms.persistence.repository.CustomerJpaRepository;
import com.eubican.practices.brokerage.oms.persistence.repository.OrderJpaRepository;
import com.eubican.practices.brokerage.oms.persistence.repository.helper.OrderSpecifications;
import com.eubican.practices.brokerage.oms.security.AuthorizationGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class OrderServiceImpl implements OrderService {

    private final OrderJpaRepository orderRepository;

    private final AssetService assetService;

    private final AuthorizationGuard authorizationGuard;

    private final CustomerJpaRepository customerRepository;

    private final OrderServiceProperties orderServiceProperties;

    @Override
    @Transactional
    public Order createOrder(Order order) {
        retrying(() -> {
            if (OrderSide.BUY == order.getSide()) {
                BigDecimal neededTRY = order.getPrice().multiply(order.getSize());
                Asset cash = assetService.retrieveCustomerAsset(order.getCustomerId(), "TRY");

                if (cash.hasInsufficientFunds(neededTRY)) {
                    log.warn("Insufficient TRY usable balance to place BUY for customer {}", order.getCustomerId());
                    throw new IllegalArgumentException("Insufficient TRY usable balance to place BUY");
                }

                cash.setUsable(cash.getUsable().subtract(neededTRY));
                cash.setReserved(cash.getReserved().add(neededTRY));
                assetService.upsertAsset(cash);
            } else {
                Asset asset = assetService.retrieveCustomerAsset(order.getCustomerId(), order.getAssetName());

                if (asset.hasInsufficientFunds(order.getSize())) {
                    log.warn("Insufficient {} usable balance to place SELL for customer {}", order.getAssetName(), order.getCustomerId());
                    throw new IllegalArgumentException("Insufficient " + order.getAssetName() + " usable balance to place SELL");
                }

                asset.setUsable(asset.getUsable().subtract(order.getSize()));
                asset.setReserved(asset.getReserved().add(order.getSize()));
                assetService.upsertAsset(asset);
            }
        });

        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        CustomerEntity ref = customerRepository.getReferenceById(order.getCustomerId());
        entity.setCustomer(ref);
        entity.setAssetName(order.getAssetName());
        entity.setSide(order.getSide());
        entity.setSize(order.getSize());
        entity.setPrice(order.getPrice());
        entity.setStatus(order.getStatus());
        entity.setCreatedAt(order.getCreatedAt());

        orderRepository.save(entity);
        log.debug("Order {} created for customer: {}", order.getId(), order.getCustomerId());

        return order;
    }

    @Override
    @Transactional
    public void cancelOrder(UUID orderID) {
        OrderEntity entity = orderRepository.findById(orderID)
                .orElseThrow(() -> {
                    log.warn("Order {} not found", orderID);
                    return new ResourceNotFoundException(String.format("Order %s not found", orderID));
                });

        // Enforce authorization: only admin or owner can cancel
        authorizationGuard.checkCustomerAccess(entity.getCustomer().getId());

        if (OrderStatus.PENDING != entity.getStatus()) {
            log.warn("Order {} cannot be canceled because it is in status {}", orderID, entity.getStatus());
            throw new OrderNotCancellableException("Only PENDING orders can be canceled");
        }

        retrying(() -> {
            if (OrderSide.BUY == entity.getSide()) {
                BigDecimal amountTRY = entity.getPrice().multiply(entity.getSize());
                Asset cash = assetService.retrieveCustomerAsset(entity.getCustomer().getId(), "TRY");

                if (cash.verifyReserved(amountTRY)) {
                    log.warn("Inconsistent TRY reserved balance to cancel BUY for customer {}", entity.getCustomer().getId());
                    throw new IllegalArgumentException("Inconsistent TRY reserved balance to cancel BUY");
                }

                cash.setReserved(cash.getReserved().subtract(amountTRY));
                cash.setUsable(cash.getUsable().add(amountTRY));
                assetService.upsertAsset(cash);
            } else {
                Asset asset = assetService.retrieveCustomerAsset(entity.getCustomer().getId(), entity.getAssetName());

                if (asset.verifyReserved(entity.getSize())) {
                    log.warn("Inconsistent {} reserved balance to cancel SELL for customer {}", entity.getAssetName(), entity.getCustomer().getId());
                    throw new IllegalArgumentException("Inconsistent " + entity.getAssetName() + " reserved balance to cancel SELL");
                }

                asset.setReserved(asset.getReserved().subtract(entity.getSize()));
                asset.setUsable(asset.getUsable().add(entity.getSize()));
                assetService.upsertAsset(asset);
            }
        });

        entity.setStatus(OrderStatus.CANCELED);
        // we are not saving explicitly because hibernate dirty checking will automatically update it on txn commit
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> fetchOrders(UUID customerId,
                                   Instant from,
                                   Instant to,
                                   OrderStatus status,
                                   String assetName,
                                   Pageable pageable
    ) {
        var spec = OrderSpecifications.byFilters(customerId, from, to, status, assetName);
        Page<OrderEntity> page = orderRepository.findAll(spec, pageable);
        return page.map(Order::from);
    }

    @Override
    @Transactional
    public void matchOrder(UUID orderID) {
        OrderEntity entity = orderRepository.findById(orderID)
                .orElseThrow(() -> {
                    log.warn("Order {} not found", orderID);
                    return new ResourceNotFoundException(String.format("Order %s not found", orderID));
                });

        if (OrderStatus.PENDING != entity.getStatus()) {
            log.warn("Order {} cannot be matched because it is in status {}", orderID, entity.getStatus());
            throw new IllegalArgumentException("Only PENDING orders can be matched");
        }

        retrying(() -> {
            UUID customerId = entity.getCustomer().getId();
            String assetName = entity.getAssetName();
            BigDecimal size = entity.getSize();
            BigDecimal price = entity.getPrice();

            if (OrderSide.BUY == entity.getSide()) {
                BigDecimal amountTRY = price.multiply(size);

                Asset cash = assetService.retrieveCustomerAsset(customerId, "TRY");
                if (cash.verifyReserved(amountTRY)) {
                    log.warn("Inconsistent TRY reserved balance to match BUY for customer {}", customerId);
                    throw new IllegalArgumentException("Inconsistent TRY reserved balance to match BUY");
                }
                cash.setReserved(cash.getReserved().subtract(amountTRY));
                assetService.upsertAsset(cash);

                Asset bought = assetService.getOrCreateAsset(customerId, assetName);
                bought.setUsable(bought.getUsable().add(size));
                bought.setSize(bought.getUsable().add(bought.getReserved()));
                assetService.upsertAsset(bought);

            } else {
                Asset sold = assetService.retrieveCustomerAsset(customerId, assetName);
                if (sold.verifyReserved(size)) {
                    log.warn("Inconsistent {} reserved balance to match SELL for customer {}", assetName, customerId);
                    throw new IllegalArgumentException("Inconsistent " + assetName + " reserved balance to match SELL");
                }
                sold.setReserved(sold.getReserved().subtract(size));
                assetService.upsertAsset(sold);

                BigDecimal amountTRY = price.multiply(size);
                Asset cash = assetService.getOrCreateAsset(customerId, "TRY");
                cash.setUsable(cash.getUsable().add(amountTRY));
                cash.setSize(cash.getUsable().add(cash.getReserved()));
                assetService.upsertAsset(cash);
            }
        });

        entity.setStatus(OrderStatus.MATCHED);
        // hibernate dirty checking will update the order on txn commit
    }

    private void retrying(Runnable work) {
        int maxRetries = orderServiceProperties.getOptimisticLockMaxRetries();
        if (maxRetries <= 0) {
            log.debug("No retries configured, running work directly");
            work.run();
            return;
        }

        log.debug("Running work with {} retries", maxRetries);
        OptimisticLockingFailureException last = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                work.run();
                return;
            } catch (OptimisticLockingFailureException ex) {
                last = ex;
            }
        }
        throw last;
    }
}
