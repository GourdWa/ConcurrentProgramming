import java.util.concurrent.locks.LockSupport;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-07-17-14:35
 */
public class LockSupportTest {
    public static void main(String[] args) {
        System.out.println("begin park");
        LockSupport.park();
        System.out.println("end park");
    }
}
