package com.learn.domain;

import com.learn.utils.Debug;

/**
 * @author ZixiangHu
 * @create 2020-05-19  13:45
 * @description
 */
public class StaticTest {

    public static void main(String[] args) {
        Debug.info(Collaborator.class.hashCode());
        Debug.info(Collaborator.number);
        Debug.info(Collaborator.flag);
    }
    static class Collaborator{
        static int number= 1;
        static boolean flag = false;
        static {
            Debug.info("Collaborator initialinzing...");
        }
    }
}
