import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-08-07-17:05
 */
public class ScheduledThreadPoolExecutorDemo {
    public static void main(String[] args) {
        Runnable task = () -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("正在运行任务，当前时间是：" + new Date(System.currentTimeMillis()));
        };
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        //固定延时执行任务
//        scheduledExecutorService.scheduleWithFixedDelay(task, 5, 2, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleAtFixedRate(task, 5, 2, TimeUnit.SECONDS);
    }
}
