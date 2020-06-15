package com.learn.domain;

import com.learn.utils.Tools;

/**
 * @author ZixiangHu
 * @create 2020-05-11  11:44
 * @description
 */
public class ThreadStartVisibility {
    static int data = 0;

    public static void main(String[] args) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                Tools.randomPause(50);
                System.out.println(data);
            }
        };
        data = 1;
        thread.start();
        Tools.randomPause(50);
        data = 2;//如果注释掉，那么输出一定是1；如果不注释，那么输出可能是1可能是2
    }
}
