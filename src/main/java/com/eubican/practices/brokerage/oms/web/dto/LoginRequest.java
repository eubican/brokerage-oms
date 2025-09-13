package com.eubican.practices.brokerage.oms.web.dto;

public record LoginRequest(
        String email,
        String password
) {
}
