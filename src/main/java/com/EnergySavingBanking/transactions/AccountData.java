package com.EnergySavingBanking.transactions;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AccountData {
    private String accountNumber;
    private AtomicInteger debitCount;
    private AtomicInteger creditCount;
    private AtomicLong balance;

    private static final int DECIMAL_DIGITS = 2;
    private static final long PRECISION_MULTIPLIER = (long) Math.pow(10, DECIMAL_DIGITS);
    private static final double PRECISION_DIVIDER = Math.pow(10, DECIMAL_DIGITS);

    public AccountData(String accountNumber) {
        this.accountNumber = accountNumber;
        this.debitCount = new AtomicInteger(0);
        this.creditCount = new AtomicInteger(0);
        this.balance = new AtomicLong(0);
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void incrementDebitCount() {
        debitCount.incrementAndGet();
    }

    public void incrementCreditCount() {
        creditCount.incrementAndGet();
    }

    public void addToBalance(double amount) {
        long amountAsLong = (long) (amount * PRECISION_MULTIPLIER);
        balance.addAndGet(amountAsLong);
    }

    public void subtractFromBalance(double amount) {
        long amountAsLong = (long) (amount * PRECISION_MULTIPLIER);
        balance.addAndGet(-amountAsLong);
    }

    public int getDebitCount() {
        return debitCount.get();
    }

    public int getCreditCount() {
        return creditCount.get();
    }

    public double getBalance() {
        return balance.get() / PRECISION_DIVIDER;
    }
}
