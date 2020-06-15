package com.learn.bean;

import com.learn.utils.Debug;
import com.learn.utils.Tools;

import java.util.Random;

/**
 * @author ZixiangHu
 * @create 2020-05-24  22:26
 * @description
 */
public class AlarmAgent {
    private final static AlarmAgent INSTANCE = new AlarmAgent();
    private boolean connectedToServer = false;
    private final HeartbearThread heartbearThread = new HeartbearThread();

    //私有化构造器
    private AlarmAgent() {

    }

    public static AlarmAgent getInstance() {
        return INSTANCE;
    }

    public void init() {
        connectToServer();
        heartbearThread.setDaemon(true);
        heartbearThread.start();
    }

    private void connectToServer() {
        new Thread() {
            @Override
            public void run() {
                doConnect();
            }
        }.start();
    }

    private void doConnect() {
        Tools.randomPause(100);
        synchronized (this) {
            connectedToServer = true;
            //连接建立完毕
            notify();
        }
    }

    public void sendAlarm(String message) throws InterruptedException {
        synchronized (this) {
            //先检查网络连接，如果连接不成功，则等待
            while (!connectedToServer) {
                Debug.info("Alarm agent was not connected to server.");
                wait();
            }
            doSendAlarm(message);
        }

    }

    private void doSendAlarm(String message) {
        Debug.info("Alarm sent:%s", message);
    }

    class HeartbearThread extends Thread {
        @Override
        public void run() {
            try {
                // 留一定的时间给网络连接线程与告警服务器建立连接
                Thread.sleep(1000);
                while (true) {
                    if (checkConnection()) {
                        connectedToServer = true;
                    } else {
                        connectedToServer = false;
                        Debug.info("Alarm agent was disconnected from server.");
                        // 检测到连接中断，重新建立连接
                        connectToServer();
                    }
                    Thread.sleep(2000);
                }

            } catch (InterruptedException e) {

            }
        }

        private boolean checkConnection() {
            boolean isConnect = true;
            final Random random = new Random();
            int rand = random.nextInt(1000);
            if (rand <= 500)
                isConnect = false;
            return isConnect;
        }
    }
}
