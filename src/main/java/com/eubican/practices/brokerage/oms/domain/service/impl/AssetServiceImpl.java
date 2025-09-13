package com.eubican.practices.brokerage.oms.domain.service.impl;

import com.eubican.practices.brokerage.oms.domain.model.Asset;
import com.eubican.practices.brokerage.oms.domain.service.AssetService;
import com.eubican.practices.brokerage.oms.persistence.entity.AssetEntity;
import com.eubican.practices.brokerage.oms.persistence.entity.CustomerEntity;
import com.eubican.practices.brokerage.oms.persistence.repository.AssetJpaRepository;
import com.eubican.practices.brokerage.oms.persistence.repository.CustomerJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import com.eubican.practices.brokerage.oms.domain.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
class AssetServiceImpl implements AssetService {

    private final AssetJpaRepository assetRepository;

    private final CustomerJpaRepository customerRepository;

    @Override
    @Transactional
    public void upsertAsset(Asset asset) {
        AssetEntity entity = assetRepository.findByCustomer_IdAndAssetName(asset.getCustomerId(), asset.getAssetName())
                .orElseGet(() -> {
                    AssetEntity e = new AssetEntity();
                    e.setAssetName(asset.getAssetName());
                    CustomerEntity ref = customerRepository.getReferenceById(asset.getCustomerId());
                    e.setCustomer(ref);
                    return e;
                });

        entity.setSize(asset.getSize());
        entity.setUsable(asset.getUsable());
        entity.setReserved(asset.getReserved());

        assetRepository.save(entity);
        log.debug("Asset {} saved for customer: {}", asset.getAssetName(), asset.getCustomerId());
    }

    @Override
    @Transactional(readOnly = true)
    public Asset retrieveCustomerAsset(UUID customerId, String assetName) {
        return assetRepository.findByCustomer_IdAndAssetName(customerId, assetName)
                .map(Asset::from)
                .orElseThrow(() -> {
                    log.warn("Asset {} not found for customer {}", assetName, customerId);
                    return new ResourceNotFoundException(assetName + " asset not found");
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Asset> fetchCustomerAssets(UUID customerId, Instant from, Instant to, int page, int size) {
        Page<AssetEntity> assetsPage = assetRepository.findByCustomer_IdAndCreatedAtBetween(
                customerId,
                from,
                to,
                PageRequest.of(page, size)
        );
        return assetsPage.map(Asset::from);
    }

}
