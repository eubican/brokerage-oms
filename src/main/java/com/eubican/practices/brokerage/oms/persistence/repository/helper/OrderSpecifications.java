package com.eubican.practices.brokerage.oms.persistence.repository.helper;

import com.eubican.practices.brokerage.oms.domain.model.OrderStatus;
import com.eubican.practices.brokerage.oms.persistence.entity.OrderEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OrderSpecifications {

    private OrderSpecifications() {
        throw new AssertionError("Cannot instantiate utility class.");
    }

    public static Specification<OrderEntity> byFilters(
            UUID customerId,
            Instant from,
            Instant to,
            OrderStatus status,
            String assetName
    ) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            // required filters
            preds.add(cb.equal(root.get("customer").get("id"), customerId));
            preds.add(cb.between(root.get("createdAt"), from, to));

            // optional filters
            if (status != null) {
                preds.add(cb.equal(root.get("status"), status));
            }
            if (assetName != null && !assetName.isBlank()) {
                String pattern = "%" + assetName.toLowerCase() + "%";
                preds.add(cb.like(cb.lower(root.get("assetName")), pattern));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
