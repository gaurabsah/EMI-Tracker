package com.emitracker.purchase;

import java.time.LocalDate;
import java.util.List;

import com.emitracker.calc.InterestMethod;

/** Response shapes shared by the Company and Customer endpoints. Money is integer paise. */
public final class PurchaseViews {

    private PurchaseViews() {
    }

    public record InstalmentView(Long id, int seq, LocalDate dueDate, long principal, long interest, long amount,
            InstalmentStatus status, LocalDate paidOn, boolean overdue) {

        static InstalmentView of(Instalment i, LocalDate today) {
            return new InstalmentView(i.getId(), i.getSeq(), i.getDueDate(), i.getPrincipal(), i.getInterest(),
                    i.getAmount(), i.getStatus(), i.getPaidOn(), i.isOverdue(today));
        }
    }

    public record PurchaseSummary(Long id, Long customerId, String customerName, String itemName, long price,
            long downPayment, long financed, InterestMethod method, int annualRateBps, int tenureMonths,
            LocalDate startDate, long outstandingBalance, long overdueCount) {
    }

    public record PurchaseDetail(Long id, Long customerId, String customerName, String itemName, long price,
            long downPayment, long financed, InterestMethod method, int annualRateBps, int tenureMonths,
            LocalDate startDate, long outstandingBalance, long overdueCount, List<InstalmentView> instalments) {
    }
}
