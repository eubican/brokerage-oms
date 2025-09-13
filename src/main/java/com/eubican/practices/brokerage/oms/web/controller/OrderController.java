package com.eubican.practices.brokerage.oms.web.controller;

import com.eubican.practices.brokerage.oms.domain.model.Order;
import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import com.eubican.practices.brokerage.oms.domain.model.constants.ControllerPaths;
import com.eubican.practices.brokerage.oms.domain.service.OrderService;
import com.eubican.practices.brokerage.oms.web.dto.CreateOrderRequest;
import com.eubican.practices.brokerage.oms.web.dto.OrderResponse;
import com.eubican.practices.brokerage.oms.web.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(ControllerPaths.API_V_1_ORDERS)
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PreAuthorize("@authorizationGuard.canAccessCustomer(#request.customerId())")
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(
                Order.from(
                        request.customerId(),
                        request.assetName(),
                        request.side(),
                        request.size(),
                        request.price()
                )
        );

        return OrderResponse.of(order);
    }

    @PostMapping("/{orderId}/cancel")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void cancelOrder(@PathVariable UUID orderId) {
        orderService.cancelOrder(orderId);
    }

    @PreAuthorize("@authorizationGuard.canAccessCustomer(#customerId)")
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public PagedResponse<OrderResponse> fetchOrders(
            @RequestParam UUID customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String assetName,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var page = orderService.fetchOrders(
                customerId,
                from,
                to,
                status,
                assetName,
                pageable
        ).map(OrderResponse::of);

        return PagedResponse.of(page);
    }

}
