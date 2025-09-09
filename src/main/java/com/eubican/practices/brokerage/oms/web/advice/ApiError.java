package com.eubican.practices.brokerage.oms.web.advice;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiError of(
            int status,
            String error,
            String message,
            String path
    ) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
