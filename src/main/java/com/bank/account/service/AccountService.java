package com.bank.account.service;

import com.bank.account.entity.Account;
import com.bank.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

@Service
public class AccountService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    // Method to create or initialize account with balance
    public Account createAccount(double balance) {
        Account account = new Account(balance);
        return accountRepository.save(account);
    }

    // Pessimistic lock-based withdrawal
    @Transactional
    public void withdrawWithPessimisticLock(Long accountId, double amount) {
        Account account = entityManager.find(Account.class, accountId, LockModeType.PESSIMISTIC_WRITE);

        if (account.getBalance() >= amount) {
            account.setBalance(account.getBalance() - amount);
            accountRepository.save(account);
        } else {
            throw new RuntimeException("Insufficient funds");
        }
    }

    // Optimistic lock-based withdrawal
    @Transactional
    public void withdrawWithOptimisticLock(Long accountId, double amount) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getBalance() >= amount) {
            account.setBalance(account.getBalance() - amount);
            accountRepository.save(account);
        } else {
            throw new RuntimeException("Insufficient funds");
        }
    }

    // Helper function to get account balance for verification
    public double getBalance(Long accountId) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));
        return account.getBalance();
    }
}
