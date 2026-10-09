package com.emitracker.calc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalculatorTest {

    private static final long FINANCED = 5_000_000L; // Rs 70,000 price - Rs 20,000 down, in paise

    private final InterestCalculator reducing = new ReducingBalanceCalculator();
    private final InterestCalculator flat = new FlatRateCalculator();

    @Test
    void reducingBalanceEmiMatchesHandCheckedValue() {
        List<InstalmentLine> lines = reducing.schedule(FINANCED, 1200, 12);

        assertThat(lines.get(0).amount()).isEqualTo(444_244L); // Rs 4,442.44
        assertThat(lines.get(0).interest()).isEqualTo(50_000L); // 1% of Rs 50,000
        assertThat(lines.get(0).principal()).isEqualTo(394_244L);
    }

    @Test
    void flatRateInstalmentMatchesHandCheckedValue() {
        List<InstalmentLine> lines = flat.schedule(FINANCED, 1200, 12);

        assertThat(lines.get(0).amount()).isEqualTo(466_667L); // Rs 4,666.67
        assertThat(lines.stream().mapToLong(InstalmentLine::interest).sum()).isEqualTo(600_000L); // Rs 6,000
        assertThat(lines.get(11).amount()).isEqualTo(466_663L); // last absorbs the remainder
    }

    @ParameterizedTest
    @CsvSource({
            "5000000, 1200, 12",
            "5000000, 0, 12",
            "5000000, 1200, 1",
            "1000001, 1850, 7",
            "9999999, 2499, 36",
            "100, 1200, 3",
            "123456789, 999, 60"
    })
    void invariantsHoldForBothMethods(long financed, int rateBps, int tenure) {
        for (InterestCalculator calculator : List.of(reducing, flat)) {
            List<InstalmentLine> lines = calculator.schedule(financed, rateBps, tenure);

            assertThat(lines).hasSize(tenure);
            assertThat(lines.stream().mapToLong(InstalmentLine::principal).sum()).isEqualTo(financed);

            long balance = financed;
            for (int i = 0; i < lines.size(); i++) {
                InstalmentLine line = lines.get(i);
                assertThat(line.seq()).isEqualTo(i + 1);
                assertThat(line.amount()).isEqualTo(line.principal() + line.interest());
                assertThat(line.principal()).isPositive();
                assertThat(line.interest()).isGreaterThanOrEqualTo(0);
                balance -= line.principal();
            }
            assertThat(balance).isZero();
        }
    }

    @Test
    void zeroRateChargesNoInterest() {
        for (InterestCalculator calculator : List.of(reducing, flat)) {
            List<InstalmentLine> lines = calculator.schedule(FINANCED, 0, 12);
            assertThat(lines.stream().mapToLong(InstalmentLine::interest).sum()).isZero();
        }
    }

    @Test
    void singleInstalmentClearsEverything() {
        List<InstalmentLine> lines = reducing.schedule(FINANCED, 1200, 1);

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).principal()).isEqualTo(FINANCED);
        assertThat(lines.get(0).interest()).isEqualTo(50_000L);
    }
}
