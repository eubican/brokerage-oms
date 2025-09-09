package com.eubican.practices.brokerage.oms.integration;

import com.eubican.practices.brokerage.oms.domain.model.Asset;
import com.eubican.practices.brokerage.oms.domain.model.Order;
import com.eubican.practices.brokerage.oms.domain.model.OrderSide;
import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import com.eubican.practices.brokerage.oms.domain.service.AssetService;
import com.eubican.practices.brokerage.oms.domain.service.OrderService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootTest
@Transactional
class OrderFlowIT {

    @Autowired
    AssetService assetService;

    @Autowired
    OrderService orderService;

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void verifyCashInitialStateAndBalanceInvariant() {
        Asset cash = assetService.retrieveCustomerAsset(CUSTOMER_ID, "TRY");

        Assertions.assertThat(cash.getUsable()).isGreaterThan(BigDecimal.ZERO);
        Assertions.assertThat(cash.getReserved()).isZero();
        Assertions.assertThat(cash.getSize()).isEqualByComparingTo(cash.getUsable().add(cash.getReserved()));
    }

    @Test
    void createAndCancelBuyOrderReservesAndReleases() {
        BigDecimal price = new BigDecimal("10.00");
        BigDecimal size = new BigDecimal("5");

        Asset cashBeforeOrder = assetService.retrieveCustomerAsset(CUSTOMER_ID, "TRY");
        BigDecimal initialUsable = cashBeforeOrder.getUsable();

        Order order = orderService.createOrder(Order.from(CUSTOMER_ID, "XYZ", OrderSide.BUY, size, price));
        Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        Asset cashAfterOrder = assetService.retrieveCustomerAsset(CUSTOMER_ID, "TRY");
        BigDecimal locked = price.multiply(size);
        Assertions.assertThat(cashAfterOrder.getUsable()).isEqualByComparingTo(initialUsable.subtract(locked));
        Assertions.assertThat(cashAfterOrder.getReserved()).isEqualByComparingTo(locked);

        orderService.cancelOrder(order.getId());
        Asset cashAfterCancel = assetService.retrieveCustomerAsset(CUSTOMER_ID, "TRY");
        Assertions.assertThat(cashAfterCancel.getUsable()).isEqualByComparingTo(initialUsable);
        Assertions.assertThat(cashAfterCancel.getReserved()).isZero();
    }

    @Test
    void createAndCancelSellOrderReservesAndReleases() {
        BigDecimal size = new BigDecimal("2");

        Asset assetBeforeCancel = assetService.retrieveCustomerAsset(CUSTOMER_ID, "XYZ");
        BigDecimal initialUsable = assetBeforeCancel.getUsable();

        Order order = orderService.createOrder(Order.from(CUSTOMER_ID, "XYZ", OrderSide.SELL, size, new BigDecimal("50")));
        Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        Asset assetAfterOrder = assetService.retrieveCustomerAsset(CUSTOMER_ID, "XYZ");
        Assertions.assertThat(assetAfterOrder.getUsable()).isEqualByComparingTo(initialUsable.subtract(size));
        Assertions.assertThat(assetAfterOrder.getReserved()).isEqualByComparingTo(size);

        orderService.cancelOrder(order.getId());

        Asset assetAfterCancel = assetService.retrieveCustomerAsset(CUSTOMER_ID, "XYZ");
        Assertions.assertThat(assetAfterCancel.getUsable()).isEqualByComparingTo(initialUsable);
        Assertions.assertThat(assetAfterCancel.getReserved()).isZero();
    }

}
