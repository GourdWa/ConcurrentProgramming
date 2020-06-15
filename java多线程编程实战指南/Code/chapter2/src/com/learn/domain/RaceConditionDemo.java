package com.learn.domain;

import com.learn.utils.RequestIDGenerator;
import com.learn.utils.Tools;

import java.text.DecimalFormat;

/**
 * @author ZixiangHu
 * @create 2020-05-10  17:40
 * @description
 */
public class RaceConditionDemo {
    public static void main(String[] args) {
        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        Thread[] workerThreads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            //每个线程10个请求
            workerThreads[i] = new WorkerThread(i, 10);
        }
        for (Thread ct : workerThreads) {
            ct.start();
        }
    }

    static class WorkerThread extends Thread {
        private final int requestCount;

        public WorkerThread(int id, int requestCount) {
            super("worker-" + id);
            this.requestCount = requestCount;
        }

        @Override
        public void run() {
            int i = requestCount;
            String requestID;
            RequestIDGenerator idGenerator = RequestIDGenerator.getInstance();
            while (i-- > 0) {
                requestID = idGenerator.nextID();
                processRequest(requestID);
            }
        }

        private void processRequest(String requestID) {
            Tools.randomPause(50);
            System.out.println(Thread.currentThread().getName() + " got requestID: " + requestID);
        }
    }
}
