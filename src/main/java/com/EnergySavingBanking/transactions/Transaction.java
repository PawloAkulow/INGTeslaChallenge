package com.EnergySavingBanking.transactions;

public class Transaction {
    private String debitAccount;
    private String creditAccount;
    private double amount;

    public Transaction(String debitAccount, String creditAccount, double amount) {
        this.debitAccount = debitAccount;
        this.creditAccount = creditAccount;
        this.amount = amount;
    }

    public String getDebitAccount() {
        return debitAccount;
    }

    public String getCreditAccount() {
        return creditAccount;
    }

    public double getAmount() {
        return amount;
    }
}
