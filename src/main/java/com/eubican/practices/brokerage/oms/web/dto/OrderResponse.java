package com.eubican.practices.brokerage.oms.web.dto;

import com.eubican.practices.brokerage.oms.domain.model.OrderSide;
import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID customerId,
        String assetName,
        OrderStatus status,
        OrderSide side,
        BigDecimal size,
        BigDecimal price,
        Instant createdAt
) {
}
