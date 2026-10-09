package com.emitracker.purchase;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emitracker.purchase.PurchaseViews.InstalmentView;
import com.emitracker.security.AuthPrincipal;

@RestController
@RequestMapping("/api/instalments")
public class InstalmentController {

    private final PurchaseService service;

    public InstalmentController(PurchaseService service) {
        this.service = service;
    }

    @PostMapping("/{id}/pay")
    public InstalmentView pay(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
        return service.pay(me.companyId(), id);
    }
}
