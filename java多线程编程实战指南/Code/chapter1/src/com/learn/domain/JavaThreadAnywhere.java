package com.learn.domain;

import com.learn.runnable.Helper;

/**
 * @author ZixiangHu
 * @create 2020-05-08  23:18
 * @description
 */
public class JavaThreadAnywhere {
    public static void main(String[] args) {
        Thread currentThread = Thread.currentThread();
        String currentThreadName = currentThread.getName();
        System.out.println("the main was executed by " + currentThreadName);
        Helper helper = new Helper("Java Thread Anywhere");
        helper.run();
    }
}
