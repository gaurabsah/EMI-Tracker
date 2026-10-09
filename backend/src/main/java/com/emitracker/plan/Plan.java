package com.emitracker.plan;

import com.emitracker.calc.InterestMethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterestMethod method;

    @Column(name = "annual_rate_bps", nullable = false)
    private int annualRateBps;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    protected Plan() {
    }

    public Plan(Long companyId, String name, InterestMethod method, int annualRateBps, int tenureMonths) {
        this.companyId = companyId;
        this.name = name;
        this.method = method;
        this.annualRateBps = annualRateBps;
        this.tenureMonths = tenureMonths;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public InterestMethod getMethod() {
        return method;
    }

    public int getAnnualRateBps() {
        return annualRateBps;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }
}
