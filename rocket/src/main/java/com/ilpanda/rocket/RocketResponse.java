package com.ilpanda.rocket;

import android.net.NetworkInfo;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class RocketResponse implements Runnable {

    private static final AtomicInteger SEQUENCE_GENERATOR = new AtomicInteger();

    Rocket rocket;

    Exception exception;

    private List<RocketRequest> requests;

    private RocketRequest request;

    private RocketInterceptor rocketInterceptor;

    public Future<?> future;

    private RocketDispatcher dispatcher;

    private File result;

    private int retryCount;

    private String url;

    private RocketRequest.Priority priority;

    private int sequence;


    private static RocketInterceptor ERROR_INTERCEPTOR = new RocketInterceptor() {
        @Override
        public boolean canInterceptor(RocketRequest request) {
            return false;
        }

        @Override
        public File interceptor(RocketRequest request) throws IOException {
            throw new IllegalStateException("Unrecognized type of request: " + request);
        }
    };


    public RocketResponse(Rocket rocket, RocketDispatcher dispatcher, RocketRequest request, RocketInterceptor rocketInterceptor) {
        this.rocket = rocket;
        this.dispatcher = dispatcher;
        this.request = request;
        this.rocketInterceptor = rocketInterceptor;
        this.retryCount = request.getRetryCount();
        this.url = request.getUrl();
        this.priority = request.getPriority();
        this.sequence = SEQUENCE_GENERATOR.incrementAndGet();
    }

    void attach(RocketRequest request) {
        if (requests == null) {
            requests = new ArrayList<>();
        }
        requests.add(request);
    }

    void detach(RocketRequest request) {
        if (this.request == request) {
            this.request = null;
        } else if (this.requests != null) {
            this.requests.remove(request);
        }
    }

    static RocketResponse forRequest(Rocket rocket, RocketDispatcher dispatcher, RocketRequest request) {
        List<RocketInterceptor> interceptorList = rocket.getInterceptorList();
        for (int i = 0; i < interceptorList.size(); i++) {
            RocketInterceptor rocketInterceptor = interceptorList.get(i);
            if (rocketInterceptor.canInterceptor(request)) {
                return new RocketResponse(rocket, dispatcher, request, rocketInterceptor);
            }
        }
        return new RocketResponse(rocket, dispatcher, request, ERROR_INTERCEPTOR);
    }

    @Override
    public void run() {
        try {
            result = rocketInterceptor.interceptor(request);
            if (request.needTransform()) {
                List<RocketTransformation> transformations = request.getTransformations();
                for (int i = 0; i < transformations.size(); i++) {
                    RocketTransformation rocketTransformation = transformations.get(i);
                    result = rocketTransformation.transform(request, result);
                }
            }
            if (result != null && result.exists()) {
                this.exception = null;
                dispatcher.dispatchComplete(this);
            } else {
                dispatcher.dispatchError(this);
            }
        } catch (PrepareException | TransformException e) {
            exception = e;
            dispatcher.dispatchError(this);
        } catch (InterruptedIOException e) {
            exception = e;
            dispatcher.dispatchCancelSuccess(this.url);
        } catch (IOException e) {
            exception = e;
            dispatcher.dispatchRetry(this);
        } catch (Exception e) {
            exception = e;
            dispatcher.dispatchError(this);
        }
    }

    boolean shouldRetry(boolean airplaneMode, NetworkInfo networkInfo) {
        if (retryCount <= 0) return false;
        retryCount--;
        return rocketInterceptor.shouldRetry(airplaneMode, networkInfo);
    }

    boolean supportsReplay() {
        return rocketInterceptor.supportsReplay();
    }

    String getKey() {
        return url;
    }

    boolean isCancelled() {
        return future != null && future.isCancelled();
    }

    boolean cancel() {
        return this.request == null
                && (this.requests == null || this.requests.isEmpty())
                && this.future != null
                && this.future.cancel(true);
    }


    Exception getException() {
        return exception;
    }

    List<RocketRequest> getRequests() {
        return requests;
    }

    RocketRequest getRequest() {
        return request;
    }

    File getResult() {
        return result;
    }

    RocketRequest.Priority getPriority() {
        return priority;
    }

    int getSequence() {
        return sequence;
    }

    @Nullable
    Object getTag() {
        return request != null ? request.getTag() : null;
    }
}
