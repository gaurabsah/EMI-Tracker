package com.emitracker.calc;

/** One scheduled instalment. All amounts are integer paise. */
public record InstalmentLine(int seq, long principal, long interest, long amount) {
}