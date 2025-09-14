package com.eubican.practices.brokerage.oms.web.controller;

import com.eubican.practices.brokerage.oms.domain.model.constants.ControllerPaths;
import com.eubican.practices.brokerage.oms.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ControllerPaths.API_V_1_ADMIN_ORDERS)
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @PreAuthorize("@authorizationGuard.isAdmin(authentication)")
    @PostMapping("/{orderId}/match")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public void matchOrder(@PathVariable UUID orderId) {
        orderService.matchOrder(orderId);
    }

}
