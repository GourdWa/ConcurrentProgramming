package com.learn.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author ZixiangHu
 * @create 2020-06-06  22:15
 * @description
 */
public class ExecutorCompletionServiceDemo {
    public static void main(String[] args) {
//        AsyncTask a;
        ExecutorService executorService =  Executors.newFixedThreadPool(5);
        try {
            int taskCount = 10;
            List<Integer> list = new ArrayList<>();
            List<Future<Integer>> futureList = new ArrayList<>();
            ExecutorCompletionService<Integer> executorCompletionService = new ExecutorCompletionService<Integer>(executorService);
            for (int i = 0; i < taskCount; i++) {
                //向线程池中提交任务
                Future<Integer> res = executorCompletionService.submit(new Task(i));
                futureList.add(res);
            }
            for (int i = 0; i < taskCount; i++) {
                Integer result = executorCompletionService.take().get();
                System.out.println("任务i=="+result+"完成!"+new Date());
                list.add(result);
            }
            System.out.println("list:" + list);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    static class Task implements Callable<Integer> {
        Integer i;

        public Task(Integer i) {
            super();
            this.i = i;
        }

        @Override
        public Integer call() throws Exception {
            if (i == 5) {
                Thread.sleep(5000);
            } else {
                Thread.sleep(1000);
            }
            System.out.println("线程：" + Thread.currentThread().getName() + "任务i=" + i + ",执行完成！");
            return i;
        }

    }
}
