package com.bank.account;

import com.bank.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.TransactionSystemException;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = Application.class)
public class TestAccountService {

    @Autowired
    private AccountService accountService;

    private Long accountId;

    @BeforeEach
    public void setUp() {
        accountId = accountService.createAccount(1000.00).getId();
    }

    @Test
    public void testPessimisticLocking() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try {
                accountService.withdrawWithPessimisticLock(accountId, 500.00);
            } finally {
                latch.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                accountService.withdrawWithPessimisticLock(accountId, 500.00);
            } finally {
                latch.countDown();
            }
        });

        t1.start();
        t2.start();

        latch.await();
        assertEquals(0.00, accountService.getBalance(accountId));
    }

    @Test
    public void testOptimisticLocking() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try {
                accountService.withdrawWithOptimisticLock(accountId, 500.00);
            } catch (TransactionSystemException e) {
                System.out.println("Optimistic lock exception in thread 1: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                accountService.withdrawWithOptimisticLock(accountId, 500.00);
            } catch (TransactionSystemException e) {
                System.out.println("Optimistic lock exception in thread 2: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        t1.start();
        t2.start();

        latch.await();
        assertEquals(500.00, accountService.getBalance(accountId));
    }
}
