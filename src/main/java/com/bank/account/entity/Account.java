package com.bank.account.entity;

import javax.persistence.*;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private double balance;

    @Version
    private Long version;  // For optimistic locking

    public Account() {}

    public Account(double balance) {
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Long getVersion() {
        return version;
    }
}
