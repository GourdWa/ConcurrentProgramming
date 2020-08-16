import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-07-08-20:32
 */
public class RandomTest {
    public static void main(String[] args) {
        // Random实例
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            System.out.println(random.nextInt(5));
        }


    }
}
