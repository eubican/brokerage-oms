package com.eubican.practices.brokerage.oms.domain.model;

import com.eubican.practices.brokerage.oms.persistence.entity.AssetEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class Asset {

    private final Long id;

    private final UUID customerId;

    private final String assetName;

    private final BigDecimal size;

    @Setter
    private BigDecimal usable;

    @Setter
    private BigDecimal reserved;

    //todo can we use annotations?
    private Asset(Long id,
                  UUID customerId,
                  String assetName,
                  BigDecimal size,
                  BigDecimal usable,
                  BigDecimal reserved
    ) {
        this.id = id;
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.assetName = Objects.requireNonNull(assetName, "assetName");
        this.size = Objects.requireNonNull(size, "size");
        this.usable = Objects.requireNonNull(usable, "usable");
        this.reserved = Objects.requireNonNull(reserved, "reserved");
    }

    public static Asset from(UUID customerId, String assetName, BigDecimal size, BigDecimal usable, BigDecimal reserved) {
        return new Asset(null, customerId, assetName, size, usable, reserved);
    }

    public static Asset from(AssetEntity entity) {
        return new Asset(
                entity.getId(),
                entity.getCustomerId(),
                entity.getAssetName(),
                entity.getSize(),
                entity.getUsable(),
                entity.getReserved()
        );
    }

    public boolean hasInsufficientFunds(BigDecimal needed) {
        return usable.compareTo(needed) < 0;
    }

    public boolean verifyReserved(BigDecimal needed) {
        return reserved.compareTo(needed) < 0;
    }

}
