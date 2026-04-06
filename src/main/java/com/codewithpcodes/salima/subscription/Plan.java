package com.codewithpcodes.salima.subscription;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum Plan {
    ULTRA_BASIC(
            BigDecimal.valueOf(2000),
            BigDecimal.valueOf(9999),
            3,
            "Outpatient consultations only"
    ),
    BASIC(
            BigDecimal.valueOf(10_000),
            BigDecimal.valueOf(49_999),
            5,
            "Outpatient + medication"
    ),
    STANDARD(
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(99_999),
            8,
            "Outpatient + medication + lab tests"
    ),
    PREMIUM(
            BigDecimal.valueOf(100_000),
            BigDecimal.valueOf(1_000_000),
            12,
            "Full coverage including specialist care"
    );

    private final BigDecimal minPayment;
    private final BigDecimal maxPayment;
    private final Integer claimMultiplier;
    private final String description;

    public boolean isValidPayment(BigDecimal amount) {
        return amount.compareTo(minPayment) >= 0 && amount.compareTo(maxPayment) <= 0;
    }

    public BigDecimal calculateClaimLimit(BigDecimal monthlyPayment) {
        return monthlyPayment.multiply(BigDecimal.valueOf(claimMultiplier));
    }

}
