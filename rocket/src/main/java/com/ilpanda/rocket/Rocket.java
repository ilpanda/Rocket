package com.ilpanda.rocket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.ilpanda.rocket.RocketDispatcher.REQUEST_BATCH_RESUME;
import static com.ilpanda.rocket.RocketDispatcher.RESPONSE_COMPLETE;
import static com.ilpanda.rocket.RocketDispatcher.RESPONSE_PROGRESS;

public class Rocket {


    private static final String TAG = "Rocket";

    @SuppressLint("StaticFieldLeak")
    private static volatile Rocket singleton = null;

    private final Context context;

    private RocketDispatcher dispatcher;

    private static String DEFAULT_PATH;

    private List<RocketInterceptor> interceptorList;

    private Logger logger;

    private boolean loggingEnabled;


    public static Rocket get() {
        if (singleton == null) {
            synchronized (Rocket.class) {
                if (singleton == null) {
                    if (RocketContentProvider.context == null) {
                        throw new IllegalStateException("context == null");
                    }
                    singleton = new Rocket.Builder(RocketContentProvider.context).Build();
                }
            }
        }
        return singleton;
    }

    private Rocket(Context context, String downloadPath, RocketDispatcher dispatcher, Logger logger, boolean loggingEnabled, Downloader downloader,
                   @Nullable List<RocketInterceptor> interceptors, @Nullable List<RocketInterceptor> networkInterceptors) {

        this.context = context;
        DEFAULT_PATH = downloadPath;
        this.dispatcher = dispatcher;
        this.logger = logger;
        this.loggingEnabled = loggingEnabled;

        dispatcher.rocket = this;

        // before download
        this.interceptorList = new ArrayList<>();
        this.interceptorList.add(new UrlInterceptor());
        this.interceptorList.add(new PrepareFileInterceptor());
        this.interceptorList.add(new DiskSpaceInterceptor());
        if (interceptors != null) {
            this.interceptorList.addAll(interceptors);
        }
        this.interceptorList.add(new NetworkInterceptor(downloader));

        // after download
        if (networkInterceptors != null) {
            this.interceptorList.addAll(networkInterceptors);
        }
    }


    public static class Builder {

        private final Context context;

        private Downloader downloader;

        private ExecutorService executorService;

        private Logger logger;

        private String downloadPath;

        private List<RocketInterceptor> interceptors;

        private List<RocketInterceptor> networkInterceptors;

        private boolean loggingEnabled;

        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null");
            }
            this.context = context;
        }

        public Builder logger(Logger logger) {
            if (logger == null) {
                throw new IllegalArgumentException("Logger must not be null");
            }
            this.logger = logger;
            return this;
        }

        public Builder downloader(Downloader downloader) {
            if (downloader == null) {
                throw new IllegalArgumentException("Downloader must not be null");
            }
            this.downloader = downloader;
            return this;
        }

        public Builder executorService(ExecutorService executorService) {
            if (executorService == null) {
                throw new IllegalArgumentException("ExecutorService must not be null");
            }
            this.executorService = executorService;
            return this;
        }

        public Builder downloadPath(String downloadPath) {
            if (TextUtils.isEmpty(downloadPath)) {
                throw new IllegalArgumentException("downloadPath must not be null");
            }
            this.downloadPath = downloadPath;
            return this;
        }


        public Builder addInterceptor(RocketInterceptor interceptor) {

            if (interceptor == null) {
                throw new IllegalArgumentException("interceptor must not be null");
            }
            if (interceptors == null) {
                interceptors = new ArrayList<>();
            }
            interceptors.add(interceptor);
            return this;
        }


        public Builder addNetworkInterceptor(RocketInterceptor interceptor) {

            if (interceptor == null) {
                throw new IllegalArgumentException("interceptor must not be null");
            }
            if (networkInterceptors == null) {
                networkInterceptors = new ArrayList<>();
            }
            networkInterceptors.add(interceptor);
            return this;
        }


        public Builder loggingEnabled(boolean loggingEnabled) {
            this.loggingEnabled = loggingEnabled;
            return this;
        }


        public Rocket Build() {

            Context context = this.context;

            if (executorService == null) {
                this.executorService = new RocketExecutorService();
            }

            if (downloadPath == null) {
                this.downloadPath = new File(context.getExternalFilesDir(null), "").getAbsolutePath();
            }

            if (this.logger == null) {
                logger = new Logger.AndroidLogger(TAG);
            }

            RocketDispatcher dispatcher = new RocketDispatcher(context, executorService, HANDLER, logger, loggingEnabled);
            if (downloader == null) {
                this.downloader = new RocketDownloader(dispatcher);
            }

            return new Rocket(context, this.downloadPath, dispatcher, logger, loggingEnabled, downloader, interceptors, networkInterceptors);
        }

    }


    public RocketRequest load(String url) {
        if (url == null) {
            throw new IllegalArgumentException("download  url  must not be null   ");
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


    public boolean isInFlight(String url) {
        return dispatcher.isInFlight(url);
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

