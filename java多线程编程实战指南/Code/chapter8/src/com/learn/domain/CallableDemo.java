package com.learn.domain;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author ZixiangHu
 * @create 2020-06-03  22:41
 * @description
 */
public class CallableDemo {
    public static void main(String[] args) {
        Callable<Integer> callable = () -> {
            int sum = 0;
            for (int i = 0; i < 100; i++) {
                if (i % 2 == 0) {
                    sum += i;
                }
            }
            return sum;
        };
        //需要借助Future实现
        FutureTask<Integer> futureTask = new FutureTask<>(callable);
        //同样需要创建一个线程，并将FutureTask的实例传入
        Thread thread = new Thread(futureTask);
        thread.start();
        try {
            System.out.println(futureTask.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
