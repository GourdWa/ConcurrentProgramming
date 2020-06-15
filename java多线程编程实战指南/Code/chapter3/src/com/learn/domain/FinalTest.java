package com.learn.domain;

/**
 * @author ZixiangHu
 * @create 2020-05-19  15:07
 * @description
 */
public class FinalTest {
    final int x;
    int y;
    static FinalTest instance;

    public FinalTest() {
        this.x = 1;
        this.y = 2;
    }

    public static void writer() {
        instance = new FinalTest();
    }

    public static void reader() {
        final FinalTest theInstance = instance;
        if (theInstance != null) {
            System.out.println(theInstance.y - theInstance.x);
        }
    }

    public static void main(String[] args) {
        writer();
        reader();
    }
}
