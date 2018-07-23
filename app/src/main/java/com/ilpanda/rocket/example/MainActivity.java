package com.ilpanda.rocket.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ilpanda.rocket.ChecksumTransformation;
import com.ilpanda.rocket.R;
import com.ilpanda.rocket.Rocket;
import com.ilpanda.rocket.RocketRequest;
import com.ilpanda.rocket.Utils;

import java.io.File;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Rocket.initialize(this);


    }


    private void download() {
        String downloadUrl = TestUriProvider.APK_DOWNLOAD_0;
        Rocket.get()
                .load(downloadUrl)
                .download();
    }

    private void downloadCallback() {

        String downloadUrl = TestUriProvider.APK_DOWNLOAD_0;
        Rocket.get()
                .load(downloadUrl)
                .callback(new RocketRequest.RocketCallback() {
                    @Override
                    public void onSuccess(File result) {
                        Log.i(TAG, "download success : " + result.getAbsolutePath());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "download error : \n " + Utils.getThreadStack(e));
                    }

                    @Override
                    public void onProgress(long bytesRead, long contentLength, float percent) {
                        Log.e(TAG, "download progress : " + bytesRead + "-- the content length : "
                                + contentLength + "-- the percent : " + percent);
                    }
                })
                .download();

    }


    private void downloadForce() {

        String downloadUrl = TestUriProvider.APK_DOWNLOAD_0;
        Rocket.get()
                .load(downloadUrl)
                .forceDownload()
                .callback(new RocketRequest.RocketCallback() {
                    @Override
                    public void onSuccess(File result) {
                        Log.i(TAG, "download success : " + result.getAbsolutePath());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "download error : \n " + Utils.getThreadStack(e));
                    }

                    @Override
                    public void onProgress(long bytesRead, long contentLength, float percent) {
                        Log.e(TAG, "download progress : " + bytesRead + "-- the content length : "
                                + contentLength + "-- the percent : " + percent);
                    }
                })
                .download();

    }


    private void checkSpaceBeforeDownload() {

        String downloadUrl = TestUriProvider.APK_DOWNLOAD_0;
        Rocket.get()
                .load(downloadUrl)
                .fileSize(300000000000L)
                .forceDownload()
                .callback(new RocketRequest.RocketCallback() {
                    @Override
                    public void onSuccess(File result) {
                        Log.i(TAG, "download success : " + result.getAbsolutePath());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "download error : \n " + Utils.getThreadStack(e));
                    }

                    @Override
                    public void onProgress(long bytesRead, long contentLength, float percent) {
                        Log.e(TAG, "download progress : " + bytesRead + "-- the content length : "
                                + contentLength + "-- the percent : " + percent);
                    }
                })
                .download();
    }


}
