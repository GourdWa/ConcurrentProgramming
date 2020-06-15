package com.learn.domain;

import com.learn.utils.Tools;
import org.w3c.dom.css.Counter;

/**
 * @author ZixiangHu
 * @create 2020-05-08  12:05
 * @description
 */
public class ThreadCreationCmp {
    public static void main(String[] args) {
        Thread t;
        CountingTask ct = new CountingTask();
        final int numberOfProcessors = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 2 * numberOfProcessors; i++) {
            //直接创建线程
            t = new Thread(ct);
            t.start();
        }
        for (int i = 0; i < 2 * numberOfProcessors; i++) {
            //以子类的方式创建线程
            t = new CountingThread();
            t.start();
        }
    }

    static class Counter {
        private int count = 0;

        public void increment() {
            count++;
        }

        public int value() {
            return count;
        }
    }

    static class CountingTask implements Runnable {
        private Counter counter = new Counter();

        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                doSomething();
                counter.increment();
            }
            System.out.println(Thread.currentThread().getName() + " CountingTask:" + counter.value());
        }

        private void doSomething() {
            Tools.randomPause(80);
        }
    }

    static class CountingThread extends Thread {
        private Counter counter = new Counter();

        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                doSomething();
                counter.increment();
            }
            System.out.println(Thread.currentThread().getName() + " CountingThread:" + counter.value());
        }

        private void doSomething() {
            Tools.randomPause(80);
        }
    }
}
