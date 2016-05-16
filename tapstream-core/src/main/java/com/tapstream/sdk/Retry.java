package com.tapstream.sdk;


public class Retry {

    public static final Strategy DEFAULT_DATA_COLLECTION_STRATEGY = new Exponential(1000, 2, 10, 2 * 60 * 1000L);
    public static final Strategy DEFAULT_USER_FACING_RETRY_STRATEGY = new FixedDelay(500, 3, 5 * 1000L);
    public static final Strategy NEVER_RETRY = new Never();

    public interface Strategy {
        /**
         * Calculate the delay in ms for the next attempt.
         *
         * @param attempt   the current attempt number starting at 0.
         * @return          the delay in milliseconds for the next attempt.
         */
        int getDelayMs(int attempt);


        /**
         * Decide if another attempt can be made.
         *
         * @param attempt   the current attempt number starting at 0.
         * @param elapsedMs the number of milliseconds since the action started.
         * @return          true if another attempt at sending the request should be made.
         */

        boolean shouldRetry(int attempt, long elapsedMs);
    }

    static public class Never implements Strategy {

        @Override
        public int getDelayMs(int attempt) {
            return 0;
        }

        @Override
        public boolean shouldRetry(int attempt, long elaspedMs) {
            return false;
        }
    }

    static public class Exponential implements Strategy {

        private final int scale;
        private final int exponent;
        private final int maxTries;
        private final long maxElapsedMs;

        public Exponential(int scale, int exponent, int maxTries, long maxElapsedMs){
            this.scale = scale;
            this.exponent = exponent;
            this.maxTries = maxTries;
            this.maxElapsedMs = maxElapsedMs;
        }

        @Override
        public int getDelayMs(int attempt) {
            if (attempt == 1)
                return 0;

            double delay = scale * Math.pow(exponent, attempt - 2);
            delay = Math.min(delay, 60000);
            delay = Math.max(delay, 0);
            return (int)delay;
        }

        @Override
        public boolean shouldRetry(int attempt, long elapsedMs) {
            if (elapsedMs > maxElapsedMs)
                return false;
            return attempt < maxTries;
        }
    }


    static public class FixedDelay implements Strategy {
        private final int maxTries;
        private final int delay;
        private final long maxElapsedMs;

        public FixedDelay(int delay, int maxTries, long maxElapsedMs) {
            this.maxTries = maxTries;
            this.delay = delay;
            this.maxElapsedMs = maxElapsedMs;
        }

        @Override
        public int getDelayMs(int attempt) {
            return delay;
        }

        @Override
        public boolean shouldRetry(int attempt, long elapsedMs) {
            if (elapsedMs > maxElapsedMs)
                return false;
            return attempt < maxTries;
        }
    }

    static public class Retryable<T> {
        private final T obj;
        private final Retry.Strategy retryStrategy;

        private int attempt = 1;
        private final long firstSent = System.currentTimeMillis();

        public Retryable(T obj, Retry.Strategy retryStrategy) {
            this.obj = obj;
            this.retryStrategy = retryStrategy;
        }

        public T get(){
            return obj;
        }

        public int getAttempt(){
            return attempt;
        }

        public int incrementAttempt(){
            return ++attempt;
        }

        public long getFirstSent(){
            return firstSent;
        }

        public int getDelayMs(){
            return retryStrategy.getDelayMs(attempt);
        }

        public boolean shouldRetry(){
            long elapsedMs = System.currentTimeMillis() - firstSent;
            return retryStrategy.shouldRetry(attempt, elapsedMs);
        }
    }
}
