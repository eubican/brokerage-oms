package com.eubican.practices.brokerage.oms.domain.exception;

import org.springframework.http.HttpStatus;

public class OrderNotCancellableException extends ApplicationException {

    public OrderNotCancellableException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

}
