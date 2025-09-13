package com.eubican.practices.brokerage.oms.persistence.repository;

import com.eubican.practices.brokerage.oms.persistence.entity.AssetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AssetJpaRepository extends JpaRepository<AssetEntity, UUID> {

    Optional<AssetEntity> findByCustomerIdAndAssetName(UUID customerId, String assetName);

    Page<AssetEntity> findByCustomerIdAndCreatedAtBetween(UUID customerId, Instant from, Instant to, Pageable pageable);

}
