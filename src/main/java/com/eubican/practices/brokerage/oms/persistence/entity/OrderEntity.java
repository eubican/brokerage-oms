package com.eubican.practices.brokerage.oms.persistence.entity;

import com.eubican.practices.brokerage.oms.domain.model.OrderSide;
import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "customer_id", nullable = false, columnDefinition = "UUID")
    private UUID customerId;

    @Column(name = "asset_name", nullable = false, length = 32)
    private String assetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private OrderSide side;

    @Column(name = "size", nullable = false, precision = 32, scale = 4)
    private BigDecimal size;

    @Column(name = "price", nullable = false, precision = 32, scale = 6)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    //todo we should use epoch long
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
