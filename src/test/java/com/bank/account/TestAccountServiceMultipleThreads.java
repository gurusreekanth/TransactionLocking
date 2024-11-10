package com.bank.account;

import com.bank.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.TransactionSystemException;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = Application.class)
public class TestAccountServiceMultipleThreads {

    @Autowired
    private AccountService accountService;

    private Long accountId;
    private static final double INITIAL_BALANCE = 1000.00;
    private static final int NUM_THREADS = 10;

    @BeforeEach
    public void setUp() {
        accountId = accountService.createAccount(INITIAL_BALANCE).getId();
    }

    @Test
    public void testRealTimeContentionWithPessimisticLocking() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        Random random = new Random();

        for (int i = 0; i < NUM_THREADS; i++) {
            executorService.submit(() -> {
                try {
                    // Random sleep to simulate real-world delay
                    Thread.sleep(random.nextInt(100));
                    accountService.withdrawWithPessimisticLock(accountId, 100.00);
                } catch (Exception e) {
                    System.out.println("Exception in thread: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
        assertEquals(0.00, accountService.getBalance(accountId), "Balance should be zero after all withdrawals");
    }

    @Test
    public void testRealTimeContentionWithOptimisticLocking() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        Random random = new Random();

        // Track successful withdrawals
        int[] successfulWithdrawals = {0};

        for (int i = 0; i < NUM_THREADS; i++) {
            executorService.submit(() -> {
                try {
                    // Random sleep to simulate real-world delay
                    Thread.sleep(random.nextInt(100));
                    accountService.withdrawWithOptimisticLock(accountId, 100.00);

                    // If withdrawal is successful, increment count
                    synchronized (successfulWithdrawals) {
                        successfulWithdrawals[0]++;
                    }
                } catch (TransactionSystemException e) {
                    System.out.println("Optimistic lock exception: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Thread interrupted: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        Thread.sleep(2000);

        // Calculate expected balance based on successful withdrawals
        double expectedBalance = INITIAL_BALANCE - (successfulWithdrawals[0] * 100.00);
        double remainingBalance = accountService.getBalance(accountId);

        System.out.println("Final balance after contention with optimistic locking: " + remainingBalance);
        System.out.println("Successful withdrawals: " + successfulWithdrawals[0]);
        assertEquals(expectedBalance, remainingBalance,
                "Balance after contention should match successful transactions");

    }
}
