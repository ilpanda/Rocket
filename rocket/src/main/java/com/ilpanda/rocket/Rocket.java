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

    final Context context;

    private RocketDispatcher dispatcher;

    private static String DEFAULT_PATH;

    private List<RocketInterceptor> interceptorList;


    public static Rocket get() {
        if (singleton == null) {
            synchronized (Rocket.class) {
                if (singleton == null) {
                    if (RocketContentProvider.context == null) {
                        throw new IllegalStateException("context == null");
                    }
                    singleton = new Rocket(RocketContentProvider.context);
                }
            }
        }
        return singleton;
    }

    private Rocket(Context context) {

        this.context = context;
        DEFAULT_PATH = new File(context.getExternalFilesDir(null), "").getAbsolutePath();

        RocketExecutorService executorService = new RocketExecutorService();
        dispatcher = new RocketDispatcher(context, this, executorService, HANDLER);

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
        cancel(request, true);
    }

    public void cancel(RocketRequest request, boolean interruptIfRunning) {
        if (request == null) {
            throw new IllegalArgumentException("Cannot cancel  with null request ");
        }
        dispatcher.dispatchCancel(request, interruptIfRunning);
    }

    public void cancelTag(Object tag) {
        cancelTag(tag, true);
    }

    public void cancelTag(Object tag, boolean interruptIfRunning) {
        if (tag == null) {
            throw new IllegalArgumentException("Cannot cancel requests with null tag.");
        }
        dispatcher.dispatchCancelTag(tag, interruptIfRunning);
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
            callback.onError(request.getUrl(), e);
        } else {
            callback.onSuccess(request.getUrl(), result);
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

