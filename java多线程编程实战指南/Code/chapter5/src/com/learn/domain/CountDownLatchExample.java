package com.learn.domain;

import com.learn.utils.Debug;
import com.learn.utils.Tools;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * @author ZixiangHu
 * @create 2020-05-25  22:58
 * @description
 */
public class CountDownLatchExample {
    private static final CountDownLatch LATCH = new CountDownLatch(4);
    private static int data;

    public static void main(String[] args) throws InterruptedException {
        Thread workerThread = new Thread() {
            @Override
            public void run() {
                for (int i = 1; i < 10; i++) {
                    data = i;
                    LATCH.countDown();
                    //使当前线程随机一段时间
                    System.out.println(i);
                    Tools.randomPause(1000);
                }
            }
        };
        workerThread.start();
        LATCH.await();
        Debug.info("It's done.data=%d", data);
    }
}
