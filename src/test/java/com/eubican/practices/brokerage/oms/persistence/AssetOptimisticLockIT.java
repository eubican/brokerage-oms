package com.eubican.practices.brokerage.oms.persistence;

import com.eubican.practices.brokerage.oms.OmsApplication;
import com.eubican.practices.brokerage.oms.persistence.entity.AssetEntity;
import com.eubican.practices.brokerage.oms.persistence.entity.CustomerEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@SpringBootTest(classes = OmsApplication.class)
class AssetOptimisticLockIT {

    @Autowired
    EntityManagerFactory emf;

    @Test
    void concurrentUpdatesOnSameAssetSecondCommitFailsWithOptimisticLock() {
        final UUID customerId = UUID.randomUUID();
        final String assetName = "TRY";

        seedCustomer(customerId);
        seedAsset(customerId, assetName, scale4("1000.0000"), scale4("0"));

        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();

        try {
            em1.getTransaction().begin();
            em2.getTransaction().begin();

            AssetEntity r1 = findAsset(em1, customerId, assetName);
            AssetEntity r2 = findAsset(em2, customerId, assetName);

            r1.setReserved(r1.getReserved().add(scale4("10.0000"))); // +10 TRY reserved
            r2.setReserved(r2.getReserved().add(scale4("5.0000"))); // +5 TRY reserved

            em1.getTransaction().commit();

            Assertions.assertThatThrownBy(() -> em2.getTransaction().commit())
                    .isInstanceOf(RollbackException.class)
                    .hasCauseInstanceOf(OptimisticLockException.class);

        } finally {
            safeClose(em1);
            safeClose(em2);
        }
    }

    @Test
    void concurrentUpdatesOnDifferentAssetsSucceedIndependently() {
        final UUID customerId = UUID.randomUUID();
        final String cash = "TRY";
        final String share = "XYZ";

        seedCustomer(customerId);
        seedAsset(customerId, cash, scale4("500.0000"), scale4("0"));
        seedAsset(customerId, share, scale6("100.000000"), scale6("0"));

        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();

        try {
            em1.getTransaction().begin();
            em2.getTransaction().begin();

            AssetEntity a1 = findAsset(em1, customerId, cash);
            a1.setReserved(a1.getReserved().add(scale4("25.0000"))); // +25 TRY reserved

            AssetEntity a2 = findAsset(em2, customerId, share);
            a2.setReserved(a2.getReserved().add(scale6("3.500000"))); // +3.5 TRY reserved

            // Both commits should succeed (different rows ⇒ different @Version columns)
            em1.getTransaction().commit();
            em2.getTransaction().commit();

        } finally {
            safeClose(em1);
            safeClose(em2);
        }

        withEm(em -> {
            AssetEntity cashRow = findAsset(em, customerId, cash);
            AssetEntity shareRow = findAsset(em, customerId, share);

            Assertions.assertThat(cashRow.getReserved()).isEqualByComparingTo(scale4("25.0000"));
            Assertions.assertThat(shareRow.getReserved()).isEqualByComparingTo(scale6("3.500000"));

            Assertions.assertThat(cashRow.getUsable().add(cashRow.getReserved()))
                    .isEqualByComparingTo(cashRow.getSize());
            Assertions.assertThat(shareRow.getUsable().add(shareRow.getReserved()))
                    .isEqualByComparingTo(shareRow.getSize());
        });
    }

    private void seedAsset(UUID customerId, String assetName, BigDecimal usable, BigDecimal reserved) {
        withEm(em -> {
            em.getTransaction().begin();
            AssetEntity a = new AssetEntity();
            a.setAssetName(assetName);
            a.setUsable(usable);
            a.setReserved(reserved);
            a.setSize(usable.add(reserved));
            a.setCustomer(em.getReference(CustomerEntity.class, customerId));
            em.persist(a);
            em.getTransaction().commit();
        });
    }

    private void seedCustomer(UUID customerId) {
        withEm(em -> {
            em.getTransaction().begin();
            CustomerEntity c = new CustomerEntity();
            c.setId(customerId);
            c.setEmail(customerId.toString() + "@test.local");
            c.setRole("ROLE_CUSTOMER");
            c.setPassword("dummy-hash");
            c.setCreatedAt(Instant.now());
            em.persist(c);
            em.getTransaction().commit();
        });
    }

    private AssetEntity findAsset(EntityManager em, UUID customerId, String assetName) {
        return em.createQuery("""
                            select a from AssetEntity a
                             where a.customer.id = :cid and a.assetName = :an
                        """, AssetEntity.class)
                .setParameter("cid", customerId)
                .setParameter("an", assetName)
                .getSingleResult();
    }

    private void withEm(java.util.function.Consumer<EntityManager> work) {
        EntityManager em = emf.createEntityManager();
        try {
            work.accept(em);
        } finally {
            safeClose(em);
        }
    }

    private void safeClose(EntityManager em) {
        if (em == null) return;
        try {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } catch (Exception ignored) {
        }
        try {
            em.close();
        } catch (Exception ignored) {
        }
    }

    private static BigDecimal scale4(String v) {
        return new BigDecimal(v).setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale6(String v) {
        return new BigDecimal(v).setScale(6, RoundingMode.HALF_UP);
    }
}
