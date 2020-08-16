import java.util.concurrent.locks.LockSupport;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-07-17-18:50
 */
public class LockSupportTest3 {
    public void testPark() {
        LockSupport.park(this);
        LockSupport.parkUntil(this,100);
    }

    public static void main(String[] args) {
        LockSupportTest3 supportTest3 = new LockSupportTest3();
        supportTest3.testPark();
    }
}
