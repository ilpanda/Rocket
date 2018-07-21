package com.ilpanda.rocket;

import android.os.SystemClock;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class RocketDownloader implements Downloader {

    private static final String TAG = "RocketDownloader";

    private OkHttpClient okHttpClient;

    private static final int DOWNLOAD_CHUNK_SIZE = 1024 * 8;

    private static final int MIN_REFRESH_TIME = 1000;

    private RocketDispatcher dispatcher;


    public RocketDownloader(RocketDispatcher dispatcher) {

        this.dispatcher = dispatcher;

        okHttpClient = new OkHttpClient.Builder()
                .readTimeout(25, TimeUnit.SECONDS)
                .connectTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .build();
    }


    @Override
    public File download(RocketRequest request) throws IOException {

        okhttp3.Request downloaderRequest = createRequest(request);

        Response response = okHttpClient.newCall(downloaderRequest).execute();

        BufferedSink sink = null;
        BufferedSource source = null;

        File result;

        try {

            if (!response.isSuccessful()) {
                throw new UnExpectedResponseCodeException("Unexpected code: " + response);
            }

            long contentLength;
            String contentLengthStr = response.header("Content-Length");

            // check  the  Content-length
            if (!TextUtils.isEmpty(contentLengthStr)) {
                contentLength = Long.parseLong(contentLengthStr);
            } else {
                throw new ResponseEmptyException("Content-Length is null" + response);
            }

            if (contentLength == 0) {
                throw new ResponseEmptyException("the  response body size  is  0" + response);
            }

            // check  the ResponseBody
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new ResponseEmptyException("the  response body is  null" + response);
            }

            source = responseBody.source();
            sink = Okio.buffer(Okio.sink(request.getTemFile()));

            writeData(request, source, sink, contentLength, dispatcher);

            result = renameFile(request);

        } catch (IOException e) {
            throw e;
        } finally {
            Utils.closeQuietly(response, source, sink);
        }

        return result;
    }

    private File renameFile(RocketRequest request) {
        File temFile = request.getTemFile();
        File targetFile = request.getTargetFile();
        return temFile.renameTo(targetFile) ? targetFile : null;
    }


    private long lastRefreshTime;

    private void writeData(RocketRequest request, BufferedSource source, BufferedSink sink, long contentLength, RocketDispatcher dispatcher) throws IOException {
        long readCount;
        long totalRead = 0;
        int interval = request.getInterval() == RocketRequest.NO_INTERVAL ? MIN_REFRESH_TIME : request.getInterval();
        while (((readCount = source.read(sink.buffer(), DOWNLOAD_CHUNK_SIZE)) != -1)) {
            sink.emit();
            totalRead += readCount;
            long currentTime = SystemClock.uptimeMillis();
            if (currentTime - lastRefreshTime >= interval) {
                float percent = Math.max(Math.min((totalRead * 1.0F) / contentLength, 1), 0);
                request.progress = new Progress(totalRead, contentLength, percent);
                dispatcher.dispatchProgress(request);
                lastRefreshTime = currentTime;
            }
        }
        request.progress = new Progress(totalRead, contentLength, 1.0f);
        dispatcher.dispatchProgress(request);
    }

    private static okhttp3.Request createRequest(RocketRequest request) {
        String url = request.getUrl();
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(url);
        return builder.build();
    }


    static class Progress {
        long bytesRead;
        long contentLength;
        float percent;

        public Progress(long bytesRead, long contentLength, float percent) {
            this.bytesRead = bytesRead;
            this.contentLength = contentLength;
            this.percent = percent;
        }
    }

}
