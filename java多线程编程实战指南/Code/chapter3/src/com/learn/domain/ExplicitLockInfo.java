package com.learn.domain;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ZixiangHu
 * @create 2020-05-16  12:37
 * @description
 */
public class ExplicitLockInfo {
    private static final Lock LOCK = new ReentrantLock();
    private static int shareData = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(() -> {
            LOCK.lock();
            try {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                shareData = 1;
            } finally {
                LOCK.unlock();
            }
        });
        t.start();
        Thread.sleep(10);
        LOCK.lock();
        try {
            System.out.println("shareData: " + shareData);
        } finally {
            LOCK.unlock();
        }
    }
}
