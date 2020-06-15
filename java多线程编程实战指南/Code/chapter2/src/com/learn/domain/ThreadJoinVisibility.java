package com.learn.domain;

import com.learn.utils.Tools;

/**
 * @author ZixiangHu
 * @create 2020-05-11  11:52
 * @description
 */
public class ThreadJoinVisibility {
    static int data = 0;

    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            Tools.randomPause(50);
            data = 1;
        });
        thread.start();
        //等待thread结束后，main线程才会继续运行
        //如果屏蔽掉join方法，那么输出可能是0，也可能是1
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(data);
    }
}
