package com.codewithpcodes.salima.subscription;

import com.codewithpcodes.salima.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public SubscriptionResponse subscribe(
            SubscriptionRequest request,
            User user
    ) {
        //1. Check if the user has an active subscription
        if (subscriptionRepository.existsByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE)) {
            throw new IllegalStateException("User already has an active subscription");
        }

        //2. Validate the payment amount falls within the plan's range
        Plan plan = request.plan();
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        if (!plan.isValidPayment(request.monthlyPayment())) {
            throw new IllegalArgumentException(String.format("Payment amount %s FCFA is outside %s plan range " +
                    "(%s - %s FCFA)",
                    currencyFormatter.format(request.monthlyPayment()),
                    plan.name(),
                    currencyFormatter.format(plan.getMinPayment()),
                    currencyFormatter.format(plan.getMaxPayment())
            ));
        }

        //3. Calculate the Claim Limit
        BigDecimal claimLimit = plan.calculateClaimLimit(request.monthlyPayment());

        LocalDateTime now = LocalDateTime.now();

        Subscription subscription = Subscription.builder()
                .user(user)
                .planName(plan)
                .monthlyPayment(request.monthlyPayment())
                .claimLimit(claimLimit)
                .claimedAmountThisMonth(BigDecimal.ZERO)
                .subscriptionStatus(SubscriptionStatus.PENDING)
                .startDate(now)
                .endDate(now.plusDays(30))
                .nextBillingDate(now)
                .paymentRetryCount(0)
                .createdAt(now)
                .build();
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        log.info("Subscription created for user {} with plan {}", user.getId(), plan.name());

        //4. Trigger the first payment immediately
        processPayment(savedSubscription);

        return buildResponse(savedSubscription);
    }


    private SubscriptionResponse buildResponse(Subscription sub) {
        Plan plan = sub.getPlanName();
        return SubscriptionResponse.builder()
                .planName(plan.name())
                .planDescription(plan.getDescription())
                .monthlyPayment(sub.getMonthlyPayment())
                .claimLimit(sub.getClaimLimit())
                .subscriptionStatus(sub.getSubscriptionStatus().name())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .nextBillingDate(sub.getNextBillingDate())
                .build();
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(UUID userId) {
        Subscription subscription = getActiveSubscription(userId);

        subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());

        Subscription saved = subscriptionRepository.save(subscription);

        log.info("Subscription cancelled for user {}", userId);

        return buildResponse(saved);
    }

    private Subscription getActiveSubscription(UUID userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("User does not have an active subscription"));
    }

    @Transactional
    public SubscriptionResponse changePlan(UUID userId, SubscriptionRequest request) {
        Subscription subscription = getActiveSubscription(userId);

        Plan newPlan = request.plan();

        // Validate new Payment amount
        if (!newPlan.isValidPayment(request.monthlyPayment())) {
            throw new IllegalArgumentException(String.format("Payment amount %s FCFA is outside %s plan range " +
                    "(%s - %s FCFA)",
                    request.monthlyPayment(),
                    newPlan.name(),
                    newPlan.getMinPayment(),
                    newPlan.getMaxPayment()
            ));
        }

        BigDecimal claimLimit = newPlan.calculateClaimLimit(request.monthlyPayment());
        subscription.setPlanName(newPlan);
        subscription.setMonthlyPayment(request.monthlyPayment());
        subscription.setClaimLimit(claimLimit);
        subscription.setClaimedAmountThisMonth(BigDecimal.ZERO);

        log.info("Subscription plan changed for user {} to {}", userId, newPlan.name());

        return buildResponse(subscriptionRepository.save(subscription));
    }

    public SubscriptionResponse getCurrentSubscription(UUID userId) {
        return buildResponse(getActiveSubscription(userId));
    }

    public void validateClaimEligibility(UUID userId, BigDecimal claimAmount) {

        Subscription subscription = getActiveSubscription(userId);
        BigDecimal remaining = subscription.getClaimLimit().subtract(subscription.getClaimedAmountThisMonth());

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        if (claimAmount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException(String.format("Claim amount of %s FCFA exceeds your remaining "+
            "monthly claim budget of %s FCFA",
                    currencyFormatter.format(claimAmount),
                    currencyFormatter.format(remaining)
            ));
        }
    }

    @Transactional
    public void recordApprovedClaim(UUID userId, BigDecimal claimAmount) {
        Subscription subscription = getActiveSubscription(userId);
        subscription.setClaimedAmountThisMonth(subscription.getClaimedAmountThisMonth().add(claimAmount));
        subscriptionRepository.save(subscription);
    }

    //Auto-renew subscriptions
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processAutoRenewals() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> due = subscriptionRepository.findDueForBilling(now);

        log.info("Auto-renewal: {} subscriptions due", due.size());
        due.forEach(this::processPayment);
    }

    //Expire subscriptions
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireSubscriptions() {
        List<Subscription> expired = subscriptionRepository.findExpired(LocalDateTime.now());

        expired.forEach(sub -> {
            sub.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            log.info("Subscription expired for user: {}", sub.getUser().getId());
        });
    }

    //Retry failed payments
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void retryPendingPayments() {
        List<Subscription> pending = subscriptionRepository.findPendingRetries();

        log.info("Retrying {} pending payments", pending.size());
        pending.forEach(this::processPayment);
    }

    //reset claims  at the first of every month
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void resetMonthlyClaimedAmounts() {
        List<Subscription> active = subscriptionRepository
                .findAll()
                .stream()
                .filter(s -> s.getSubscriptionStatus() == SubscriptionStatus.ACTIVE)
                .toList();

        active.forEach(sub -> {
            sub.setClaimedAmountThisMonth(BigDecimal.ZERO);
            subscriptionRepository.save(sub);
        });

        log.info("Monthly claims amount reset for {} active subscriptions", active.size());
    }

    private void processPayment(Subscription subscription) {
        try {
            // MOMO client here
            boolean success = true;
            if (success) onPaymentSuccess(subscription);
            else onPaymentFailure(subscription);
        } catch (Exception e) {
            log.error("Payment error for subscription {}: {}", subscription.getId(), e.getMessage());
            onPaymentFailure(subscription);
        }
    }

    private void onPaymentFailure(Subscription subscription) {
        int retries = subscription.getPaymentRetryCount() + 1;
        subscription.setPaymentRetryCount(retries);

        if (retries >= 3) {
            subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
            log.warn("Subscription {} expired after 3 failed payments", subscription.getId());
        }

        subscriptionRepository.save(subscription);
        log.warn("Payment failed (attempt {}) for subscription {}", retries, subscription.getId());
    }

    private void onPaymentSuccess(Subscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        subscription.setEndDate(now.plusDays(30));
        subscription.setNextBillingDate(now.plusDays(30));
        subscription.setPaymentRetryCount(0);
        subscriptionRepository.save(subscription);

        log.info("Payment successful - subscription {} renewed", subscription.getId());
    }

}
