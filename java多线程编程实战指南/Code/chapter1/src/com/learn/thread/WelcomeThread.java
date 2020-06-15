package com.learn.thread;

/**
 * @author ZixiangHu
 * @create 2020-05-08  11:21
 * @description
 */
public class WelcomeThread extends Thread {
    @Override
    public void run() {
        System.out.println("WelcomeThread...run...");
    }
}
