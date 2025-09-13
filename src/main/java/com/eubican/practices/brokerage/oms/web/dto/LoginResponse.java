package com.eubican.practices.brokerage.oms.web.dto;

import java.time.Instant;

public record LoginResponse(String tokenType, String accessToken, Instant expiresAt) {
}