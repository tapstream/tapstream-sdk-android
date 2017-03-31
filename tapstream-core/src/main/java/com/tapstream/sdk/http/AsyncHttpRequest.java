package com.tapstream.sdk.http;

import com.tapstream.sdk.ApiResponse;
import com.tapstream.sdk.Logging;
import com.tapstream.sdk.Retry;
import com.tapstream.sdk.SettableApiFuture;
import com.tapstream.sdk.errors.ApiException;
import com.tapstream.sdk.errors.RecoverableApiException;
import com.tapstream.sdk.errors.RetriesExhaustedException;
import com.tapstream.sdk.errors.UnrecoverableApiException;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class AsyncHttpRequest<T extends ApiResponse> implements Runnable {

    public abstract static class Handler<T> {
        protected abstract T checkedRun(HttpResponse resp) throws IOException, ApiException;

        protected void onError(Throwable e){
            this.onFailure();
        }

        protected void onFailure(UnrecoverableApiException e, HttpResponse resp){
            Logging.log(Logging.ERROR, "Unrecoverable API exception. " +
                            "Check that your API secret and account name are correct (cause: %s)",
                    e.toString());
            this.onFailure();
        }
        protected void onFailure(){}
        protected void onRetry(){}
    }

    final HttpClient client;
    final SettableApiFuture<T> responseFuture;
    final Retry.Retryable<HttpRequest> retryable;
    final Handler<T> handler;
    final ScheduledExecutorService executor;

    public AsyncHttpRequest(SettableApiFuture<T> responseFuture, Retry.Retryable<HttpRequest> retryable, Handler<T> handler, ScheduledExecutorService executor, HttpClient client) {
        this.responseFuture = responseFuture;
        this.retryable = retryable;
        this.handler = handler;
        this.executor = executor;
        this.client = client;
    }

    private void fail(Throwable e){
        handler.onError(e);
        responseFuture.setException(e);
    }

    private void fail(UnrecoverableApiException e, HttpResponse resp){
        handler.onFailure(e, resp);
        responseFuture.setException(e);
    }

    private void retryRequest(Exception e){
        if (responseFuture.isCancelled()){
            Logging.log(Logging.INFO, "API request cancelled");
        } else if (retryable.shouldRetry()) {
            Logging.log(Logging.ERROR, "Failure during request, retrying (cause: %s)", e.toString());
            retryable.incrementAttempt();
            handler.onRetry();
            Future<?> requestFuture = executor.schedule(this, retryable.getDelayMs(), TimeUnit.MILLISECONDS);
            responseFuture.propagateCancellationTo(requestFuture);
        } else {
            Logging.log(Logging.ERROR, "No more retries, failing permanently (cause: %s).", e.toString());
            fail(new RetriesExhaustedException());
        }
    }

    final public void run() {
        try {
            HttpResponse response = client.sendRequest(retryable.get());

            // Send unrecoverable exceptions to the response-aware handler method
            try {
                response.throwOnError();
            }catch(UnrecoverableApiException e){
                fail(e, response);
                return;
            }

            responseFuture.set(handler.checkedRun(response));
        } catch (RecoverableApiException e) {
            retryRequest(e);
        } catch (IOException e) {
            retryRequest(e);
        } catch (UnrecoverableApiException e){
            fail(e);
        } catch (Exception e) {
            Logging.log(Logging.ERROR, "Unhandled exception during request (cause: %s)",
                    e.toString());
            fail(e);
        }
    }
}
