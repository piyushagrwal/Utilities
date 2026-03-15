package com.utilities.circuitbreaker;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;

public class HistoryLogger {
    private static final long PRINT_PERIOD_MILLI = 180000L;
    private static final long[] TIME_BUCKETS = {2L,4L,6L,8L,10L,25L,50L,100L,250L,500L,1000L,2500L,5000L,10000L,20000L,30000L,40000L,50000L,60000L,60001L};
    public String name;
    private AtomicLong histPrintTime = new AtomicLong(System.currentTimeMillis() + PRINT_PERIOD_MILLI);
    private AtomicIntegerArray histCounts = new AtomicIntegerArray(TIME_BUCKETS.length);

    public HistoryLogger(String name){
        this.name = name;
    }

    public void logTimeTaken(long time){
        int arrLen = TIME_BUCKETS.length;
        int index = 0;
        while ((TIME_BUCKETS[index] < time) && (index < arrLen)){
            index++;
        }
//        if(index >= arrLen){
//            index = arrLen - 1;
//        }
        this.histCounts.incrementAndGet(index);
        long currentPrintTime = this.histPrintTime.get();
        if(System.currentTimeMillis() >= currentPrintTime){
            long newPrintTime = System.currentTimeMillis() + PRINT_PERIOD_MILLI;
            if(this.histPrintTime.compareAndSet(currentPrintTime, newPrintTime)){
                int[] readings = new int[arrLen];
                for (int i = 0; i < arrLen; i++) {
                    readings[i] = this.histCounts.getAndSet(i,0);
                }
                int totalCount = 0;
                StringBuilder sb = new StringBuilder();
                sb.append("History - { name :").append(this.name);
                for (int i = 0; i < arrLen; i++) {
                    int reading = readings[i];
                    sb.append(", c_").append(TIME_BUCKETS[i]).append("_ms: ").append(reading);
                    totalCount += reading;
                }
                sb.append(", total count: ").append(totalCount);
                int p90Count = totalCount * 90 / 100;
                int p95Count = totalCount * 95 / 100;
                int partCount = 0;
                int p90Index = 0;
                int p95Index = 0;
                int p100Index = 0;
                for (int i = 0; i < arrLen; i++) {
                    partCount += readings[i];
                    if((partCount <= p90Count) && (readings[i] > 0)){
                        p90Index = i;
                    }
                    if((partCount <= p95Count) && (readings[i] > 0)){
                        p95Index = i;
                    }
                    if((partCount <= totalCount) && (readings[i] > 0)){
                        p100Index = i;
                    }
                }
                sb.append(", p90: ").append(TIME_BUCKETS[p90Index]);
                sb.append(", p95: ").append(TIME_BUCKETS[p95Index]);
                sb.append(", p100: ").append(TIME_BUCKETS[p100Index]);
                sb.append("}");
                // Log here using the log framework
                System.out.println(this);
                System.out.println(sb.toString());
            }
        }
    }
}
