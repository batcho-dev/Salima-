package com.codewithpcodes.salima.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByUserIdAndStatus(
            UUID userId,
            SubscriptionStatus status
    );

    @Query("select s from Subscription s where s.subscriptionStatus = 'ACTIVE'" +
            "and s.nextBillingDate <= :now")
    List<Subscription> findDueForBilling(LocalDateTime now);

    @Query("select s from Subscription s where s.subscriptionStatus = 'ACTIVE'" +
            "and s.endDate < :now")
    List<Subscription> findExpired(LocalDateTime now);

    @Query("select s from Subscription s where s.subscriptionStatus = 'PENDING'" +
            "and s.paymentRetryCount < 3")
    List<Subscription> findPendingRetries();

    boolean existsByUserIdAndStatus(UUID userId, SubscriptionStatus status);
}
