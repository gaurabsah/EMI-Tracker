package com.emitracker.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class FlatRateCalculator implements InterestCalculator {

    @Override
    public InterestMethod method() {
        return InterestMethod.FLAT;
    }

    @Override
    public List<InstalmentLine> schedule(long financed, int annualRateBps, int tenureMonths) {
        // total interest = P * annual_rate * n / 12, with the rate held in basis points
        long totalInterest = divideHalfUp(
                BigDecimal.valueOf(financed)
                        .multiply(BigDecimal.valueOf(annualRateBps))
                        .multiply(BigDecimal.valueOf(tenureMonths)),
                BigDecimal.valueOf(120_000));

        long amount = divideHalfUp(BigDecimal.valueOf(financed + totalInterest), BigDecimal.valueOf(tenureMonths));
        long interest = divideHalfUp(BigDecimal.valueOf(totalInterest), BigDecimal.valueOf(tenureMonths));
        long principal = amount - interest;

        List<InstalmentLine> lines = new ArrayList<>(tenureMonths);
        long principalPaid = 0;
        long interestPaid = 0;
        for (int seq = 1; seq < tenureMonths; seq++) {
            lines.add(new InstalmentLine(seq, principal, interest, amount));
            principalPaid += principal;
            interestPaid += interest;
        }
        long lastPrincipal = financed - principalPaid;
        long lastInterest = totalInterest - interestPaid;
        lines.add(new InstalmentLine(tenureMonths, lastPrincipal, lastInterest, lastPrincipal + lastInterest));
        return lines;
    }

    private static long divideHalfUp(BigDecimal numerator, BigDecimal denominator) {
        return numerator.divide(denominator, 0, RoundingMode.HALF_UP).longValueExact();
    }
}
