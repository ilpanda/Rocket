package com.ilpanda.rocket;

import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okio.ByteString;

public class RocketRequest {

    static final int NO_INTERVAL = -1;

    Rocket rocket;

    private String url;

    private File targetFile;

    private File temFile;

    private int interval = NO_INTERVAL;

    private long fileSize;  //byte

    private String md5;

    private int retryCount = 2;

    private RocketCallback callback;

    boolean forceDownload;

    private boolean cancelled;

    private Object tag;

    private Priority priority;

    private List<RocketTransformation> transformations;

    RocketDownloader.Progress progress;

    public RocketRequest(Rocket rocket, String url) {
        this.rocket = rocket;
        this.url = url;
    }

    public RocketRequest targetFile(File targetFile) {
        this.targetFile = targetFile;
        return this;
    }

    public RocketRequest retryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public RocketRequest md5(String md5) {
        this.md5 = md5;
        transform(new ChecksumTransformation());
        return this;
    }

    public RocketRequest fileSize(long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public RocketRequest callback(RocketCallback callback) {
        this.callback = callback;
        return this;
    }

    public RocketRequest forceDownload() {
        this.forceDownload = true;
        return this;
    }

    public RocketRequest tag(Object tag) {
        this.tag = tag;
        return this;
    }

    public RocketRequest priority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public RocketRequest transform(RocketTransformation transformation) {

        if (transformation == null) {
            throw new IllegalArgumentException("transformation cannot be null.");
        }

        if (transformations == null) {
            transformations = new ArrayList<>(3);
        }

        transformations.add(transformation);
        return this;
    }


    public RocketRequest transform(List<RocketTransformation> transformations) {

        if (transformations == null) {
            throw new IllegalArgumentException("transformations cannot be null.");
        }

        for (int i = 0; i < transformations.size(); i++) {
            transform(transformations.get(i));
        }

        return this;
    }


    public RocketRequest interval(int interval) {

        if (interval <= 0) {
            throw new IllegalArgumentException("interval  must  greater than 0");
        }

        this.interval = interval;
        return this;
    }

    public void download() {
        rocket.download(this);
    }


    boolean needTransform() {
        return this.transformations != null && !this.transformations.isEmpty();
    }


    String getUrl() {
        return url;
    }


    File getTargetFile() {
        return targetFile;
    }

    String getFileMd5() {
        return md5;
    }

    long getFileSize() {
        return fileSize;
    }


    void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
    }

    String getFileName() {

        return ByteString.of(this.url.getBytes()).md5().hex();
    }

    int getRetryCount() {
        return retryCount;
    }

    void cancel() {
        cancelled = true;
    }

    boolean isCancelled() {
        return cancelled;
    }

    @Nullable
    RocketCallback getCallback() {
        return callback;
    }

    File getTemFile() {
        return temFile;
    }

    void setTemFile(File temFile) {
        this.temFile = temFile;
    }

    int getInterval() {
        return interval;
    }

    Object getTag() {
        return tag;
    }

    Priority getPriority() {
        return priority;
    }

    List<RocketTransformation> getTransformations() {
        return transformations;
    }

    public interface RocketCallback {

        void onSuccess(File result);

        void onError(Exception e);

        void onProgress(long bytesRead, long contentLength, float percent);
    }

    public static class SimpleCallback implements RocketCallback {

        @Override
        public void onSuccess(File result) {

        }

        @Override
        public void onError(Exception e) {

        }

        @Override
        public void onProgress(long bytesRead, long contentLength, float percent) {

        }
    }


    public enum Priority {
        LOW,
        NORMAL,
        HIGH
    }

}
