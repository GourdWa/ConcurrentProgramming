import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-08-09-11:09
 */
public class CountDownLatchDemo {
    private static CountDownLatch countDownLatch = new CountDownLatch(2);

    public static void main(String[] args) throws InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        //提交线程A
        threadPool.submit(() -> {
            System.out.println("Thread A start");
            try {
                Thread.sleep(1000);
                System.out.println("Thread A over");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                countDownLatch.countDown();
            }
        });
        //提交线程B
        threadPool.submit(() -> {
            System.out.println("Thread B start");
            try {
                Thread.sleep(1000);
                System.out.println("Thread B over");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                countDownLatch.countDown();
            }
        });
        System.out.println("Main thread start");
        countDownLatch.await();
        System.out.println("Main thread over");
        threadPool.shutdown();
    }
}
