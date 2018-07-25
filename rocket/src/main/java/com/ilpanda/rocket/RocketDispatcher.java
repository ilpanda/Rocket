package com.ilpanda.rocket;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.ilpanda.rocket.Utils.getService;

public class RocketDispatcher {

    private static final int BATCH_DELAY = 1000;

    private static final int AIRPLANE_MODE_OFF = 0;
    private static final int AIRPLANE_MODE_ON = 1;


    private static final int REQUEST_SUBMIT = 0;
    private static final int REQUEST_PAUSE = 1;
    private static final int REQUEST_RESUME = 2;
    private static final int REQUEST_CANCEL = 3;
    private static final int REQUEST_CANCEL_TAG = 4;
    private static final int REQUEST_COMPLETE = 5;
    private static final int REQUEST_PROGRESS = 6;
    private static final int REQUEST_RETRY = 7;
    private static final int REQUEST_ERROR = 8;
    private static final int REQUEST_CANCEL_CALLBACK = 9;
    private static final int RESPONSE_DELAY_NEXT_BATCH = 10;
    static final int NETWORK_STATE_CHANGE = 11;
    static final int AIRPLANE_MODE_CHANGE = 12;

    static final int RESPONSE_COMPLETE = 13;
    static final int RESPONSE_PROGRESS = 14;
    static final int REQUEST_BATCH_RESUME = 15;


    final HandlerThread dispatcherThread;

    final Map<String, RocketResponse> responseMap;

    final Map<String, RocketRequest> pausedMap;

    final Map<String, RocketRequest> failedMap;

    final Rocket rocket;

    final Handler dispatcherHandler;

    final ExecutorService executorService;

    final List<RocketResponse> batch;

    final Handler mainThreadHandler;

    final Set<Object> pausedTags;

    final boolean scansNetworkChanges;

    final NetworkBroadcastReceiver broadcastReceiver;

    final Context context;

    boolean airplaneMode;


    public RocketDispatcher(Context context, Rocket rocket, ExecutorService executorService, Handler mainThreadHandler) {
        this.context = context;
        this.rocket = rocket;
        this.executorService = executorService;
        this.mainThreadHandler = mainThreadHandler;
        this.dispatcherThread = new HandlerThread("RocketDispatcher HandlerThread", THREAD_PRIORITY_BACKGROUND);
        this.dispatcherThread.start();
        this.dispatcherHandler = new RocketDispatcherHandler(dispatcherThread.getLooper(), this);

        this.responseMap = new HashMap<>();
        this.pausedMap = new HashMap<>();
        this.failedMap = new HashMap<>();
        this.batch = new ArrayList<>(4);
        this.airplaneMode = Utils.isAirplaneModeOn(this.context);
        this.pausedTags = new LinkedHashSet<>();

        this.scansNetworkChanges = Utils.hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE);

        this.broadcastReceiver = new NetworkBroadcastReceiver(this);
        this.broadcastReceiver.register();
    }


    void dispatchSubmit(RocketRequest request) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_SUBMIT, request));
    }

    void dispatchPause(Object tag) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_PAUSE, tag));
    }

    void dispatchResume(Object tag) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_RESUME, tag));
    }


    void dispatchCancel(RocketRequest request) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_CANCEL, request));
    }

    void dispatchComplete(RocketResponse response) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_COMPLETE, response));
    }

    void dispatchRetry(RocketResponse response) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_RETRY, response));
    }

    void dispatchCancelSuccess(String url) {


    }

    void dispatchProgress(RocketRequest request) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_PROGRESS, request));
    }

    void dispatchError(RocketResponse response) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_ERROR, response));
    }

    void dispatchCancelTag(Object tag) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_CANCEL_TAG, tag));
    }

    void dispatchCancelCallback(RocketRequest.RocketCallback tag) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_CANCEL_CALLBACK, tag));
    }

    void dispatchNetworkStateChange(NetworkInfo info) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(NETWORK_STATE_CHANGE, info));
    }

    void dispatchAirplaneModeChange(boolean airplaneMode) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(AIRPLANE_MODE_CHANGE,
                airplaneMode ? AIRPLANE_MODE_ON : AIRPLANE_MODE_OFF, 0));
    }

    private void performSubmit(RocketRequest request) {
        performSubmit(request, true);
    }

    private void performSubmit(RocketRequest request, boolean dismissFailed) {

        if (pausedTags.contains(request.getTag())) {
            pausedMap.put(request.getUrl(), request);
            return;
        }

        RocketResponse response = responseMap.get(request.getUrl());
        if (response != null) {
            response.attach(request);
            return;
        }

        response = RocketResponse.forRequest(rocket, this, request);

        response.future = executorService.submit(response);

        responseMap.put(request.getUrl(), response);

        if (dismissFailed) {
            failedMap.remove(request.getUrl());
        }
    }


    @SuppressLint("MissingPermission")
    void performRetry(RocketResponse response) {

        NetworkInfo networkInfo = null;

        if (scansNetworkChanges) {
            ConnectivityManager connectivityManager = Utils.getService(context, CONNECTIVITY_SERVICE);
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }

        if (response.shouldRetry(airplaneMode, networkInfo)) {
            response.exception = null;
            response.future = executorService.submit(response);
        } else {
            boolean willReplay = scansNetworkChanges && response.supportsReplay();
            performError(response);
            if (willReplay) {
                markForReplay(response);
            }
        }
    }


    private void markForReplay(RocketResponse response) {
        RocketRequest request = response.getRequest();
        if (request != null) {
            markForReplay(request);
        }
        List<RocketRequest> joined = response.getRequests();
        if (joined != null) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, n = joined.size(); i < n; i++) {
                RocketRequest join = joined.get(i);
                markForReplay(join);
            }
        }
    }

    private void markForReplay(RocketRequest request) {
        failedMap.put(request.getUrl(), request);
    }


    void performError(RocketResponse response) {
        responseMap.remove(response.getKey());
        batch(response);
    }

    void performAirplaneModeChange(boolean airplaneMode) {
        this.airplaneMode = airplaneMode;
    }

    void performNetworkStateChange(NetworkInfo info) {
        // Intentionally check only if isConnected() here before we flush out failed actions.
        if (info != null && info.isConnected()) {
            flushFailedActions();
        }
    }


    private void flushFailedActions() {

        if (failedMap.isEmpty()) return;
        Iterator<RocketRequest> iterator = failedMap.values().iterator();
        while (iterator.hasNext()) {
            RocketRequest next = iterator.next();
            iterator.remove();
            performSubmit(next, false);
        }
    }


    private void performPause(Object tag) {
        // Trying to pause a tag that is already paused.
        if (!pausedTags.add(tag)) {
            return;
        }

        for (Iterator<RocketResponse> it = responseMap.values().iterator(); it.hasNext(); ) {
            RocketResponse next = it.next();
            RocketRequest single = next.getRequest();
            List<RocketRequest> joined = next.getRequests();

            boolean hasMultiple = joined != null && !joined.isEmpty();

            // Hunter has no requests, bail early.
            if (single == null && !hasMultiple) {
                continue;
            }

            if (single != null && single.getTag().equals(tag)) {
                next.detach(single);
                pausedMap.put(single.getUrl(), single);
            }

            if (hasMultiple) {
                for (int i = joined.size() - 1; i >= 0; i--) {
                    RocketRequest request = joined.get(i);
                    if (!request.getTag().equals(tag)) {
                        continue;
                    }
                    next.detach(request);
                    pausedMap.put(request.getUrl(), request);
                }
            }

            if (next.cancel()) {
                it.remove();
            }
        }
    }

    private void performCancelCallback(RocketRequest.RocketCallback cancelCallback) {

        List<RocketResponse> responseList = new ArrayList<>(responseMap.values());
        for (int i = 0, n = responseList.size(); i < n; i++) {
            // cancel request
            RocketResponse response = responseList.get(i);
            RocketRequest request = response.getRequest();
            cancelCallback(request, cancelCallback);

            // cancel  attach  request
            List<RocketRequest> requests = response.getRequests();
            if (requests != null && !requests.isEmpty()) {
                for (int j = 0; j < requests.size(); j++) {
                    RocketRequest rocketRequest = requests.get(i);
                    cancelCallback(rocketRequest, cancelCallback);
                }
            }
        }
    }

    private void performResume(Object tag) {
        // Trying to resume a tag that is not paused.
        if (!pausedTags.remove(tag)) {
            return;
        }

        List<RocketRequest> batch = null;
        for (Iterator<RocketRequest> i = pausedMap.values().iterator(); i.hasNext(); ) {
            RocketRequest action = i.next();
            if (action.getTag().equals(tag)) {
                if (batch == null) {
                    batch = new ArrayList<>();
                }
                batch.add(action);
                i.remove();
            }
        }

        if (batch != null) {
            mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(REQUEST_BATCH_RESUME, batch));
        }
    }


    private void performCancel(RocketRequest request) {
        String key = request.getUrl();
        RocketResponse rocketResponse = responseMap.get(key);

        if (rocketResponse != null) {
            request.cancel();
            rocketResponse.detach(request);
            if (rocketResponse.cancel()) {
                responseMap.remove(key);
            }
        }

        if (pausedTags.contains(request.getTag())) {
            pausedMap.remove(key);
        }

        failedMap.remove(key);
    }

    private void performComplete(RocketResponse response) {
        responseMap.remove(response.getKey());
        batch(response);
    }

    private void performCancelTag(Object tag) {
        List<RocketResponse> responseList = new ArrayList<>(responseMap.values());
        for (int i = 0, n = responseList.size(); i < n; i++) {
            // cancel request
            RocketResponse response = responseList.get(i);
            RocketRequest request = response.getRequest();
            cancelRequest(request, tag);

            // cancel  attach  request
            List<RocketRequest> requests = response.getRequests();
            if (requests != null && !requests.isEmpty()) {
                for (int j = 0; j < requests.size(); j++) {
                    RocketRequest rocketRequest = requests.get(i);
                    cancelRequest(rocketRequest, tag);
                }
            }
        }
    }


    void performProgress(RocketRequest request) {
        mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(RESPONSE_PROGRESS, request));
    }

    private void performBatch() {
        List<RocketResponse> copy = new ArrayList<>(batch);
        batch.clear();
        mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(RESPONSE_COMPLETE, copy));
    }

    private void batch(RocketResponse response) {
        if (response.isCancelled()) return;
        batch.add(response);
        if (!dispatcherHandler.hasMessages(RESPONSE_DELAY_NEXT_BATCH)) {
            dispatcherHandler.sendEmptyMessageDelayed(RESPONSE_DELAY_NEXT_BATCH, BATCH_DELAY);
        }
    }

    private static class RocketDispatcherHandler extends Handler {

        private final RocketDispatcher dispatcher;

        RocketDispatcherHandler(Looper looper, RocketDispatcher dispatcher) {
            super(looper);
            this.dispatcher = dispatcher;
        }

        @Override
        public void handleMessage(final Message msg) {

            switch (msg.what) {

                case REQUEST_SUBMIT:
                    RocketRequest request = (RocketRequest) msg.obj;
                    dispatcher.performSubmit(request);
                    break;

                case REQUEST_COMPLETE:
                    dispatcher.performComplete((RocketResponse) msg.obj);
                    break;

                case REQUEST_ERROR:
                    dispatcher.performError((RocketResponse) msg.obj);
                    break;

                case REQUEST_PROGRESS:
                    dispatcher.performProgress((RocketRequest) msg.obj);
                    break;

                case RESPONSE_DELAY_NEXT_BATCH:
                    dispatcher.performBatch();
                    break;

                case REQUEST_RETRY:
                    dispatcher.performRetry((RocketResponse) msg.obj);
                    break;

                case REQUEST_CANCEL:
                    dispatcher.performCancel((RocketRequest) msg.obj);
                    break;

                case REQUEST_CANCEL_TAG:
                    dispatcher.performCancelTag(msg.obj);
                    break;

                case REQUEST_PAUSE:
                    dispatcher.performPause(msg.obj);
                    break;

                case REQUEST_RESUME:
                    dispatcher.performResume(msg.obj);
                    break;

                case REQUEST_CANCEL_CALLBACK:
                    dispatcher.performCancelCallback((RocketRequest.RocketCallback) msg.obj);
                    break;

                case NETWORK_STATE_CHANGE: {
                    NetworkInfo info = (NetworkInfo) msg.obj;
                    dispatcher.performNetworkStateChange(info);
                    break;
                }
                case AIRPLANE_MODE_CHANGE: {
                    dispatcher.performAirplaneModeChange(msg.arg1 == AIRPLANE_MODE_ON);
                    break;
                }

            }
        }
    }

    private void cancelRequest(RocketRequest request, Object tag) {
        if (request != null && tag.equals(request.getTag())) {
            performCancel(request);
        }
    }

    private void cancelCallback(RocketRequest request, RocketRequest.RocketCallback cancelCallback) {

        if (request.getCallback() == null) return;

        if (request.getCallback() == cancelCallback) {
            String key = request.getUrl();
            RocketResponse rocketResponse = responseMap.get(key);
            // should   I  use  request.cancel() ?
            if (rocketResponse != null) {
                rocketResponse.detach(request);
            }
        }
    }

    static class NetworkBroadcastReceiver extends BroadcastReceiver {

        static final String EXTRA_AIRPLANE_STATE = "state";

        private RocketDispatcher dispatcher;

        public NetworkBroadcastReceiver(RocketDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        void register() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_AIRPLANE_MODE_CHANGED);
            if (dispatcher.scansNetworkChanges) {
                filter.addAction(CONNECTIVITY_ACTION);
            }
            dispatcher.context.registerReceiver(this, filter);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            final String action = intent.getAction();

            if (ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                if (!intent.hasExtra(EXTRA_AIRPLANE_STATE)) {
                    return; // No airplane state, ignore it. Should we query Utils.isAirplaneModeOn?
                }
                dispatcher.dispatchAirplaneModeChange(intent.getBooleanExtra(EXTRA_AIRPLANE_STATE, false));
            } else if (CONNECTIVITY_ACTION.equals(action)) {
                ConnectivityManager connectivityManager = getService(context, CONNECTIVITY_SERVICE);
                dispatcher.dispatchNetworkStateChange(connectivityManager.getActiveNetworkInfo());
            }
        }

    }


}
