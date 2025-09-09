package com.eubican.practices.brokerage.oms.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AssetResponse(
        Long id,
        UUID customerId,
        String assetName,
        BigDecimal size,
        BigDecimal usableSize
) {
}