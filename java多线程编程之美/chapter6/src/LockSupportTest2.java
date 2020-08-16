import java.util.concurrent.locks.LockSupport;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-07-17-14:45
 */
public class LockSupportTest2 {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("child thread begin unpark v1");
                LockSupport.park();
                System.out.println("child thread unpark v1");
            }
        });

        thread.start();
        Thread.sleep(1000);
        System.out.println("main thread begin unpark");
        LockSupport.unpark(thread);


    }
}
