package com.eubican.practices.brokerage.oms.persistence.repository;

import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import com.eubican.practices.brokerage.oms.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByCustomerIdAndCreatedAtBetween(UUID customerId, Instant from, Instant to, Pageable pageable);

    Page<OrderEntity> findByCustomerIdAndCreatedAtBetweenAndStatus(
            UUID customerId, Instant from, Instant to, OrderStatus status, Pageable pageable);

    Page<OrderEntity> findByCustomerIdAndCreatedAtBetweenAndAssetName(
            UUID customerId, Instant from, Instant to, String assetName, Pageable pageable);

    Page<OrderEntity> findByCustomerIdAndCreatedAtBetweenAndStatusAndAssetName(
            UUID customerId, Instant from, Instant to, OrderStatus status, String assetName, Pageable pageable);


}
