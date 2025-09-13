package com.eubican.practices.brokerage.oms.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer")
@Data
public class CustomerEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "role", nullable = false, length = 64)
    private String role;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String password;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
