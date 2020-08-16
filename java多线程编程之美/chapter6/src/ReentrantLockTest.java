import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-07-22-20:40
 */
public class ReentrantLockTest {
    public static void main(String[] args) {
        System.out.println(1 >>> 16);
        ReentrantLock lock = new ReentrantLock();
        ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    }
}
