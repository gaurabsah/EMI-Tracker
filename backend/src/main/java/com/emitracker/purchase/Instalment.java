package com.emitracker.purchase;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "instalments")
public class Instalment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @Column(nullable = false)
    private int seq;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private long principal;

    @Column(nullable = false)
    private long interest;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstalmentStatus status = InstalmentStatus.PENDING;

    @Column(name = "paid_on")
    private LocalDate paidOn;

    protected Instalment() {
    }

    Instalment(Purchase purchase, int seq, LocalDate dueDate, long principal, long interest, long amount) {
        this.purchase = purchase;
        this.seq = seq;
        this.dueDate = dueDate;
        this.principal = principal;
        this.interest = interest;
        this.amount = amount;
    }

    public void markPaid(LocalDate on) {
        this.status = InstalmentStatus.PAID;
        this.paidOn = on;
    }

    public boolean isOverdue(LocalDate today) {
        return status == InstalmentStatus.PENDING && dueDate.isBefore(today);
    }

    public Long getId() {
        return id;
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public int getSeq() {
        return seq;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public long getPrincipal() {
        return principal;
    }

    public long getInterest() {
        return interest;
    }

    public long getAmount() {
        return amount;
    }

    public InstalmentStatus getStatus() {
        return status;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }
}
