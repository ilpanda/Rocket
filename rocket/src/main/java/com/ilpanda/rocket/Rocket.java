package com.ilpanda.rocket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.ilpanda.rocket.RocketDispatcher.REQUEST_BATCH_RESUME;
import static com.ilpanda.rocket.RocketDispatcher.RESPONSE_COMPLETE;
import static com.ilpanda.rocket.RocketDispatcher.RESPONSE_PROGRESS;

public class Rocket {


    @SuppressLint("StaticFieldLeak")
    private static volatile Rocket singleton = null;

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    private RocketDispatcher dispatcher;

    private static String DEFAULT_PATH;

    private List<RocketInterceptor> interceptorList;

    public static void initialize(Context context) {

        if (context == null) {
            throw new IllegalArgumentException("context  is  null");
        }

        Rocket.context = context;
        DEFAULT_PATH = new File(context.getExternalFilesDir(null), "").getAbsolutePath();
    }


    public static Rocket get() {
        if (singleton == null) {
            synchronized (Rocket.class) {
                if (singleton == null) {
                    singleton = new Rocket();
                }
            }
        }
        return singleton;
    }

    private Rocket() {

        RocketExecutorService executorService = new RocketExecutorService();
        dispatcher = new RocketDispatcher(this, executorService, HANDLER);

        RocketDownloader rocketDownloader = new RocketDownloader(dispatcher);

        interceptorList = new ArrayList<>();
        interceptorList.add(new UrlInterceptor());
        interceptorList.add(new PrepareFileInterceptor());
        interceptorList.add(new DiskSpaceInterceptor());
        interceptorList.add(new NetworkInterceptor(rocketDownloader));
    }

    public RocketRequest load(String url) {
        if (url == null) {
            throw new IllegalArgumentException("download  url  can not be null   ");
        }
        return new RocketRequest(this, url);
    }


    public void cancel(RocketRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Cannot cancel  with null request ");
        }
        dispatcher.dispatchCancel(request);
    }

    public void cancelTag(Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Cannot cancel requests with null tag.");
        }
        dispatcher.dispatchCancelTag(tag);
    }

    public void cancelCallback(RocketRequest.RocketCallback callback) {
        if (callback == null) return;
        dispatcher.dispatchCancelCallback(callback);
    }

    public void pauseTag(Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Cannot cancel requests with null tag.");
        }
        dispatcher.dispatchPause(tag);
    }


    public void resumeTag(Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Cannot cancel requests with null tag.");
        }
        dispatcher.dispatchResume(tag);
    }


    void download(RocketRequest request) {
        dispatcher.dispatchSubmit(request);
    }

    String getDefaultPath() {
        return DEFAULT_PATH;
    }

    List<RocketInterceptor> getInterceptorList() {
        return interceptorList;
    }

    void complete(RocketResponse response) {

        RocketRequest single = response.getRequest();

        List<RocketRequest> joined = response.getRequests();

        boolean hasMultiple = joined != null && !joined.isEmpty();
        boolean shouldDeliver = single != null || hasMultiple;

        if (!shouldDeliver) {
            return;
        }

        File result = response.getResult();
        Exception exception = response.getException();

        if (single != null) {
            deliverResult(result, single, exception);
        }

        if (hasMultiple) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, n = joined.size(); i < n; i++) {
                RocketRequest join = joined.get(i);
                deliverResult(result, join, exception);
            }
        }
    }


    void deliverResult(File result, RocketRequest request, Exception e) {

        if (request.isCancelled()) {
            return;
        }

        RocketRequest.RocketCallback callback = request.getCallback();

        if (callback == null) {
            return;
        }

        if (e != null) {
            callback.onError(e);
        } else {
            callback.onSuccess(result);
        }
    }

    void deliverProgress(RocketRequest request) {
        RocketRequest.RocketCallback callback = request.getCallback();
        if (callback == null) return;

        RocketDownloader.Progress progress = request.progress;
        request.getCallback().onProgress(progress.bytesRead, progress.contentLength, progress.percent);
    }


    private static Handler HANDLER = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RESPONSE_COMPLETE:
                    @SuppressWarnings("unchecked")
                    List<RocketResponse> responseList = (List<RocketResponse>) msg.obj;
                    for (int i = 0; i < responseList.size(); i++) {
                        RocketResponse response = responseList.get(i);
                        response.rocket.complete(response);
                    }
                    break;
                case RESPONSE_PROGRESS:
                    RocketRequest rocketRequest = (RocketRequest) msg.obj;
                    rocketRequest.rocket.deliverProgress(rocketRequest);
                    break;

                case REQUEST_BATCH_RESUME:
                    @SuppressWarnings("unchecked")
                    List<RocketRequest> rocketRequestList = (List<RocketRequest>) msg.obj;
                    for (int i = 0; i < rocketRequestList.size(); i++) {
                        RocketRequest request = rocketRequestList.get(i);
                        request.rocket.download(request);
                    }
            }
        }
    };
}

