package com.learn.domain;

/**
 * @author ZixiangHu
 * @create 2020-05-18  21:21
 * @description
 */
public class SimpleMultiThreadedSingleton {
    private static SimpleMultiThreadedSingleton instance = null;

    /**
     * 私有化构造器
     */
    private SimpleMultiThreadedSingleton() {

    }

    public static SimpleMultiThreadedSingleton getInstance() {
        synchronized (SimpleMultiThreadedSingleton.class) {
            if (instance == null) {
                instance = new SimpleMultiThreadedSingleton();
            }
        }
        return instance;
    }

    public void someService() {
        //其他服务
    }
}
