package com.eubican.practices.brokerage.oms.domain.service;

import com.eubican.practices.brokerage.oms.domain.model.Asset;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.UUID;

public interface AssetService {

    void upsertAsset(Asset asset);

    Asset retrieveCustomerAsset(UUID customerId, String assetName);

    Asset getOrCreateAsset(UUID customerId, String assetName);

    Page<Asset> fetchCustomerAssets(UUID customerId, Instant from, Instant to, int page, int size);

}
