package com.eubican.practices.brokerage.oms.persistence.repository;

import com.eubican.practices.brokerage.oms.domain.model.OrderSide;
import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import com.eubican.practices.brokerage.oms.persistence.entity.CustomerEntity;
import com.eubican.practices.brokerage.oms.persistence.entity.OrderEntity;
import com.eubican.practices.brokerage.oms.persistence.repository.helper.OrderSpecifications;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@DataJpaTest
class OrderJpaRepositorySpecTest {

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private EntityManager em;

    private UUID customerAId;

    private Instant t0;
    private Instant t1;
    private Instant t2;
    private Instant t3;

    @BeforeEach
    void setUp() {
        t0 = Instant.now().minus(10, ChronoUnit.DAYS);
        t1 = Instant.now().minus(5, ChronoUnit.DAYS);
        t2 = Instant.now().minus(1, ChronoUnit.DAYS);
        t3 = Instant.now().plus(1, ChronoUnit.DAYS);


        CustomerEntity customerA = persistCustomer(t0);
        CustomerEntity customerB = persistCustomer(t0);

        customerAId = customerA.getId();

        persistOrder(customerA, t1.plus(1, ChronoUnit.HOURS), OrderStatus.PENDING, OrderSide.BUY, "BTCUSDT");
        persistOrder(customerA, t1.plus(2, ChronoUnit.HOURS), OrderStatus.MATCHED, OrderSide.BUY, "ETHUSDT");
        persistOrder(customerA, t2.minus(3, ChronoUnit.HOURS), OrderStatus.CANCELED, OrderSide.BUY, "btcusdt");
        persistOrder(customerB, t1.plus(1, ChronoUnit.HOURS), OrderStatus.PENDING, OrderSide.BUY, "BTCUSDT");

        em.flush();
        em.clear();
    }

    @Test
    void findByRequiredOnly() {
        var spec = OrderSpecifications.byFilters(
                customerAId, t0, t3, null, null
        );

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<OrderEntity> page = orderRepository.findAll(spec, pageable);

        Assertions.assertThat(page.getContent()).hasSize(3);
        Assertions.assertThat(page.getContent()).allMatch(o -> o.getCustomer().getId().equals(customerAId));
    }

    @Test
    void findByStatus() {
        var spec = OrderSpecifications.byFilters(
                customerAId, t0, t3, OrderStatus.MATCHED, null
        );

        Page<OrderEntity> page = orderRepository.findAll(spec, PageRequest.of(0, 10));
        Assertions.assertThat(page.getContent()).hasSize(1);
        Assertions.assertThat(page.getContent().getFirst().getStatus()).isEqualTo(OrderStatus.MATCHED);
    }

    @Test
    void findByAssetNameContainsCaseInsensitive() {
        var spec = OrderSpecifications.byFilters(
                customerAId, t0, t3, null, "btc"
        );

        Page<OrderEntity> page = orderRepository.findAll(spec, PageRequest.of(0, 10));
        Assertions.assertThat(page.getContent()).hasSize(2);
        Assertions.assertThat(page.getContent()).allMatch(o -> o.getAssetName().toLowerCase().contains("btc"));
    }

    @Test
    void findByStatusAndAssetNameContains() {
        var spec = OrderSpecifications.byFilters(
                customerAId, t0, t3, OrderStatus.PENDING, "btc"
        );

        Page<OrderEntity> page = orderRepository.findAll(spec, PageRequest.of(0, 10));
        Assertions.assertThat(page.getContent()).hasSize(1);
        Assertions.assertThat(page.getContent().getFirst().getStatus()).isEqualTo(OrderStatus.PENDING);
        Assertions.assertThat(page.getContent().getFirst().getAssetName().equalsIgnoreCase("BTCUSDT")).isTrue();
    }

    @Test
    void respectsDateWindow() {
        var spec = OrderSpecifications.byFilters(
                customerAId, t1, t2, null, null
        );

        Page<OrderEntity> page = orderRepository.findAll(spec, PageRequest.of(0, 10));

        Assertions.assertThat(page.getContent()).hasSize(3);
        Assertions.assertThat(page.getContent()).allMatch(o ->
                !o.getCreatedAt().isBefore(t1) && !o.getCreatedAt().isAfter(t2)
        );
    }

    @Test
    void findByAssetNameContainsNoMatch() {
        var spec = OrderSpecifications.byFilters(
                customerAId,
                t0, t3,
                null,
                "dogecoin"
        );

        Page<OrderEntity> page = orderRepository.findAll(spec, PageRequest.of(0, 10));

        Assertions.assertThat(page).isEmpty();
    }


    private CustomerEntity persistCustomer(Instant createdAt) {
        var customer = new CustomerEntity();
        customer.setId(UUID.randomUUID());
        customer.setEmail("test" + customer.getId() + "@example.com");
        customer.setRole("ROLE_CUSTOMER");
        customer.setPassword("test" + customer.getId() + "password");
        customer.setCreatedAt(createdAt);
        em.persist(customer);
        em.flush();
        return customer;
    }

    private void persistOrder(CustomerEntity customer, Instant createdAt, OrderStatus status, OrderSide side, String asset) {
        var order = new OrderEntity();
        order.setId(UUID.randomUUID());
        order.setCustomer(customer);
        order.setAssetName(asset);
        order.setSide(side);
        order.setSize(new BigDecimal("1"));
        order.setPrice(new BigDecimal("100"));
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        em.persist(order);
    }
}
