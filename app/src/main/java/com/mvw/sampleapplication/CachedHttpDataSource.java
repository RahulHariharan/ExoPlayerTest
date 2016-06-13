package com.mvw.sampleapplication;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.TransferListener;
import com.google.android.exoplayer.util.Predicate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by rahul on 12-06-2016.
 */
public class CachedHttpDataSource extends DefaultHttpDataSource {

    Context mContext;
    public CachedHttpDataSource(String userAgent, Predicate<String> contentTypePredicate, Context context) {
        super(userAgent, contentTypePredicate);
        this.mContext = context;
        videoCache();
    }

    public CachedHttpDataSource(String userAgent, Predicate<String> contentTypePredicate, TransferListener listener, Context context) {
        super(userAgent, contentTypePredicate, listener);
        this.mContext = context;
        videoCache();
    }

    public CachedHttpDataSource(String userAgent, Predicate<String> contentTypePredicate, TransferListener listener, int connectTimeoutMillis, int readTimeoutMillis, Context context) {
        super(userAgent, contentTypePredicate, listener, connectTimeoutMillis, readTimeoutMillis);
        this.mContext = context;
        videoCache();
    }

    public CachedHttpDataSource(String userAgent, Predicate<String> contentTypePredicate, TransferListener listener, int connectTimeoutMillis, int readTimeoutMillis, boolean allowCrossProtocolRedirects, Context context) {
        super(userAgent, contentTypePredicate, listener, connectTimeoutMillis, readTimeoutMillis, allowCrossProtocolRedirects);
        this.mContext = context;
        videoCache();
    }

    private void videoCache(){
        VideoDownloader downloader = new VideoDownloader();
        downloader.execute();
    }

    class VideoDownloader extends AsyncTask<Void, Long, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            OkHttpClient client = new OkHttpClient();
            String url = "http://download.wavetlan.com/SVV/Media/HTTP/MP4/ConvertedFiles/Media-Convert/Unsupported/test7.mp4";
            Call call = client.newCall(new Request.Builder().url(url).get().build());

            try {
                Response response = call.execute();
                if (response.code() == 200 || response.code() == 201) {

                    /*Headers responseHeaders = response.headers();
                    for (int i = 0; i < responseHeaders.size(); i++) {
                        Log.d(LOG_TAG, responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }*/

                    InputStream inputStream = null;
                    try {
                        inputStream = response.body().byteStream();

                        byte[] buff = new byte[1024 * 4];
                        long downloaded = 0;
                        long target = response.body().contentLength();
                        File mediaFile = new File(mContext.getCacheDir(), "mySuperVideo.mp4");
                        OutputStream output = new FileOutputStream(mediaFile);

                        publishProgress(0L, target);
                        while (true) {
                            int readed = inputStream.read(buff);

                            if (readed == -1) {
                                break;
                            }
                            output.write(buff, 0, readed);
                            //write buff
                            downloaded += readed;
                            publishProgress(downloaded, target);
                            if (isCancelled()) {
                                return false;
                            }
                        }

                        output.flush();
                        output.close();

                        return downloaded == target;
                    } catch (IOException ignore) {
                        return false;
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                } else {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        /*@Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            progressBar.setMax(values[1].intValue());
            progressBar.setProgress(values[0].intValue());

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            progressBar.setVisibility(View.GONE);

            if (mediaFile != null && mediaFile.exists()) {
                playVideo();
            }
        }*/
    }
}
