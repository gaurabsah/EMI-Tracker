package com.emitracker.plan;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.emitracker.calc.InterestMethod;
import com.emitracker.security.AuthPrincipal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    public record CreatePlanRequest(
            @NotBlank String name,
            @NotNull InterestMethod method,
            @Min(0) @Max(10_000) int annualRateBps,
            @Min(1) @Max(360) int tenureMonths) {
    }

    public record PlanView(Long id, String name, InterestMethod method, int annualRateBps, int tenureMonths) {
        static PlanView of(Plan p) {
            return new PlanView(p.getId(), p.getName(), p.getMethod(), p.getAnnualRateBps(), p.getTenureMonths());
        }
    }

    private final PlanRepository plans;

    public PlanController(PlanRepository plans) {
        this.plans = plans;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanView create(@AuthenticationPrincipal AuthPrincipal me, @Valid @RequestBody CreatePlanRequest r) {
        Plan plan = plans.save(new Plan(me.companyId(), r.name().trim(), r.method(), r.annualRateBps(), r.tenureMonths()));
        return PlanView.of(plan);
    }

    @GetMapping
    public List<PlanView> list(@AuthenticationPrincipal AuthPrincipal me) {
        return plans.findByCompanyIdOrderByIdAsc(me.companyId()).stream().map(PlanView::of).toList();
    }
}
