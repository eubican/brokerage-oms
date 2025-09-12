package com.eubican.practices.brokerage.oms.web.controller;

import com.eubican.practices.brokerage.oms.domain.service.AssetService;
import com.eubican.practices.brokerage.oms.web.dto.AssetResponse;
import com.eubican.practices.brokerage.oms.web.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public PagedResponse<AssetResponse> fetchCustomerAssets(
            @RequestParam UUID customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var page = assetService.fetchCustomerAssets(
                customerId,
                from,
                to,
                pageable.getPageNumber(),
                pageable.getPageSize()
        ).map(AssetResponse::of);

        return PagedResponse.of(page);
    }

    @GetMapping("/{assetName}")
    @ResponseStatus(code = HttpStatus.OK)
    public AssetResponse fetchCustomerAsset(@RequestParam UUID customerId, @PathVariable String assetName) {
        return AssetResponse.of(assetService.retrieveCustomerAsset(customerId, assetName));
    }

}
