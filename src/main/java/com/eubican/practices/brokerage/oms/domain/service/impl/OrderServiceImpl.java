package com.eubican.practices.brokerage.oms.domain.service.impl;

import com.eubican.practices.brokerage.oms.config.properties.OrderServiceProperties;
import com.eubican.practices.brokerage.oms.domain.exception.OrderNotCancellableException;
import com.eubican.practices.brokerage.oms.domain.model.Asset;
import com.eubican.practices.brokerage.oms.domain.model.Order;
import com.eubican.practices.brokerage.oms.domain.model.OrderSide;
import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import com.eubican.practices.brokerage.oms.domain.service.AssetService;
import com.eubican.practices.brokerage.oms.domain.service.OrderService;
import com.eubican.practices.brokerage.oms.persistence.entity.OrderEntity;
import com.eubican.practices.brokerage.oms.persistence.entity.CustomerEntity;
import com.eubican.practices.brokerage.oms.persistence.repository.OrderJpaRepository;
import com.eubican.practices.brokerage.oms.persistence.repository.CustomerJpaRepository;
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

import com.eubican.practices.brokerage.oms.domain.exception.ResourceNotFoundException;

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
                    log.warn("Insufficient {} usable balance to place BUY for customer {}", order.getAssetName(), order.getCustomerId());
                    throw new IllegalArgumentException("Insufficient " + order.getAssetName() + " usable balance to place BUY");
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
                    log.warn("Inconsistent {} reserved balance to cancel BUY for customer {}", entity.getAssetName(), entity.getCustomer().getId());
                    throw new IllegalArgumentException("Inconsistent " + entity.getAssetName() + " reserved balance to cancel BUY");
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
        Page<OrderEntity> ordersPage;

        //todo a more flexible approach for future filters, we can switch the repo to JPA Specifications (single method + dynamic predicates)
        if (assetName != null && !assetName.isBlank()) {
            ordersPage = (status == null)
                    ? orderRepository.findByCustomer_IdAndCreatedAtBetweenAndAssetName(customerId, from, to, assetName, pageable)
                    : orderRepository.findByCustomer_IdAndCreatedAtBetweenAndStatusAndAssetName(customerId, from, to, status, assetName, pageable);
        } else {
            ordersPage = (status == null)
                    ? orderRepository.findByCustomer_IdAndCreatedAtBetween(customerId, from, to, pageable)
                    : orderRepository.findByCustomer_IdAndCreatedAtBetweenAndStatus(customerId, from, to, status, pageable);
        }

        return ordersPage.map(Order::from);
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
            if (OrderSide.BUY == entity.getSide()) {
                BigDecimal amountTRY = entity.getPrice().multiply(entity.getSize());

                // Deduct reserved TRY (spend the cash)
                Asset cash = assetService.retrieveCustomerAsset(entity.getCustomer().getId(), "TRY");
                if (cash.verifyReserved(amountTRY)) {
                    log.warn("Inconsistent TRY reserved balance to match BUY for customer {}", entity.getCustomer().getId());
                    throw new IllegalArgumentException("Inconsistent TRY reserved balance to match BUY");
                }
                cash.setReserved(cash.getReserved().subtract(amountTRY));
                assetService.upsertAsset(cash);

                // Credit bought asset as usable
                try {
                    Asset bought = assetService.retrieveCustomerAsset(entity.getCustomer().getId(), entity.getAssetName());
                    bought.setUsable(bought.getUsable().add(entity.getSize()));
                    assetService.upsertAsset(bought);
                } catch (ResourceNotFoundException rnfe) {
                    Asset newAsset = Asset.from(
                            entity.getCustomer().getId(),
                            entity.getAssetName(),
                            entity.getSize(),
                            entity.getSize(),
                            BigDecimal.ZERO
                    );
                    assetService.upsertAsset(newAsset);
                }
            } else { // SELL
                // Deduct reserved units of the sold asset
                Asset sold = assetService.retrieveCustomerAsset(entity.getCustomer().getId(), entity.getAssetName());
                if (sold.verifyReserved(entity.getSize())) {
                    log.warn("Inconsistent {} reserved balance to match SELL for customer {}", entity.getAssetName(), entity.getCustomer().getId());
                    throw new IllegalArgumentException("Inconsistent " + entity.getAssetName() + " reserved balance to match SELL");
                }
                sold.setReserved(sold.getReserved().subtract(entity.getSize()));
                assetService.upsertAsset(sold);

                // Credit TRY as usable
                BigDecimal amountTRY = entity.getPrice().multiply(entity.getSize());
                try {
                    Asset cash = assetService.retrieveCustomerAsset(entity.getCustomer().getId(), "TRY");
                    cash.setUsable(cash.getUsable().add(amountTRY));
                    assetService.upsertAsset(cash);
                } catch (ResourceNotFoundException rnfe) {
                    Asset newCash = Asset.from(
                            entity.getCustomer().getId(),
                            "TRY",
                            amountTRY,
                            amountTRY,
                            BigDecimal.ZERO
                    );
                    assetService.upsertAsset(newCash);
                }
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
