package com.emitracker.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ReducingBalanceCalculator implements InterestCalculator {

    private static final MathContext MC = new MathContext(40, RoundingMode.HALF_UP);
    private static final BigDecimal BPS_PER_MONTH_DIVISOR = BigDecimal.valueOf(120_000);

    @Override
    public InterestMethod method() {
        return InterestMethod.REDUCING;
    }

    @Override
    public List<InstalmentLine> schedule(long financed, int annualRateBps, int tenureMonths) {
        BigDecimal r = BigDecimal.valueOf(annualRateBps).divide(BPS_PER_MONTH_DIVISOR, MC);
        long emi = emi(financed, r, tenureMonths);

        List<InstalmentLine> lines = new ArrayList<>(tenureMonths);
        long balance = financed;
        for (int seq = 1; seq <= tenureMonths; seq++) {
            long interest = round(BigDecimal.valueOf(balance).multiply(r, MC));
            if (seq == tenureMonths) {
                lines.add(new InstalmentLine(seq, balance, interest, balance + interest));
            } else {
                long principal = emi - interest;
                lines.add(new InstalmentLine(seq, principal, interest, emi));
                balance -= principal;
            }
        }
        return lines;
    }

    private static long emi(long financed, BigDecimal r, int n) {
        BigDecimal p = BigDecimal.valueOf(financed);
        if (r.signum() == 0) {
            return round(p.divide(BigDecimal.valueOf(n), MC));
        }
        BigDecimal growth = BigDecimal.ONE.add(r).pow(n, MC);
        BigDecimal emi = p.multiply(r, MC).multiply(growth, MC)
                .divide(growth.subtract(BigDecimal.ONE), MC);
        return round(emi);
    }

    private static long round(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
