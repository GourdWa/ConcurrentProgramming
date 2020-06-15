package com.learn.domain;

import com.learn.thread.WelcomeThread;

/**
 * @author ZixiangHu
 * @create 2020-05-08  11:22
 * @description 通过继承的方式实现多线程
 */
public class WelcomeApp {
    public static void main(String[] args) {
        Thread welcomeThread = new WelcomeThread();
        welcomeThread.start();

        System.out.println("WelcomeApp...main...");

    }
}
