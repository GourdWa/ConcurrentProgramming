package com.learn.domain;

import java.util.concurrent.*;

/**
 * @author ZixiangHu
 * @create 2020-06-03  22:27
 * @description
 */
public class ThreadPoolDemo {
    public static void main(String[] args) {
//        new ThreadPoolExecutor()
        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        Future<Integer> submit = service.submit(new NumThread());
        service.shutdown();

    }

    static class NumThread implements Callable<Integer> {

        @Override
        public Integer call(){
            int sum = 0;
            for (int i = 0; i < 100; i++) {
                if (i % 2 == 0) {
                    System.out.println(Thread.currentThread().getName() + "-" + i);
                    sum += i;
                }
            }
            return sum;
        }
    }

//    class
}
