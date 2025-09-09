package com.eubican.practices.brokerage.oms.web.controller;

import com.eubican.practices.brokerage.oms.domain.model.Order;
import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import com.eubican.practices.brokerage.oms.domain.service.OrderService;
import com.eubican.practices.brokerage.oms.web.dto.OrderResponse;
import com.eubican.practices.brokerage.oms.web.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
        Page<Order> p = orderService.fetchOrders(
                customerId,
                from,
                to,
                status,
                assetName,
                pageable
        );

        return new PagedResponse<>(
                p.getContent().stream().map(this::toResponse).toList(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isFirst(),
                p.isLast()
        );
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(),
                order.getCustomerId(),
                order.getAssetName(),
                order.getStatus(),
                order.getSide(),
                order.getSize(),
                order.getPrice(),
                order.getCreatedAt()
        );
    }

}
