package com.codewithpcodes.salima.subscription;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlanInfo {
    private String planName;
    private String description;
    private BigDecimal minPayment;
    private BigDecimal maxPayment;
    private Integer claimMultiplier;
    private BigDecimal exampleClaimLimit;
}
