package com.ilpanda.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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

        initView();


        String downloadUrl = TestUriProvider.APK_DOWNLOAD_1;

        Rocket.get()
                .load(downloadUrl)
                .forceDownload()
                .callback(new RocketRequest.RocketCallback() {

                    @Override
                    public void onSuccess(String url, File result) {
                        Log.i(TAG, "download success : " + result.getAbsolutePath());
                    }

                    @Override
                    public void onError(String url, Exception e) {
                        Toast.makeText(MainActivity.this, "下载失败,请查看日志。", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "download error : \n " + Utils.getThreadStack(e));
                    }

                    @Override
                    public void onProgress(long bytesRead, long contentLength, float percent) {
                        Log.i(TAG, "download progress : " + bytesRead + "-- the content length : "
                                + contentLength + "-- the percent : " + percent);
                    }
                })
                .download();

    }


    private void initView() {

        findViewById(R.id.download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                download();
            }
        });

        findViewById(R.id.download_callback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadCallback();
            }

        });

        findViewById(R.id.check_disk_space).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkSpaceBeforeDownload();
            }
        });

        findViewById(R.id.download_checksum).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checksum();
            }
        });

        findViewById(R.id.download_interval).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadInterval();
            }
        });
    }


    private void download() {
        String downloadUrl = TestUriProvider.APK_DOWNLOAD_0;
        Rocket.get()
                .load(downloadUrl)
                .download();
    }

    private void downloadCallback() {

        String downloadUrl = TestUriProvider.APK_DOWNLOAD_1;
        Rocket.get()
                .load(downloadUrl)
                .callback(new RocketRequest.RocketCallback() {

                    @Override
                    public void onSuccess(String url, File result) {
                        Log.i(TAG, "download success : " + result.getAbsolutePath());
                    }

                    @Override
                    public void onError(String url, Exception e) {
                        Toast.makeText(MainActivity.this, "下载失败,请查看日志。", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "download error : \n " + Utils.getThreadStack(e));
                    }

                    @Override
                    public void onProgress(long bytesRead, long contentLength, float percent) {
                        Log.i(TAG, "download progress : " + bytesRead + "-- the content length : "
                                + contentLength + "-- the percent : " + percent);
                    }
                })
                .download();
    }


    private void downloadForce() {

        String downloadUrl = TestUriProvider.APK_DOWNLOAD_1;
        Rocket.get()
                .load(downloadUrl)
                .forceDownload()
                .callback(new RocketRequest.RocketCallback() {
                    @Override
                    public void onSuccess(String url, File result) {
                        Log.i(TAG, "download success : " + result.getAbsolutePath());
                    }

                    @Override
                    public void onError(String url, Exception e) {
                        Toast.makeText(MainActivity.this, "下载失败,请查看日志。", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "download error : \n " + Utils.getThreadStack(e));
                    }


                    @Override
                    public void onProgress(long bytesRead, long contentLength, float percent) {
                        Log.i(TAG, "download progress : " + bytesRead + "-- the content length : "
                                + contentLength + "-- the percent : " + percent);
                    }
                })
                .download();

    }


    private void checkSpaceBeforeDownload() {

        String downloadUrl = TestUriProvider.APK_DOWNLOAD_2;
        Rocket.get()
                .load(downloadUrl)
                .fileSize(Long.MAX_VALUE)
                .forceDownload()
                .callback(new RocketRequest.RocketCallback() {
                    @Override
                    public void onSuccess(String url, File result) {
                        Log.i(TAG, "download success : " + result.getAbsolutePath());
                    }

                    @Override
                    public void onError(String url, Exception e) {
                        Toast.makeText(MainActivity.this, "下载失败,请查看日志。", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "download error : \n " + Utils.getThreadStack(e));
                    }


                    @Override
                    public void onProgress(long bytesRead, long contentLength, float percent) {
                        Log.i(TAG, "download progress : " + bytesRead + "-- the content length : "
                                + contentLength + "-- the percent : " + percent);
                    }
                })
                .download();
    }

    private void downloadTag() {
        String downloadUrl = TestUriProvider.APK_DOWNLOAD_3;
        Rocket.get()
                .load(downloadUrl)
                .tag(this)
                .download();
    }


    private void cancelTag() {
        Rocket.get().cancelTag(this);
    }


    private void checksum() {
        // 你可以输入一个错误错误的 MD5 ,看是否会回调 onError() 。
        String downloadUrl = TestUriProvider.APK_DOWNLOAD_0;
        Rocket.get()
                .load(downloadUrl)
                .md5("952C473765A25DC003C6750BB85C947F")
                .callback(new RocketRequest.SimpleCallback() {
                    @Override
                    public void onSuccess(String url, File result) {
                        Log.i(TAG, "download success : " + result.getAbsolutePath());
                    }

                    @Override
                    public void onError(String url, Exception e) {
                        Toast.makeText(MainActivity.this, "下载失败,请查看日志。", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "download error : \n " + Utils.getThreadStack(e));
                    }


                })
                .download();
    }


    private void downloadInterval() {
        String downloadUrl = TestUriProvider.APK_DOWNLOAD_3;
        Rocket.get()
                .load(downloadUrl)
                .interval(3000)
                .forceDownload()
                .callback(new RocketRequest.SimpleCallback() {

                    @Override
                    public void onProgress(long bytesRead, long contentLength, float percent) {
                        Log.i(TAG, "download progress : " + bytesRead + "-- the content length : "
                                + contentLength + "-- the percent : " + percent);
                    }

                    @Override
                    public void onError(String String, Exception e) {
                        Toast.makeText(MainActivity.this, "下载失败,请查看日志。", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "download error : \n " + Utils.getThreadStack(e));
                    }
                })
                .download();
    }


}
