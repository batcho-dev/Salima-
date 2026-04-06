package com.codewithpcodes.salima.subscription;

import com.codewithpcodes.salima.claim.Claim;
import com.codewithpcodes.salima.payment.Payment;
import com.codewithpcodes.salima.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan planName;

    @Column(nullable = false)
    private BigDecimal monthlyPayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus subscriptionStatus;
    private int waitingPeriodInDays;
    private BigDecimal claimLimit;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime nextBillingDate;

    @Column(nullable = false)
    @Builder.Default
    private int paymentRetryCount = 0;

    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "subscription")
    private List<Claim> claims;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "subscription")
    private List<Payment> payments;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
