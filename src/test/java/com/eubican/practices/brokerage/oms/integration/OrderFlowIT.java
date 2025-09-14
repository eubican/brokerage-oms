package com.eubican.practices.brokerage.oms.integration;

import com.eubican.practices.brokerage.oms.domain.exception.ResourceNotFoundException;
import com.eubican.practices.brokerage.oms.domain.model.Asset;
import com.eubican.practices.brokerage.oms.domain.model.Order;
import com.eubican.practices.brokerage.oms.domain.model.OrderSide;
import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import com.eubican.practices.brokerage.oms.domain.service.AssetService;
import com.eubican.practices.brokerage.oms.domain.service.OrderService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @BeforeEach
    void setAdminAuth() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "password", "ROLE_ADMIN")
        );
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

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


    @Test
    void cancelOrderNotFoundThrowsResourceNotFound() {
        UUID randomId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        Assertions.assertThatThrownBy(() -> orderService.cancelOrder(randomId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void cancelBuyOrderWithInconsistentReservedThrowsIllegalArgument() {
        BigDecimal price = new BigDecimal("10.00");
        BigDecimal size = new BigDecimal("2");
        Order order = orderService.createOrder(Order.from(CUSTOMER_ID, "XYZ", OrderSide.BUY, size, price));

        Asset cash = assetService.retrieveCustomerAsset(CUSTOMER_ID, "TRY");
        cash.setReserved(cash.getReserved().subtract(BigDecimal.ONE));
        assetService.upsertAsset(cash);

        Assertions.assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inconsistent TRY reserved balance to cancel BUY");
    }

    @Test
    void cancelSellOrderWithInconsistentReservedThrowsIllegalArgument() {
        BigDecimal price = new BigDecimal("50.00");
        BigDecimal size = new BigDecimal("1");
        Order order = orderService.createOrder(Order.from(CUSTOMER_ID, "XYZ", OrderSide.SELL, size, price));

        Asset xyz = assetService.retrieveCustomerAsset(CUSTOMER_ID, "XYZ");
        xyz.setReserved(xyz.getReserved().subtract(new BigDecimal("0.5")));
        assetService.upsertAsset(xyz);

        Assertions.assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inconsistent XYZ reserved balance to cancel SELL");
    }

    @Test
    void matchBuyOrderBurnsReservedTryAndCreditsAsset() {
        BigDecimal price = new BigDecimal("12.50");
        BigDecimal size = new BigDecimal("4");

        Asset cashBefore = assetService.retrieveCustomerAsset(CUSTOMER_ID, "TRY");
        BigDecimal cashUsableBefore = cashBefore.getUsable();
        BigDecimal cashReservedBefore = cashBefore.getReserved();

        Asset xyzBefore;
        try {
            xyzBefore = assetService.retrieveCustomerAsset(CUSTOMER_ID, "XYZ");
        } catch (Exception e) {
            xyzBefore = Asset.from(CUSTOMER_ID, "XYZ", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal xyzUsableBefore = xyzBefore.getUsable();
        BigDecimal xyzReservedBefore = xyzBefore.getReserved();

        Order order = orderService.createOrder(Order.from(CUSTOMER_ID, "XYZ", OrderSide.BUY, size, price));
        BigDecimal lockedTry = price.multiply(size);

        Asset cashAfterCreate = assetService.retrieveCustomerAsset(CUSTOMER_ID, "TRY");
        Assertions.assertThat(cashAfterCreate.getUsable()).isEqualByComparingTo(cashUsableBefore.subtract(lockedTry));
        Assertions.assertThat(cashAfterCreate.getReserved()).isEqualByComparingTo(cashReservedBefore.add(lockedTry));

        orderService.matchOrder(order.getId());

        Asset cashAfterMatch = assetService.retrieveCustomerAsset(CUSTOMER_ID, "TRY");
        Assertions.assertThat(cashAfterMatch.getReserved()).isEqualByComparingTo(cashReservedBefore);
        Assertions.assertThat(cashAfterMatch.getUsable()).isEqualByComparingTo(cashUsableBefore.subtract(lockedTry));

        Asset xyzAfterMatch = assetService.retrieveCustomerAsset(CUSTOMER_ID, "XYZ");
        Assertions.assertThat(xyzAfterMatch.getUsable()).isEqualByComparingTo(xyzUsableBefore.add(size));
        Assertions.assertThat(xyzAfterMatch.getReserved()).isEqualByComparingTo(xyzReservedBefore);
    }

    @Test
    void matchSellOrderBurnsReservedAssetAndCreditsTry() {
        BigDecimal price = new BigDecimal("20.00");
        BigDecimal size = new BigDecimal("3");
        BigDecimal proceeds = price.multiply(size);

        Asset xyzBefore = assetService.retrieveCustomerAsset(CUSTOMER_ID, "XYZ");
        BigDecimal xyzUsableBefore = xyzBefore.getUsable();
        BigDecimal xyzReservedBefore = xyzBefore.getReserved();

        Asset cashBefore;
        try {
            cashBefore = assetService.retrieveCustomerAsset(CUSTOMER_ID, "TRY");
        } catch (Exception e) {
            cashBefore = Asset.from(CUSTOMER_ID, "TRY", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal cashUsableBefore = cashBefore.getUsable();
        BigDecimal cashReservedBefore = cashBefore.getReserved();

        Order order = orderService.createOrder(Order.from(CUSTOMER_ID, "XYZ", OrderSide.SELL, size, price));
        Asset xyzAfterCreate = assetService.retrieveCustomerAsset(CUSTOMER_ID, "XYZ");
        Assertions.assertThat(xyzAfterCreate.getUsable()).isEqualByComparingTo(xyzUsableBefore.subtract(size));
        Assertions.assertThat(xyzAfterCreate.getReserved()).isEqualByComparingTo(xyzReservedBefore.add(size));

        orderService.matchOrder(order.getId());

        Asset xyzAfterMatch = assetService.retrieveCustomerAsset(CUSTOMER_ID, "XYZ");
        Assertions.assertThat(xyzAfterMatch.getReserved()).isEqualByComparingTo(xyzReservedBefore);
        Assertions.assertThat(xyzAfterMatch.getUsable()).isEqualByComparingTo(xyzUsableBefore.subtract(size));

        Asset cashAfterMatch = assetService.retrieveCustomerAsset(CUSTOMER_ID, "TRY");
        Assertions.assertThat(cashAfterMatch.getUsable()).isEqualByComparingTo(cashUsableBefore.add(proceeds));
        Assertions.assertThat(cashAfterMatch.getReserved()).isEqualByComparingTo(cashReservedBefore);
    }

    @Test
    void matchNonPendingOrderThrows() {
        BigDecimal price = new BigDecimal("10.00");
        BigDecimal size = new BigDecimal("1");

        Order order = orderService.createOrder(Order.from(CUSTOMER_ID, "XYZ", OrderSide.BUY, size, price));
        orderService.cancelOrder(order.getId());

        Assertions.assertThatThrownBy(() -> orderService.matchOrder(order.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PENDING orders can be matched");
    }

    @Test
    void matchBuyOrderWithInconsistentReservedThrowsIllegalArgument() {
        BigDecimal price = new BigDecimal("12.00");
        BigDecimal size = new BigDecimal("3");
        Order order = orderService.createOrder(Order.from(CUSTOMER_ID, "XYZ", OrderSide.BUY, size, price));

        Asset cash = assetService.retrieveCustomerAsset(CUSTOMER_ID, "TRY");
        cash.setReserved(cash.getReserved().subtract(BigDecimal.ONE));
        assetService.upsertAsset(cash);

        Assertions.assertThatThrownBy(() -> orderService.matchOrder(order.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inconsistent TRY reserved balance to match BUY");
    }

    @Test
    void matchSellOrderWithInconsistentReservedThrowsIllegalArgument() {
        BigDecimal price = new BigDecimal("7.50");
        BigDecimal size = new BigDecimal("2");
        Order order = orderService.createOrder(Order.from(CUSTOMER_ID, "XYZ", OrderSide.SELL, size, price));

        Asset xyz = assetService.retrieveCustomerAsset(CUSTOMER_ID, "XYZ");
        xyz.setReserved(xyz.getReserved().subtract(new BigDecimal("1")));
        assetService.upsertAsset(xyz);

        Assertions.assertThatThrownBy(() -> orderService.matchOrder(order.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inconsistent XYZ reserved balance to match SELL");
    }


}
