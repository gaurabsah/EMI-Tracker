package com.emitracker.purchase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.emitracker.purchase.PurchaseViews.PurchaseDetail;
import com.emitracker.purchase.PurchaseViews.PurchaseSummary;
import com.emitracker.security.AuthPrincipal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    public record CreatePurchaseRequest(
            @NotNull Long customerId,
            @NotNull Long planId,
            @NotBlank String itemName,
            @Min(1) long price,
            @Min(0) long downPayment,
            LocalDate startDate) {
    }

    private final PurchaseService service;

    public PurchaseController(PurchaseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseDetail create(@AuthenticationPrincipal AuthPrincipal me,
            @Valid @RequestBody CreatePurchaseRequest r) {
        return service.create(me.companyId(), r.customerId(), r.planId(), r.itemName(), r.price(),
                r.downPayment(), r.startDate());
    }

    @GetMapping
    public List<PurchaseSummary> list(@AuthenticationPrincipal AuthPrincipal me) {
        return service.list(me.companyId());
    }

    @GetMapping("/{id}")
    public PurchaseDetail get(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
        return service.get(me.companyId(), id);
    }
}
