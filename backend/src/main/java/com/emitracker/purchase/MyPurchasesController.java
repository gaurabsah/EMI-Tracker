package com.emitracker.purchase;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emitracker.common.ApiException;
import com.emitracker.purchase.PurchaseViews.PurchaseDetail;
import com.emitracker.purchase.PurchaseViews.PurchaseSummary;
import com.emitracker.security.AuthPrincipal;

@RestController
@RequestMapping("/api/me/purchases")
public class MyPurchasesController {

    private final PurchaseService service;

    public MyPurchasesController(PurchaseService service) {
        this.service = service;
    }

    @GetMapping
    public List<PurchaseSummary> list(@AuthenticationPrincipal AuthPrincipal me) {
        return service.listForCustomer(customerId(me));
    }

    @GetMapping("/{id}")
    public PurchaseDetail get(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
        return service.getForCustomer(customerId(me), id);
    }

    private static Long customerId(AuthPrincipal me) {
        if (me.customerId() == null) {
            throw ApiException.notFound("Customer profile not found");
        }
        return me.customerId();
    }
}
