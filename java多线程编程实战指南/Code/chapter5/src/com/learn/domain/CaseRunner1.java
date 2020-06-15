package com.learn.domain;

import com.learn.bean.AlarmAgent;
import com.learn.utils.Tools;

/**
 * @author ZixiangHu
 * @create 2020-05-24  22:48
 * @description
 */
public class CaseRunner1 {
    final static AlarmAgent alarmAgent;
    static {
        alarmAgent = AlarmAgent.getInstance();
        alarmAgent.init();
    }

    public static void main(String[] args) throws InterruptedException {

        alarmAgent.sendAlarm("Database offline!");
        Tools.randomPause(12000);
        alarmAgent.sendAlarm("XXX service unreachable!");


    }
}
