package com.utilities.circuitbreaker;

public class CircuitBreakerBuilder {

    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    public CircuitBreakerBuilder withName(String name){
        this.circuitBreaker.name = name;
        return this;
    }
    public CircuitBreakerBuilder setSlidingWindowSize(int slidingWindowSize){
        this.circuitBreaker.slidingWindowSize = slidingWindowSize;
        return this;
    }
    public CircuitBreakerBuilder setMinimumNumberOfCalls(int minimumNumberOfCalls){
        this.circuitBreaker.minimumNumberOfCalls = minimumNumberOfCalls;
        return this;
    }
    public CircuitBreakerBuilder setSlowCallDurationThreshold(long threshold){
        this.circuitBreaker.slowCallDurationThreshold = threshold;
        return this;
    }
    public CircuitBreakerBuilder setSlowCallRateThreshold(double threshold){
        this.circuitBreaker.slowCallRateThreshold = threshold;
        return this;
    }
    public CircuitBreakerBuilder setFailureRateThreshold(double threshold){
        this.circuitBreaker.failureRateThreshold = threshold;
        return this;
    }
    public CircuitBreakerBuilder setWaitDurationInOpenState(long duration){
        this.circuitBreaker.waitDurationInOpenState = duration;
        return this;
    }
    public CircuitBreakerBuilder setAllowedCallsInHalfOpenState(int allowedCalls){
        this.circuitBreaker.allowedCallsInHalfOpenState = allowedCalls;
        return this;
    }
    public CircuitBreakerBuilder addIgnoredExceptions(Class<? extends Exception> ignoredException){
        this.circuitBreaker.ignoredExceptions.add(ignoredException);
        return this;
    }
    public CircuitBreaker build(){
        this.circuitBreaker.initExecutorService();
        return this.circuitBreaker;
    }
}
