package com.EnergySavingBanking.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Transaction {
    private String debitAccount;
    private String creditAccount;
    private BigDecimal amount;

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public Transaction(String debitAccount, String creditAccount, BigDecimal amount) {
        this.debitAccount = debitAccount;
        this.creditAccount = creditAccount;
        this.amount = setScaledValue(amount);
    }

    public String getDebitAccount() {
        return debitAccount;
    }

    public String getCreditAccount() {
        return creditAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    private BigDecimal setScaledValue(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING_MODE);
    }
}