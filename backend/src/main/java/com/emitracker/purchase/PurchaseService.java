package com.emitracker.purchase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emitracker.calc.InterestCalculator;
import com.emitracker.calc.InterestMethod;
import com.emitracker.common.ApiException;
import com.emitracker.customer.Customer;
import com.emitracker.customer.CustomerRepository;
import com.emitracker.plan.Plan;
import com.emitracker.plan.PlanRepository;
import com.emitracker.purchase.PurchaseViews.InstalmentView;
import com.emitracker.purchase.PurchaseViews.PurchaseDetail;
import com.emitracker.purchase.PurchaseViews.PurchaseSummary;

@Service
public class PurchaseService {

    private final PurchaseRepository purchases;
    private final InstalmentRepository instalments;
    private final PlanRepository plans;
    private final CustomerRepository customers;
    private final Map<InterestMethod, InterestCalculator> calculators = new EnumMap<>(InterestMethod.class);
    private final Clock clock;

    public PurchaseService(PurchaseRepository purchases, InstalmentRepository instalments, PlanRepository plans,
            CustomerRepository customers, List<InterestCalculator> calculatorBeans, Clock clock) {
        this.purchases = purchases;
        this.instalments = instalments;
        this.plans = plans;
        this.customers = customers;
        this.clock = clock;
        calculatorBeans.forEach(c -> calculators.put(c.method(), c));
    }

    @Transactional
    public PurchaseDetail create(Long companyId, Long customerId, Long planId, String itemName, long price,
            long downPayment, LocalDate startDate) {
        if (downPayment < 0 || downPayment >= price) {
            throw ApiException.badRequest("downPayment must be at least 0 and less than price");
        }
        Customer customer = customers.findByIdAndCompanyId(customerId, companyId)
                .orElseThrow(() -> ApiException.notFound("Customer not found"));
        Plan plan = plans.findByIdAndCompanyId(planId, companyId)
                .orElseThrow(() -> ApiException.notFound("Plan not found"));

        LocalDate start = startDate != null ? startDate : today();
        Purchase purchase = new Purchase(companyId, customer.getId(), itemName.trim(), price, downPayment,
                plan.getMethod(), plan.getAnnualRateBps(), plan.getTenureMonths(), start);
        purchase.addSchedule(calculators.get(plan.getMethod())
                .schedule(purchase.getFinanced(), plan.getAnnualRateBps(), plan.getTenureMonths()));

        return detail(purchases.save(purchase), customer.getName());
    }

    @Transactional(readOnly = true)
    public List<PurchaseSummary> list(Long companyId) {
        return summaries(purchases.findByCompanyIdOrderByIdDesc(companyId));
    }

    @Transactional(readOnly = true)
    public PurchaseDetail get(Long companyId, Long id) {
        Purchase purchase = purchases.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> ApiException.notFound("Purchase not found"));
        return detail(purchase, customerName(purchase.getCustomerId()));
    }

    @Transactional
    public InstalmentView pay(Long companyId, Long instalmentId) {
        Instalment instalment = instalments.findForPayment(instalmentId, companyId)
                .orElseThrow(() -> ApiException.notFound("Instalment not found"));
        if (instalment.getStatus() == InstalmentStatus.PAID) {
            throw ApiException.conflict("Instalment is already paid");
        }
        instalment.markPaid(today());
        return InstalmentView.of(instalment, today());
    }

    @Transactional(readOnly = true)
    public List<PurchaseSummary> listForCustomer(Long customerId) {
        return summaries(purchases.findByCustomerIdOrderByIdDesc(customerId));
    }

    @Transactional(readOnly = true)
    public PurchaseDetail getForCustomer(Long customerId, Long id) {
        Purchase purchase = purchases.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> ApiException.notFound("Purchase not found"));
        return detail(purchase, customerName(customerId));
    }

    private List<PurchaseSummary> summaries(List<Purchase> list) {
        LocalDate today = today();
        Map<Long, String> names = customers.findAllById(list.stream().map(Purchase::getCustomerId).distinct().toList())
                .stream().collect(Collectors.toMap(Customer::getId, Customer::getName));
        return list.stream().map(p -> summary(p, names.get(p.getCustomerId()), today)).toList();
    }

    private PurchaseSummary summary(Purchase p, String customerName, LocalDate today) {
        return new PurchaseSummary(p.getId(), p.getCustomerId(), customerName, p.getItemName(), p.getPrice(),
                p.getDownPayment(), p.getFinanced(), p.getMethod(), p.getAnnualRateBps(), p.getTenureMonths(),
                p.getStartDate(), p.outstandingBalance(), overdueCount(p, today));
    }

    private PurchaseDetail detail(Purchase p, String customerName) {
        LocalDate today = today();
        return new PurchaseDetail(p.getId(), p.getCustomerId(), customerName, p.getItemName(), p.getPrice(),
                p.getDownPayment(), p.getFinanced(), p.getMethod(), p.getAnnualRateBps(), p.getTenureMonths(),
                p.getStartDate(), p.outstandingBalance(), overdueCount(p, today),
                p.getInstalments().stream().map(i -> InstalmentView.of(i, today)).toList());
    }

    private static long overdueCount(Purchase p, LocalDate today) {
        return p.getInstalments().stream().filter(i -> i.isOverdue(today)).count();
    }

    private String customerName(Long customerId) {
        return customers.findById(customerId).map(Customer::getName).orElse(null);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
