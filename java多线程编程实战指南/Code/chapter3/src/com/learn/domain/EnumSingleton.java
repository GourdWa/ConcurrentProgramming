package com.learn.domain;

/**
 * @author ZixiangHu
 * @create 2020-05-18  22:04
 * @description
 */
public class EnumSingleton {
    public static enum Singleton{
        INSTANCE;

        //私有构造器
         Singleton(){

        }
        public void doService(){
            //doSome
        }
    }
}
