package com.eubican.practices.brokerage.oms.domain.service.impl;

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

    //todo we'll make this configurational
    private static final int MAX_RETRIES = 3;

    private final OrderJpaRepository orderRepository;

    private final AssetService assetService;

    private final AuthorizationGuard authorizationGuard;

    private final CustomerJpaRepository customerRepository;

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

    private void retrying(Runnable work) {
        OptimisticLockingFailureException last = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
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
