package com.codewithpcodes.salima.subscription;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {

    private String planName;
    private String planDescription;
    private BigDecimal monthlyPayment;
    private BigDecimal claimLimit;
    private BigDecimal claimedAmountThisMonth;
    private BigDecimal remainingClaimBudget;
    private String subscriptionStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime nextBillingDate;

}
