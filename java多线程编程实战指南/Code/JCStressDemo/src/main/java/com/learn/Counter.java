package com.learn;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-06-13-21:42
 */
public class Counter {
    private volatile long count;

    public long value(){
        return count;
    }

    public void increment(){
        count++;
    }
}
