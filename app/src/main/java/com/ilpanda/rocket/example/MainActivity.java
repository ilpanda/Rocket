package com.ilpanda.rocket.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ilpanda.rocket.ChecksumTransformation;
import com.ilpanda.rocket.R;
import com.ilpanda.rocket.Rocket;
import com.ilpanda.rocket.RocketRequest;

import java.io.File;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Rocket.initialize(this);


        downloadApk();


    }


    private void downloadApk() {
        Rocket.get()
                .load(TestUriProvider.APK_DOWNLOAD_0)
                .md5("")
                .callback(new RocketRequest.RocketCallback() {
                    @Override
                    public void onSuccess(File result) {
                        Log.i(TAG, "download success : " + result);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "download  error : " + e);
                    }

                    @Override
                    public void onProgress(long bytesRead, long contentLength, float percent) {
                        Log.e(TAG, "download  progress : " + bytesRead);
                    }
                })
                .download();
    }


}
