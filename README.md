# 什么是 Rocket !

Rocket 是为了让 Android 平台上的文件下载更简单。

通过几行代码就可以轻松实现下载功能 。

依赖的网络框架为 OkHttp 。

---

###  优点
1. 链式调用。
2. 多个任务同时下载。
3. 取消。
4. 暂停。
5. 继续。
6. 查看下载进度
7. 按优先级下载任务。
8. 同一 Url , 避免重复请求。
9. 下载出错,重新下载。
10. 支持对下载文件额外处理。
11. 文件 MD5 校验。
12. 如果本地文件存在避免,重复网络请求。

---

### 配置


在使用 Rocket 之前, 你需要在你的 Application 当中初始化。

```
        Rocket.initialize(this);
```


---

### 使用

最简单的方法 :

```
   Rocket.get()
                .load(downloadUrl)
                .download();
```


如果你想要使用回调, 使用 RocketCallback 。

```
        Rocket.get()
                .load(downloadUrl)
                .callback(new RocketRequest.RocketCallback() {
                    @Override
                    public void onSuccess(File result) {

                    }

                    @Override
                    public void onError(Exception e) {

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


如果文件已经下载到本地,默认情况下 Rocket 不会重新下载。

```
     Rocket.get()
                .load(downloadUrl)
                .callback(new RocketRequest.SimpleCallback() {
                    @Override
                    public void onSuccess(File result) {

                    }
                })
                .download();
```

如果你想要强制下载文件, 调用 forceDownload() 方法。

```
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
```

默认情况下, Rocket 每隔一秒刷新一次下载进度,如果你想更改,可以使用 interval() 方法:

```
      Rocket.get()
                .load(downloadUrl)
                .interval(3000)  // 每隔 3s
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
```

如果你事先知道文件大小, Rocket 可以为你检测 SD 卡空间是否足够。

```

```

