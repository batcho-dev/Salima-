package com.codewithpcodes.salima.subscription;

import com.codewithpcodes.salima.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    public ResponseEntity<List<PlanInfo>> getAvailablePlans() {
        List<PlanInfo> plans = Arrays.stream(Plan.values())
                .map(p -> PlanInfo.builder()
                        .planName(p.name())
                        .description(p.getDescription())
                        .minPayment(p.getMinPayment())
                        .maxPayment(p.getMaxPayment())
                        .claimMultiplier(p.getClaimMultiplier())
                        .exampleClaimLimit(p.calculateClaimLimit(p.getMinPayment()))
                        .build()
                )
                .collect(Collectors.toList());
                return ResponseEntity.ok(plans);
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscription(
            @Valid @RequestBody SubscriptionRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(request, currentUser));
    }

    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(currentUser.getId()));
    }

    @PatchMapping("/plan")
    public ResponseEntity<SubscriptionResponse> changePlan(
            @Valid @RequestBody SubscriptionRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(subscriptionService.changePlan(currentUser.getId(), request));
    }

    @PatchMapping("/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(currentUser.getId()));
    }
}
