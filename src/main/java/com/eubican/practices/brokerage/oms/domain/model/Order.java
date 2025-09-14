package com.eubican.practices.brokerage.oms.domain.model;

import com.eubican.practices.brokerage.oms.persistence.entity.OrderEntity;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class Order {
    private final UUID id;

    private final UUID customerId;

    private final String assetName;

    private final OrderSide side;

    private final BigDecimal size;

    private final BigDecimal price;

    private final OrderStatus status;

    private final Instant createdAt;

    private Order(UUID id,
                  UUID customerId,
                  String assetName,
                  OrderSide side,
                  BigDecimal size,
                  BigDecimal price,
                  OrderStatus status,
                  Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.assetName = requireAsset(assetName);
        this.side = Objects.requireNonNull(side, "side");

        this.size = normalizeShareSize(requirePositive(requireScaleAtMost(size, 6, "size"), "size"));
        this.price = normalizePrice(requirePositive(requireScaleAtMost(price, 4, "price"), "price"));

        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Order from(UUID customerId,
                             String assetName,
                             OrderSide side,
                             BigDecimal size,
                             BigDecimal price
    ) {
        return new Order(
                UUID.randomUUID(),
                customerId,
                assetName,
                side,
                size,
                price,
                OrderStatus.PENDING,
                Instant.now()
        );
    }

    public static Order from(OrderEntity entity) {
        return new Order(
                entity.getId(),
                entity.getCustomer().getId(),
                entity.getAssetName(),
                entity.getSide(),
                entity.getSize(),
                entity.getPrice(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    private static BigDecimal requirePositive(BigDecimal v, String field) {
        if (v == null || v.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
        return v;
    }

    private static BigDecimal requireScaleAtMost(BigDecimal v, int maxScale, String field) {
        if (v == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (v.scale() > maxScale) {
            throw new IllegalArgumentException(field + " scale exceeds " + maxScale + " decimals");
        }
        return v;
    }

    private static String requireAsset(String asset) {
        if (asset == null || asset.isBlank()) {
            throw new IllegalArgumentException("assetName is required");
        }
        if (!asset.matches("[A-Z0-9_]{2,16}")) {
            throw new IllegalArgumentException("assetName must match [A-Z0-9_]{2,16}");
        }
        return asset;
    }

    private static BigDecimal normalizePrice(BigDecimal price) {
        return price.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeShareSize(BigDecimal size) {
        return size.setScale(6, RoundingMode.HALF_UP);
    }

}
