###  Rocket

Rocket 是为了让 Android 平台上的文件下载更简单。

通过几行代码就可以轻松实现下载功能 。

---

###  优点
1. 链式调用。
2. 多个任务同时下载。(默认最大并发数为3)
3. 取消。
4. 暂停。
5. 继续。
6. 查看下载进度
7. 按优先级下载任务。
8. 同一 Url , 避免重复请求。
9. 下载出错,重新下载。
10. 支持对下载文件额外处理。
11. 文件 MD5 校验。
12. 如果本地文件存在,避免重复网络请求。

---

### 下载

你可以下载源码依赖 module , 也可以通过 Maven or Gradle 依赖 。

```
<dependency>
  <groupId>com.ilpanda</groupId>
  <artifactId>rocket</artifactId>
  <version>1.0.4</version>
  <type>pom</type>
</dependency>
```

Gradle :

```
compile 'com.ilpanda:rocket:1.0.4'
```
---


### 添加权限

你需要在 AndroidManifest.xml 添加以下权限。

```
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```
---

### 使用 :

链式调用 :

```
   Rocket.get()
                .load(downloadUrl)
                .download();
```


回调 : 回调方法都是在主线程执行。

```
      Rocket.get()
                .load(downloadUrl)
                .callback(new RocketRequest.RocketCallback() {
                    @Override
                    public void onSuccess(String url, File result) {

                    }

                    @Override
                    public void onError(String url, Exception e) {

                    }

                    @Override
                    public void onProgress(long bytesRead, long contentLength, float percent) {

                    }
                })
                .download();
```

如果你只关注某一个回调,使用 SimpleCallback :

```
        Rocket.get()
                .load(downloadUrl)
                .callback(new RocketRequest.SimpleCallback(){
                    @Override
                    public void onProgress(long bytesRead, long contentLength, float percent) {
                        super.onProgress(bytesRead, contentLength, percent);
                    }
                })
                .download();
```


如果文件已经成功下载到本地,默认情况下 Rocket 不会再次下载。

```
      Rocket.get()
                .load(downloadUrl)
                .callback(new RocketRequest.SimpleCallback() {
                    @Override
                    public void onSuccess(String url, File result) {

                    }
                })
                .download();
```

如果文件已经成功下载到本地,但是你想要强制从网络下载文件, 调用 forceDownload() 方法。

```
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
```

默认情况下, Rocket 每隔一秒刷新一次下载进度,如果你想更改刷新间隔,可以使用 interval() 方法 :

```
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
```

如果你事先知道文件大小,Rocket 可以为你检测 SD 卡空间是否足够。默认情况, Rocket 需要下载文件大小的
1.3倍磁盘空间。

```
// 下面代码会下载失败,因为所需要的空间超过了磁盘空间。
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
```

默认情况下,文件的下载路径为 /sdcard/android/data/包名/files 目录下, 你也可以为你的请求
配置下载路径。


```
      Rocket.get()
                .load(downloadUrl)
                .targetFile(targetFile)
                .download();
```


如果你想要取消下载请求,你需要设置 tag 。

```
    private void downloadTag() {
        Rocket.get()
                .load(downloadUrl)
                .tag(this)
                .download();
    }


    private void cancelTag() {
        Rocket.get().cancelTag(this);
    }

```

如果文件下载成功后,你想要校验文件的 MD5 ,你可以使用以下代码 :
如果文件校验失败,会回调 RocketCallback 的 onError() 方法。

```
        Rocket.get()
                .load(downloadUrl)
                .md5(fileMD5)
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
```

---

### TODO

1. 添加日志配置。
2. 支持断点下载。
4. 支持自定义配置 Rocket 。
---

### Thanks

[Picasso](https://github.com/square/picasso)
