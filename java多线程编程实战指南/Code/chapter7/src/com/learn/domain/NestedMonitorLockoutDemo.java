package com.learn.domain;

import com.learn.utils.Tools;

import java.lang.reflect.Member;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author ZixiangHu
 * @create 2020-05-31  21:13
 * @description
 */
public class NestedMonitorLockoutDemo {
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(10);
    private int processed = 0;
    private int accepted = 0;

    public static void main(String[] args) throws InterruptedException {
        NestedMonitorLockoutDemo u = new NestedMonitorLockoutDemo();
        u.start();
        int i = 0;
        Tools.randomPause(100);
        while (i++ < 100000) {
            u.accept("message" + i);
            Tools.randomPause(100);
        }
    }

    protected synchronized void accept(String message) throws InterruptedException {
        queue.put(message);
        accepted++;
    }

    protected synchronized void doProcess() throws InterruptedException {
        String msg = queue.take();
        System.out.println("Process:" + msg);
        processed++;
    }


    private void start() {
        new WorkerThread().start();
    }

    class WorkerThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    doProcess();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
