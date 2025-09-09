package com.eubican.practices.brokerage.oms.domain.service;

import com.eubican.practices.brokerage.oms.domain.model.Order;
import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface OrderService {

    Order createOrder(Order order);

    void cancelOrder(UUID orderID);

    Page<Order> fetchOrders(UUID customerId, Instant from, Instant to, OrderStatus status, String assetName, Pageable pageable);

    // Phase 2 (bonus): void matchOrder(UUID orderId);

}
