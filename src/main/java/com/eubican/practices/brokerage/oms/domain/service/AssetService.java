package com.eubican.practices.brokerage.oms.domain.service;

import com.eubican.practices.brokerage.oms.domain.model.Asset;

import java.util.UUID;

public interface AssetService {

    void upsertAsset(Asset asset);

    Asset retrieveCustomerAsset(UUID customerId, String assetName);

}
