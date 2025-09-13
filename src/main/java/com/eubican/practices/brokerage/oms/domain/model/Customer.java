package com.eubican.practices.brokerage.oms.domain.model;

import com.eubican.practices.brokerage.oms.persistence.entity.CustomerEntity;
import lombok.Getter;

import java.util.UUID;

@Getter
public final class Customer {

    private final UUID id;

    private final String email;

    private final String passwordHash;

    private final String role;

    private Customer(UUID id, String email, String role, String passwordHash) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public static Customer from(String email, String role) {
        return new Customer(UUID.randomUUID(), email, role, null);
    }

    public static Customer from(CustomerEntity entity) {
        return new Customer(entity.getId(), entity.getEmail(), entity.getRole(), entity.getPassword());
    }

}
