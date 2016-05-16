package com.tapstream.sdk;


import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class TestRetry {

    @Test
    public void testRetryableMaxElapsed() throws Exception {
        Retry.Strategy strategy = new Retry.Strategy(){

            @Override
            public int getDelayMs(int attempt) {
                return attempt * 5;
            }

            @Override
            public boolean shouldRetry(int attempt, long elapsedMs) {
                if (elapsedMs > 50)
                    return false;
                return true;
            }
        };

        Object inner = new Object();
        Retry.Retryable<Object> retryable = new Retry.Retryable<Object>(inner, strategy);
        assertThat(retryable.get(), is(inner));
        assertThat(retryable.shouldRetry(), is(true));
        Thread.sleep(100);
        assertThat(retryable.shouldRetry(), is(false));
    }

    @Test
    public void testRetryableAttempts() throws Exception{

        Retry.Strategy strategy = new Retry.Strategy(){

            @Override
            public int getDelayMs(int attempt) {
                return attempt * 5;
            }

            @Override
            public boolean shouldRetry(int attempt, long elapsedMs) {
                if (elapsedMs > 100)
                    return false;
                return attempt < 3;
            }
        };

        Object inner = new Object();
        Retry.Retryable<Object> retryable = new Retry.Retryable<Object>(inner, strategy);
        assertThat(retryable.get(), is(inner));

        // Initial attempt
        assertThat(retryable.shouldRetry(), is(true));
        assertThat(retryable.getAttempt(), is(1));
        assertThat(retryable.getDelayMs(), is(5));

        // Second attempt
        assertThat(retryable.incrementAttempt(), is(2));
        assertThat(retryable.getAttempt(), is(2));
        assertThat(retryable.shouldRetry(), is(true));
        assertThat(retryable.getDelayMs(), is(10));

        // third attempt should not be allowed
        assertThat(retryable.incrementAttempt(), is(3));
        assertThat(retryable.getAttempt(), is(3));
        assertThat(retryable.shouldRetry(), is(false));
        assertThat(retryable.getDelayMs(), is(15));

    }

    @Test
    public void testExponentialStrategy() throws Exception {
        Retry.Strategy strategy = new Retry.Exponential(1000, 2, 3, 1000);
        assertThat(strategy.getDelayMs(1), is(0));
        assertThat(strategy.getDelayMs(2), is(1000));
        assertThat(strategy.getDelayMs(3), is(2000));
        assertThat(strategy.shouldRetry(1, 0), is(true));
        assertThat(strategy.shouldRetry(2, 0), is(true));
        assertThat(strategy.shouldRetry(3, 0), is(false));
        assertThat(strategy.shouldRetry(1, 1001), is(false));
    }

    @Test
    public void testFixedDelayStrategy() throws Exception {
        Retry.Strategy strategy = new Retry.FixedDelay(1000, 3, 1000);
        assertThat(strategy.getDelayMs(1), is(1000));
        assertThat(strategy.getDelayMs(2), is(1000));
        assertThat(strategy.getDelayMs(3), is(1000));
        assertThat(strategy.shouldRetry(1, 0), is(true));
        assertThat(strategy.shouldRetry(2, 0), is(true));
        assertThat(strategy.shouldRetry(3, 0), is(false));
        assertThat(strategy.shouldRetry(1, 1001), is(false));
    }

}
