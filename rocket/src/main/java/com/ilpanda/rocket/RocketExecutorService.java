package com.ilpanda.rocket;

import android.os.Process;
import android.support.annotation.NonNull;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RocketExecutorService extends ThreadPoolExecutor {

    private static final int DEFAULT_THREAD_COUNT = 3;

    RocketExecutorService() {
        super(DEFAULT_THREAD_COUNT, DEFAULT_THREAD_COUNT, 0, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<Runnable>(), new RocketThreadFactory());
    }


    @Override
    public Future<?> submit(Runnable task) {
        RocketFutureTask rocketFutureTask = new RocketFutureTask((RocketResponse) task);
        execute(rocketFutureTask);
        return rocketFutureTask;
    }

    private static class RocketThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new RocketThread(r);
        }
    }

    private static class RocketThread extends Thread {
        public RocketThread(Runnable r) {
            super(r);
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            super.run();
        }
    }

    private static class RocketFutureTask extends FutureTask<RocketResponse> implements Comparable<RocketFutureTask> {

        private RocketResponse response;

        public RocketFutureTask(@NonNull RocketResponse response) {
            super(response, null);
            this.response = response;
        }

        @Override
        public int compareTo(@NonNull RocketFutureTask other) {
            RocketRequest.Priority p1 = response.getPriority();
            RocketRequest.Priority p2 = other.response.getPriority();
            // High-priority requests are "lesser" so they are sorted to the front.
            // Equal priorities are sorted by sequence number to provide FIFO ordering.

            return p1 == p2 ? response.getSequence() - other.response.getSequence() : p1.ordinal() - p2.ordinal();
        }
    }

}
