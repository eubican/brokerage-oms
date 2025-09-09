package com.eubican.practices.brokerage.oms.domain.service.impl;

import com.eubican.practices.brokerage.oms.domain.model.Asset;
import com.eubican.practices.brokerage.oms.domain.service.AssetService;
import com.eubican.practices.brokerage.oms.persistence.entity.AssetEntity;
import com.eubican.practices.brokerage.oms.persistence.repository.AssetJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class AssetServiceImpl implements AssetService {

    private final AssetJpaRepository assetRepository;

    @Override
    public void upsertAsset(Asset asset) {
        AssetEntity entity = assetRepository.findByCustomerIdAndAssetName(asset.getCustomerId(), asset.getAssetName())
                .orElseGet(() -> {
                    AssetEntity e = new AssetEntity();
                    e.setCustomerId(asset.getCustomerId());
                    e.setAssetName(asset.getAssetName());
                    return e;
                });

        entity.setSize(asset.getSize());
        entity.setUsable(asset.getUsable());
        entity.setReserved(asset.getReserved());

        assetRepository.save(entity);
        log.debug("Asset {} saved for customer: {}", asset.getAssetName(), asset.getCustomerId());
    }

    @Override
    public Asset retrieveCustomerAsset(UUID customerId, String assetName) {
        return assetRepository.findByCustomerIdAndAssetName(customerId, assetName)
                .map(Asset::from)
                .orElseThrow(() -> {
                    log.warn("Asset {} not found for customer {}", assetName, customerId);
                    return new IllegalArgumentException(assetName + " asset not found");
                });
    }

}
