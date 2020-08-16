import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-07-09-17:09
 */
public class RandomTest2 {
//    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    static class MyThread extends Thread {
        private String name;

        public MyThread(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            System.out.println(name + ": " + ThreadLocalRandom.current().nextInt(10));
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            new MyThread("线程" + i).start();
        }

    }
}
