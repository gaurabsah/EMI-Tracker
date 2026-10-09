package com.emitracker.purchase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.emitracker.calc.InstalmentLine;
import com.emitracker.calc.InterestMethod;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(nullable = false)
    private long price;

    @Column(name = "down_payment", nullable = false)
    private long downPayment;

    @Column(nullable = false)
    private long financed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterestMethod method;

    @Column(name = "annual_rate_bps", nullable = false)
    private int annualRateBps;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<Instalment> instalments = new ArrayList<>();

    protected Purchase() {
    }

    public Purchase(Long companyId, Long customerId, String itemName, long price, long downPayment,
            InterestMethod method, int annualRateBps, int tenureMonths, LocalDate startDate) {
        this.companyId = companyId;
        this.customerId = customerId;
        this.itemName = itemName;
        this.price = price;
        this.downPayment = downPayment;
        this.financed = price - downPayment;
        this.method = method;
        this.annualRateBps = annualRateBps;
        this.tenureMonths = tenureMonths;
        this.startDate = startDate;
    }

    public void addSchedule(List<InstalmentLine> lines) {
        for (InstalmentLine line : lines) {
            instalments.add(new Instalment(this, line.seq(), startDate.plusMonths(line.seq()),
                    line.principal(), line.interest(), line.amount()));
        }
    }

    public long outstandingBalance() {
        return instalments.stream()
                .filter(i -> i.getStatus() == InstalmentStatus.PENDING)
                .mapToLong(Instalment::getPrincipal)
                .sum();
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getItemName() {
        return itemName;
    }

    public long getPrice() {
        return price;
    }

    public long getDownPayment() {
        return downPayment;
    }

    public long getFinanced() {
        return financed;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public List<Instalment> getInstalments() {
        return instalments;
    }
}
