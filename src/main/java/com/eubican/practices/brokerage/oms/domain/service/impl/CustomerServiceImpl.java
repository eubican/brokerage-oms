package com.eubican.practices.brokerage.oms.domain.service.impl;

import com.eubican.practices.brokerage.oms.domain.exception.ResourceNotFoundException;
import com.eubican.practices.brokerage.oms.domain.model.Customer;
import com.eubican.practices.brokerage.oms.domain.service.CustomerService;
import com.eubican.practices.brokerage.oms.persistence.entity.CustomerEntity;
import com.eubican.practices.brokerage.oms.persistence.repository.CustomerJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerJpaRepository customerRepository;

    @Override
    public Customer findByEmail(String email) {
        CustomerEntity customer = customerRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("Customer not found for email {}", email);
            return new ResourceNotFoundException("Customer not found");
        });
        return Customer.from(customer);
    }
}
