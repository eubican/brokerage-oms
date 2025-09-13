package com.eubican.practices.brokerage.oms.domain.service;

import com.eubican.practices.brokerage.oms.domain.model.Customer;

public interface CustomerService {

    Customer findByEmail(String email);

}
