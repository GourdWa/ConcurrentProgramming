package com.learn.bearn;

import com.learn.utils.Debug;

/**
 * @author ZixiangHu
 * @create 2020-05-31  16:29
 * @description
 */
public class DeadlockingPhilosopher extends AbstractPhilosopher {

    public DeadlockingPhilosopher(int id, Chopstick left, Chopstick right) {
        super(id, left, right);
    }

    @Override
    public void eat() {
        synchronized (left) {
            Debug.info("%s 正在拿起他左边的筷子 %s ...%n", this, right);
            left.pickUp();
            synchronized (right){
                Debug.info("%s 正在拿起他右边的筷子 %s ...%n", this, right);
                right.pickUp();
                doEat();
                right.putDown();
            }
            left.putDown();
        }
    }
}
