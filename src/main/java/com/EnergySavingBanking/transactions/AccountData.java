package com.EnergySavingBanking.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AccountData {
    private String accountNumber;
    private AtomicInteger debitCount;
    private AtomicInteger creditCount;
    private AtomicReference<BigDecimal> balance;

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public AccountData(String accountNumber) {
        this.accountNumber = accountNumber;
        this.debitCount = new AtomicInteger(0);
        this.creditCount = new AtomicInteger(0);
        this.balance = new AtomicReference<>(setScaledValue(BigDecimal.ZERO));
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

    public void addToBalance(BigDecimal amount) {
        balance.updateAndGet(b -> setScaledValue(b.add(amount)));
    }

    public void subtractFromBalance(BigDecimal amount) {
        balance.updateAndGet(b -> setScaledValue(b.subtract(amount)));
    }

    public int getDebitCount() {
        return debitCount.get();
    }

    public int getCreditCount() {
        return creditCount.get();
    }

    public BigDecimal getBalance() {
        return balance.get();
    }

    private BigDecimal setScaledValue(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING_MODE);
    }
}