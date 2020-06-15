package com.learn.runnable;

/**
 * @author ZixiangHu
 * @create 2020-05-08  23:19
 * @description
 */
public class Helper implements Runnable {
    private final String message;

    public Helper(String message) {
        this.message = message;
    }

    @Override
    public void run() {
        doSomething(message);
    }

    private void doSomething(String message) {
        Thread currentThread = Thread.currentThread();
        String currentThreadName = currentThread.getName();
        System.out.println("doSomething was executed by:" + currentThreadName);
        System.out.println("doSomething with " + message);
    }
}
