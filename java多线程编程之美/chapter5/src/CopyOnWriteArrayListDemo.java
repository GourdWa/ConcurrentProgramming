import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-07-14-14:26
 */
public class CopyOnWriteArrayListDemo {
    public static void main(String[] args) {
        CopyOnWriteArrayList<Integer> writeArrayList = new CopyOnWriteArrayList<>();
        int[] arr = new int[]{1, 2, 3, 4, 5};
        int[] copyArr = Arrays.copyOf(arr, arr.length + 1);
        System.out.println("arr: " + Arrays.toString(arr));
        System.out.println("copyArr: " + Arrays.toString(copyArr));
        copyArr[0] = 12;
        System.out.println("arr: " + Arrays.toString(arr));
        System.out.println("copyArr: " + Arrays.toString(copyArr));
//        writeArrayList.iterator()
    }
}
