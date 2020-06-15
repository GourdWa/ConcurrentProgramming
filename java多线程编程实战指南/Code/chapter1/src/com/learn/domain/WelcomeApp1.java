package com.learn.domain;

import com.learn.runnable.WelcomeTask;

/**
 * @author ZixiangHu
 * @create 2020-05-08  11:28
 * @description 实现Runnable接口
 */
public class WelcomeApp1 {
    public static void main(String[] args) {
        Thread thread = new Thread(new WelcomeTask());
        thread.start();
        System.out.println("WelcomeApp1...main...");
    }
}
