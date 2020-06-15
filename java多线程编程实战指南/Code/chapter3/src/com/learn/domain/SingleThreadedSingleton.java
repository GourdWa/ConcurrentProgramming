package com.learn.domain;

/**
 * @author ZixiangHu
 * @create 2020-05-18  21:03
 * @description
 */
public class SingleThreadedSingleton {
    private static SingleThreadedSingleton instance = null;

    /**
     * 私有化构造器
     */
    private SingleThreadedSingleton() {

    }

    public static SingleThreadedSingleton getInstance() {
        if (instance == null) {
            instance = new SingleThreadedSingleton();
        }
        return instance;
    }

    public void someService() {
        //其他服务
    }
}
