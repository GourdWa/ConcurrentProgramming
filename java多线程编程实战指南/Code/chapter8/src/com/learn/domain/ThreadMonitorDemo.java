package com.learn.domain;

import com.learn.utils.Debug;
import com.learn.utils.Tools;
import javafx.geometry.VPos;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ZixiangHu
 * @create 2020-06-02  22:59
 * @description
 */
public class ThreadMonitorDemo {
    volatile boolean inited = false;
    static int threadIndex = 0;
    final static Logger LOGGER = Logger.getAnonymousLogger();
    final BlockingQueue<String> channel = new ArrayBlockingQueue<String>(100);

    public static void main(String[] args) throws InterruptedException {
        ThreadMonitorDemo demo = new ThreadMonitorDemo();
        demo.init();
        for (int i = 0; i < 100; i++) {
            demo.service("test-" + i);
        }
        Thread.sleep(5000);
        System.exit(0);
    }

    public synchronized void init() {
        if (inited)
            return;
        Debug.info("init...");
        WokrerThread t = new WokrerThread();
        t.setName("Worker0-" + threadIndex++);
        t.setUncaughtExceptionHandler(new ThreadMonitor());
        t.start();
        inited = true;

    }

    public void service(String message) throws InterruptedException {
        channel.put(message);
    }

    private class ThreadMonitor implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Debug.info("Current thread is `t`:%s, it is still alive:%s",
                    Thread.currentThread() == t, t.isAlive());

            // 将线程异常终止的相关信息记录到日志中
            String threadInfo = t.getName();
            LOGGER.log(Level.SEVERE, threadInfo + " terminated:", e);
            // 创建并启动替代线程
            LOGGER.info("About to restart " + threadInfo);
            // 重置线程启动标记
            inited = false;
            init();
        }
    }

    private class WokrerThread extends Thread {
        @Override
        public void run() {
            Debug.info("Do something important...");
            String msg;
            try {
                for (; ; ) {
                    msg = channel.take();
                    process(msg);
                }
            } catch (InterruptedException e) {
                // 什么也不做
            }
        }

        private void process(String message) {
            Debug.info(message);
            // 模拟随机性异常
            int i = (int) (Math.random() * 100);
            if (i < 2) {
                throw new RuntimeException("test");
            }
            Tools.randomPause(100);
        }
    }// 类ThreadMonitorDemo定义结束
}
