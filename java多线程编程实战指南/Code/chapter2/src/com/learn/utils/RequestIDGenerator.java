package com.learn.utils;

import com.learn.inter.CircularSeqGenerator;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author ZixiangHu
 * @create 2020-05-10  17:31
 * @description
 */
public final class RequestIDGenerator implements CircularSeqGenerator {

    private final static RequestIDGenerator INSTANCE = new RequestIDGenerator();
    private final static short SEQ_UPPER_LIMIT = 999;
    private short sequence = -1;

    /**
     * 私有化构造器
     */
    private RequestIDGenerator() {

    }

    /**
     * 返回该例的唯一实例
     *
     * @return
     */
    public static RequestIDGenerator getInstance() {
        return INSTANCE;
    }


    /**
     * 生成循环序列号
     *
     * @return
     */
    @Override
    public synchronized short nextSequence() {
        if (sequence >= SEQ_UPPER_LIMIT)
            sequence = 0;
        else
            sequence ++;
        return sequence;
    }

    /**
     * 生成一个新的Request ID
     * @return
     */
    public String nextID() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        DecimalFormat df = new DecimalFormat("000");
        short sequence = nextSequence();
        return "0049" + timestamp + df.format(sequence);
    }
}
