package com.eubican.practices.brokerage.oms.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(
        name = "asset",
        uniqueConstraints = @UniqueConstraint(name = "uk_asset_customer_asset", columnNames = {"customer_id", "asset_name"})
)
@Data
public class AssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "customer_id", nullable = false, columnDefinition = "UUID")
    private UUID customerId;


    @Column(name = "asset_name", nullable = false, length = 32)
    private String assetName;


    @Column(name = "size", nullable = false, precision = 32, scale = 6)
    private BigDecimal size = BigDecimal.ZERO;


    @Column(name = "usable_size", nullable = false, precision = 32, scale = 6)
    private BigDecimal usable = BigDecimal.ZERO;

    @Column(name = "reserved_size", nullable = false, precision = 32, scale = 6)
    private BigDecimal reserved = BigDecimal.ZERO;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    private void normalizeAndSyncSize() {
        if (usable == null) {
            usable = BigDecimal.ZERO;
        }
        if (reserved == null) {
            reserved = BigDecimal.ZERO;
        }
        usable = usable.setScale(6, RoundingMode.HALF_UP);
        reserved = reserved.setScale(6, RoundingMode.HALF_UP);
        size = usable.add(reserved).setScale(6, RoundingMode.HALF_UP);
    }
}
