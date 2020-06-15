package com.learn.domain;

/**
 * @author ZixiangHu
 * @create 2020-05-30  15:32
 * @description
 */
public class ThreadLocalTest1 {
    static ThreadLocal<Integer> threadLocal = new ThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " " + threadLocal.get());
            threadLocal.set(1);
            System.out.println(Thread.currentThread().getName() + " " + threadLocal.get());
        });
        Thread t2 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " " + threadLocal.get());
            threadLocal.set(1);
            System.out.println(Thread.currentThread().getName() + " " + threadLocal.get());
        });
        t1.start();
        t1.join();
        t2.start();
    }
}
