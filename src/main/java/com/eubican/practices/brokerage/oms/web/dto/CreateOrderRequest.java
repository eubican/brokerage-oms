package com.eubican.practices.brokerage.oms.web.dto;

import com.eubican.practices.brokerage.oms.domain.model.OrderSide;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID customerId,
        @NotBlank @Pattern(regexp = "[A-Z0-9_]{2,16}") String assetName,
        @NotNull OrderSide side,
        @NotNull @DecimalMin(value = "0.000001") @Digits(integer = 26, fraction = 6) BigDecimal size,
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 28, fraction = 4) BigDecimal price
) {
}
