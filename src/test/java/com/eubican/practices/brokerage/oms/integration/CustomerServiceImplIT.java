package com.eubican.practices.brokerage.oms.integration;

import com.eubican.practices.brokerage.oms.domain.exception.ResourceNotFoundException;
import com.eubican.practices.brokerage.oms.domain.model.Customer;
import com.eubican.practices.brokerage.oms.domain.service.CustomerService;
import com.eubican.practices.brokerage.oms.persistence.entity.CustomerEntity;
import com.eubican.practices.brokerage.oms.persistence.repository.CustomerJpaRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest
@Transactional
@Rollback
class CustomerServiceImplIT {

    private static final String ROLE = "ROLE_CUSTOMER";

    @Autowired
    CustomerService customerService;

    @Autowired
    CustomerJpaRepository customerRepository;

    private UUID existingId;
    private String existingEmail;

    @BeforeEach
    void setUp() {
        existingId = UUID.randomUUID();
        existingEmail = "alice+" + existingId + "@example.test";

        CustomerEntity entity = new CustomerEntity();
        entity.setId(existingId);
        entity.setEmail(existingEmail);
        entity.setPassword("");
        entity.setRole(ROLE);
        entity.setCreatedAt(Instant.now());
        customerRepository.save(entity);
    }

    @Test
    void findByEmailReturnsCustomerWhenExists() {
        Customer result = customerService.findByEmail(existingEmail);
        Assertions.assertThat(result.getId()).isEqualTo(existingId);
        Assertions.assertThat(result.getEmail()).isEqualTo(existingEmail);
        Assertions.assertThat(result.getRole()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void findByEmailThrowsWhenMissing() {
        Assertions.assertThatThrownBy(() -> customerService.findByEmail("nobody@nowhere.test"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

}
