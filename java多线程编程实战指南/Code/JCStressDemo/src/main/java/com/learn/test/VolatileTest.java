package com.learn.test;

import org.openjdk.jcstress.annotations.*;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-06-14-14:34
 */
public class VolatileTest {
    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = Expect.ACCEPTABLE)
    @Outcome(id = "STALE", expect = Expect.ACCEPTABLE_INTERESTING)
    @State
    public static class NoVolatile {
        private int i = 0;

        @Actor
        public void actor() {
            while (i == 0) {
                //nothing
            }
        }
        @Signal
        public void single() {
            i = 1;
        }
    }
    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = Expect.ACCEPTABLE)
    @Outcome(id = "STALE", expect = Expect.FORBIDDEN)
    @State
    public static class AddVolatile {
        private volatile int i = 0;
        @Actor
        public void actor() {
            while (i == 0) {
                //nothing
            }
        }
        @Signal
        public void single() {
            i = 1;
        }
    }
}
