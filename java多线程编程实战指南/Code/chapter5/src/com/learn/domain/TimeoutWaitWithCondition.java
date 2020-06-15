package com.learn.domain;

import com.learn.utils.Debug;
import com.learn.utils.Tools;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ZixiangHu
 * @create 2020-05-25  21:49
 * @description
 */
public class TimeoutWaitWithCondition {
    private static final Lock LOCK = new ReentrantLock();
    private static final Condition CONDITION = LOCK.newCondition();
    private static boolean ready = false;
    protected static final Random RANDOM = new Random();

    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(() -> {
            for (; ; ) {
                LOCK.lock();
                try {
                    ready = RANDOM.nextInt(20) < 5 ? true : false;
                    if (ready)
                        CONDITION.signal();
                } finally {
                    LOCK.unlock();
                }
                Tools.randomPause(500);
            }
        });
        t.setDaemon(true);
        t.start();
        waiter(1000);
    }

    private static void waiter(final long timeOut) throws InterruptedException {
        if (timeOut < 0) {
            throw new IllegalArgumentException();
        }
        final Date deadline = new Date(System.currentTimeMillis() + timeOut);
        boolean continueToWait = true;
        LOCK.lock();
        try {
            while (!ready) {
                Debug.info("还没有准备好，continueToWait：%s", continueToWait);
                //等待未超时，继续等待
                if (!continueToWait) {
                    Debug.error("等待超时，不能执行目标代码");
                    return;
                }
                continueToWait = CONDITION.awaitUntil(deadline);
            }
            //执行目标动作
            guarededAction();
        } finally {
            LOCK.unlock();
        }
    }

    private static void guarededAction() {
        Debug.info("做一些事情");
    }
}
