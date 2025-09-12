package com.eubican.practices.brokerage.oms.web.dto;

import com.eubican.practices.brokerage.oms.domain.model.Asset;

import java.math.BigDecimal;
import java.util.UUID;

public record AssetResponse(
        UUID customerId,
        String assetName,
        BigDecimal size,
        BigDecimal usable,
        BigDecimal reserved
) {
    public static AssetResponse of(Asset asset) {
        return new AssetResponse(
                asset.getCustomerId(),
                asset.getAssetName(),
                asset.getSize(),
                asset.getUsable(),
                asset.getReserved()
        );
    }
}