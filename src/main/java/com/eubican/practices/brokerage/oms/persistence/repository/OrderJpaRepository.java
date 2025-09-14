package com.eubican.practices.brokerage.oms.persistence.repository;

import com.eubican.practices.brokerage.oms.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface OrderJpaRepository
        extends JpaRepository<OrderEntity, UUID>, JpaSpecificationExecutor<OrderEntity> {

}
