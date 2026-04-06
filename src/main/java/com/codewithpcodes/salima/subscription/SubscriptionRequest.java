package com.codewithpcodes.salima.subscription;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SubscriptionRequest(
        @NotNull(message = "Plan is required")
        Plan plan,

        @NotNull(message = "Monthly payment amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal monthlyPayment
) {
}
