package com.learn.domain;

import java.util.Date;
import java.util.concurrent.Semaphore;

/**
 * @author ZixiangHu
 * @create 2020-05-29  22:52
 * @description
 */
public class SemaphoreTest {
    public static void main(String[] args) {
        //同一个时刻只能允许三个线程对资源进行访问
        Semaphore semaphore = new Semaphore(3);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    System.out.println(new Date(System.currentTimeMillis()) + " " + Thread.currentThread().getName() + " is running...");
                    Thread.sleep(1000);
                    System.out.println(new Date(System.currentTimeMillis()) + " " + Thread.currentThread().getName() + " end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    semaphore.release();
                }
            }).start();
        }
    }
}
