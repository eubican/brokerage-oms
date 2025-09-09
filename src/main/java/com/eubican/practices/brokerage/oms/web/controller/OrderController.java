package com.eubican.practices.brokerage.oms.web.controller;

import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import com.eubican.practices.brokerage.oms.domain.service.OrderService;
import com.eubican.practices.brokerage.oms.web.dto.OrderResponse;
import com.eubican.practices.brokerage.oms.web.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public PagedResponse<OrderResponse> fetchOrders(
            @RequestParam UUID customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String assetName,
            @PageableDefault(sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var page = orderService.fetchOrders(customerId, from, to, status, assetName, pageable)
                .map(OrderResponse::of);

        return PagedResponse.of(page);
    }

}
