package com.learn.domain;

import com.learn.utils.Debug;
import com.learn.utils.Tools;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * @author ZixiangHu
 * @create 2020-05-26  8:55
 * @description
 */
public class ShootPractice {
    // 参与打靶训练的士兵
    static class Soldier {
        private final int seqNo;

        public Soldier(int seqNo) {
            this.seqNo = seqNo;
        }

        public void fire() {
            Debug.info(this + " start firing...");
            Tools.randomPause(5000);
            System.out.println(this + " fired.");
        }

        @Override
        public String toString() {
            return "Soldier-" + seqNo;
        }

    }// 类Soldier定义结束

    //参与训练的全部士兵
    final Soldier[][] rank;
    //每排中士兵的个数
    final int N;
    //打靶持续时间
    final int lasting;
    //标识是否继续打靶
    volatile boolean done = false;
    //用来指示进行下一轮打靶的是那一排士兵
    volatile int nextLine = 0;

    final CyclicBarrier shiftBarrier;
    final CyclicBarrier startBarrier;

    public ShootPractice(int N, final int lineCount, int lasting) {
        this.N = N;
        this.lasting = lasting;
        this.rank = new Soldier[lineCount][N];
        for (int i = 0; i < lineCount; i++) {
            for (int j = 0; j < N; j++) {
                rank[i][j] = new Soldier(i * N + j);
            }
        }
        shiftBarrier = new CyclicBarrier(N, () -> {
            nextLine = (nextLine + 1) % lineCount;
            Debug.info("下一轮是：%s", nextLine);
        });
        startBarrier = new CyclicBarrier(N);
    }

    public static void main(String[] args) throws InterruptedException {
        ShootPractice sp = new ShootPractice(4, 5, 24);
        sp.start();
    }

    public void start() throws InterruptedException {
        Thread[] threads = new Thread[N];
        for (int i = 0; i < N; i++) {
            threads[i] = new Shooting(i);
            threads[i].start();
        }
        Thread.sleep(lasting * 1000);
        stop();
        for (Thread t : threads) {
            t.join();
        }
        Debug.info("练习结束");
    }

    public void stop() {
        done = true;
    }

    class Shooting extends Thread {
        final int index;

        public Shooting(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            Soldier soldier;
            try {
                while (!done) {
                    soldier = rank[nextLine][index];
                    startBarrier.await();
                    soldier.fire();
                    shiftBarrier.await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
}


