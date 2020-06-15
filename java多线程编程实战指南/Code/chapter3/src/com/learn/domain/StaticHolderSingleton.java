package com.learn.domain;

/**
 * @author ZixiangHu
 * @create 2020-05-18  21:55
 * @description
 */
public class StaticHolderSingleton {
    private StaticHolderSingleton() {

    }

    private static class InstanceHolder {
        private static final StaticHolderSingleton INSTANCE = new StaticHolderSingleton();
    }

    public static StaticHolderSingleton getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void doService() {
        //其他业务
    }
}
