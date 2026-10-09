package com.emitracker.calc;

import java.util.List;

public interface InterestCalculator {

    InterestMethod method();

    /**
     * @param financed   amount financed, in paise
     * @param annualRateBps annual rate in basis points (1200 = 12% p.a.)
     * @param tenureMonths  number of monthly instalments
     */
    List<InstalmentLine> schedule(long financed, int annualRateBps, int tenureMonths);
}