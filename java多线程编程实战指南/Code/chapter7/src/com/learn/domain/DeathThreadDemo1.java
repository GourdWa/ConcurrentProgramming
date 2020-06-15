package com.learn.domain;

/**
 * @author ZixiangHu
 * @create 2020-05-31  16:04
 * @description
 */
public class DeathThreadDemo1 {

    public static void main(String[] args) {
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();

        new Thread(() -> {
            synchronized (sb1) {
                System.out.println(Thread.currentThread().getName() + " is running");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (sb2) {
                    System.out.println(Thread.currentThread().getName() + " is ending");
                }
            }
        }).start();

        new Thread(() -> {
            synchronized (sb2) {
                System.out.println(Thread.currentThread().getName() + " is running");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (sb1) {
                    System.out.println(Thread.currentThread().getName() + " is ending");
                }
            }
        }).start();
    }
}
