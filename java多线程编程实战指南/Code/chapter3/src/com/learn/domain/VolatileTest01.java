package com.learn.domain;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ZixiangHu
 * @create 2020-05-19  12:32
 * @description
 */
public class VolatileTest01 {
    private static volatile  int count = 0;
    public static void main(String[] args) {
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(){
                @Override
                public void run() {
                    for (int j = 0; j < 10; j++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        count++;
                        System.out.println(count);
                    }
                }
            };
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }


    }
}
