package dom.learn.domain;

/**
 * @author ZixiangHu
 * @create 2020-05-23  22:07
 * @description
 */
public class GetCpuNum {
    public static void main(String[] args) {
        int nCpu = Runtime.getRuntime().availableProcessors();
        System.out.println(nCpu);
    }
}
