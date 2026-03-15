package com.utilities.circuitbreaker;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreaker {

    private static final int CORE_THREAD_COUNT = 4;
    private static final int MAX_THREAD_COUNT = 32767;
    private static final long WORKER_KEEP_ALIVE_SEC = 15L;
    private static final int SHUTDOWN_TIME_SEC = 3;
    String name = "default";
    int slidingWindowSize = 10;
    int minimumNumberOfCalls = 10;
    long slowCallDurationThreshold = 5000L;
    double slowCallRateThreshold = 1.0D;
    double failureRateThreshold = 0.5D;
    long waitDurationInOpenState = 60000L;
    int allowedCallsInHalfOpenState = 5;

    ArrayList<Class<? extends Exception>> ignoredExceptions = new ArrayList<>();

    private ThreadPoolExecutor executorService = null;
    private SlidingWindowStats successSlidingStats;
    private SlidingWindowStats slownessSlidingStats;
    private AtomicInteger callsInHalfOpen = new AtomicInteger(0);
    private CircuitStatus circuitStatus = CircuitStatus.CLOSED;
    private volatile long backOffUpto = 0L;

    CircuitBreaker(){
        this.successSlidingStats = new SlidingWindowStats(this.slidingWindowSize);
        this.slownessSlidingStats = new SlidingWindowStats(this.slidingWindowSize);
    }

    void initExecutorService(){
        this.executorService = new ThreadPoolExecutor(CORE_THREAD_COUNT, MAX_THREAD_COUNT, WORKER_KEEP_ALIVE_SEC, TimeUnit.SECONDS, new SynchronousQueue<>());
        this.executorService.allowCoreThreadTimeOut(true);
    }

    public void changeSlowCallDurationThreshold(long newValue){
        this.slowCallDurationThreshold = newValue;
    }

    public void shutdown(){
        this.executorService.shutdown();
        try{
            this.executorService.awaitTermination(SHUTDOWN_TIME_SEC, TimeUnit.SECONDS);
        }catch (InterruptedException e){

        }
        if(!this.executorService.isTerminated()){
            this.executorService.shutdownNow();
        }
    }

    public void performTask(CircuitBreakerTask task) throws RejectedExecutionException, InterruptedException,
            TimeoutException{
        if(this.circuitStatus == CircuitStatus.OPEN){
            if(System.currentTimeMillis() < this.backOffUpto) {
                throw new RejectedExecutionException();
            }
            setCircuitStatus(CircuitStatus.HALF_OPEN);
        } else if (this.circuitStatus == CircuitStatus.HALF_OPEN) {
            if(this.callsInHalfOpen.get() > this.allowedCallsInHalfOpenState){
                setCircuitStatus(CircuitStatus.OPEN);
                throw new RejectedExecutionException();
            }
        }
        CountDownLatch latch = task.getLatch();
        try{
            this.executorService.submit(task);
            boolean success = latch.await(this.slowCallDurationThreshold, TimeUnit.MILLISECONDS);
            if(success){
                processTaskFinish(task);
            }else{
                processTaskTimeout(task);
                tryRemoveTask(task);
                throw new TimeoutException();
            }
        }catch (RejectedExecutionException e){
            processTaskTimeout(task);
            throw e;
        }catch (InterruptedException e){
            processTaskTimeout(task);
            tryRemoveTask(task);
            throw e;
        }
    }

    private void processTaskFinish(CircuitBreakerTask task){
        if(this.circuitStatus == CircuitStatus.HALF_OPEN){
            this.callsInHalfOpen.incrementAndGet();
        }
        this.slownessSlidingStats.addValue(1L);
        if(task.hasException()){
            boolean ignoredExceptionFound = false;
            Class exceptionClass;
            if(!this.ignoredExceptions.isEmpty()){
                exceptionClass = task.getException().getClass();
                for(Class ignoredExClass: this.ignoredExceptions){
                    if(ignoredExClass.isAssignableFrom(exceptionClass)){
                        ignoredExceptionFound = true;
                        break;
                    }
                }
            }
            if(ignoredExceptionFound){
                this.successSlidingStats.addValue(1L);
                processSuccess();
            }else{
                this.successSlidingStats.addValue(0L);
                processFailure();
            }
        }else{
            this.successSlidingStats.addValue(1L);
            processSuccess();
        }
    }
    private void tryRemoveTask(CircuitBreakerTask task){
        boolean removeSuccess = this.executorService.remove(task);
        if(!removeSuccess){
            Thread workerThread = task.getWorkerThread();
            if(workerThread != null){
                workerThread.interrupt();
            }
        }
    }

    private void processTaskTimeout(CircuitBreakerTask task){
        if(this.circuitStatus == CircuitStatus.HALF_OPEN){
            this.callsInHalfOpen.incrementAndGet();
        }
        this.slownessSlidingStats.addValue(0);
        this.successSlidingStats.addValue(0);
        processTimeout();
        task.onTimeOut();
    }

    private void processSuccess(){
        if(this.circuitStatus == CircuitStatus.HALF_OPEN){
            double successRate = this.successSlidingStats.getSlidingAvg();
            double failureRate = 1.0D - successRate;
            if((this.successSlidingStats.size() >= this.minimumNumberOfCalls) && (failureRate < this.failureRateThreshold)){
                setCircuitStatus(CircuitStatus.CLOSED);
            }
        }
    }
    private void processFailure(){
        if(this.circuitStatus == CircuitStatus.CLOSED){
            double successRate = this.successSlidingStats.getSlidingAvg();
            double failureRate = 1.0D - successRate;
            if((this.successSlidingStats.size() >= this.minimumNumberOfCalls) && (failureRate >= this.failureRateThreshold)){
                setCircuitStatus(CircuitStatus.OPEN);
            }
        }
    }
    private void processTimeout(){
        if(this.circuitStatus == CircuitStatus.CLOSED){
            double successRate = this.slownessSlidingStats.getSlidingAvg();
            double slowRate = 1.0D - successRate;
            if((this.successSlidingStats.size() >= this.minimumNumberOfCalls) && (slowRate >= this.slowCallRateThreshold)){
                setCircuitStatus(CircuitStatus.OPEN);
            }
        }
    }

    private void setCircuitStatus(CircuitStatus status){
        System.out.println("Setting circuit status from " +this.circuitStatus+ " to "+ status);
        this.circuitStatus = status;
        if(status == CircuitStatus.OPEN){
            this.backOffUpto = System.currentTimeMillis() + this.waitDurationInOpenState;
        }
        this.callsInHalfOpen.set(0);
    }
    private static enum CircuitStatus{
        CLOSED, HALF_OPEN, OPEN
    }
}
