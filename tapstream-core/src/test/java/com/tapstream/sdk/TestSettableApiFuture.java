package com.tapstream.sdk;

import org.junit.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;


public class TestSettableApiFuture {

    @Test
    public void testCancel() throws Exception {
        SettableApiFuture<Object> future = new SettableApiFuture<Object>();
        future.cancel(true);
        assertThat(future.isCancelled(), is(true));
        assertThat(future.isDone(), is(true));
        assertThat(future.isError(), is(false));

        try{
            future.get();
            fail("Should have thrown");
        } catch (CancellationException e){
            // Expected
        }
    }

    @Test
    public void testPropagateCancellationTo() throws Exception {
        SettableApiFuture<Object> future = new SettableApiFuture<Object>();
        SettableApiFuture<Object> someOtherFuture = new SettableApiFuture<Object>();
        future.propagateCancellationTo(someOtherFuture);

        assertThat(future.isCancelled(), is(false));
        assertThat(someOtherFuture.isCancelled(), is(false));
        future.cancel(true);
        assertThat(future.isCancelled(), is(true));
        assertThat(someOtherFuture.isCancelled(), is(true));
    }

    @Test
    public void testSetException() throws Exception {
        Exception e = new Exception("Test Exception");
        SettableApiFuture<Object> future = new SettableApiFuture<Object>();
        assertThat(future.isDone(), is(false));
        assertThat(future.isCancelled(), is(false));
        assertThat(future.isError(), is(false));
        future.setException(e);
        assertThat(future.isDone(), is(true));
        assertThat(future.isCancelled(), is(false));
        assertThat(future.isError(), is(true));

        try {
            future.get();
            fail("Should have thrown an exception");
        } catch (ExecutionException executionException){
            assertThat(executionException.getCause(), sameInstance((Throwable)e));
        }
    }

    @Test
    public void testSet() throws Exception {
        Object expected = new Object();
        SettableApiFuture<Object> future = new SettableApiFuture<Object>();
        assertThat(future.isDone(), is(false));
        assertThat(future.isCancelled(), is(false));
        assertThat(future.isError(), is(false));
        future.set(expected);
        assertThat(future.isDone(), is(true));
        assertThat(future.isCancelled(), is(false));
        assertThat(future.isError(), is(false));
        Object actual = future.get();
        assertThat(actual, sameInstance(expected));
    }

    @Test
    public void testCallbackSuccessDelayed() throws Exception {
        final AtomicReference<Object> actual = new AtomicReference();
        final Object expected = new Object();

        SettableApiFuture<Object> future = new SettableApiFuture<Object>();
        future.setCallback(new Callback<Object>() {

            @Override
            public void success(Object result) {
                actual.set(result);
            }

            @Override
            public void error(Throwable reason) {
                fail("Should not be called");
            }
        });

        future.set(expected);
        assertThat(actual.get(), sameInstance(expected));
    }

    @Test
    public void testCallbackSuccessImmediate() throws Exception {
        final AtomicReference<Object> actual = new AtomicReference<Object>();
        final Object expected = new Object();

        SettableApiFuture<Object> future = new SettableApiFuture<Object>();
        future.set(expected);
        future.setCallback(new Callback<Object>() {

            @Override
            public void success(Object result) {
                actual.set(result);
            }

            @Override
            public void error(Throwable reason) {
                fail("Should not be called");
            }
        });

        assertThat(actual.get(), sameInstance(expected));
    }

    @Test
    public void testCallbackErrorDelayed() throws Exception {
        final AtomicReference<Throwable> actual = new AtomicReference<Throwable>();
        final Throwable expected = new Exception("Test Exception");

        SettableApiFuture<Object> future = new SettableApiFuture<Object>();
        future.setCallback(new Callback<Object>() {

            @Override
            public void success(Object result) {
                fail("Should not be called");
            }

            @Override
            public void error(Throwable reason) {
                actual.set(reason);
            }
        });

        future.setException(expected);
        assertThat(actual.get(), sameInstance(expected));
    }


    @Test
    public void testGetWithTimeout() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);
        final Object expected = new Object();
        final SettableApiFuture<Object> future = new SettableApiFuture<Object>();
        final AtomicReference<Object> actual = new AtomicReference<Object>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    actual.set(future.get(100, TimeUnit.MILLISECONDS));
                } catch (Exception e){
                    fail("Should not have thrown");
                } finally {
                    lock.countDown();
                }

            }
        }).start();

        future.set(expected);
        lock.await();
        assertThat(actual.get(), sameInstance(expected));

    }

    @Test(expected = TimeoutException.class, timeout=1000)
    public void testGetWithTimeoutException() throws Exception {
        final SettableApiFuture<Object> future = new SettableApiFuture<Object>();
        future.get(50, TimeUnit.MILLISECONDS);
    }
}
