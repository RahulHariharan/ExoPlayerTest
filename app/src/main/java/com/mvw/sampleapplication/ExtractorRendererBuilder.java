package com.mvw.sampleapplication;

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.FileDataSource;
import com.google.android.exoplayer.upstream.TransferListener;
import com.google.android.exoplayer.upstream.cache.Cache;
import com.google.android.exoplayer.upstream.cache.CacheDataSource;
import com.google.android.exoplayer.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer.upstream.cache.SimpleCache;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * A {@link } for streams that can be read using an {@link Extractor}.
 */
public class ExtractorRendererBuilder implements DemoPlayer.RendererBuilder, TransferListener{

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private final Context context;
    private final String userAgent;
    private Uri uri;
    File mediaFile;
    byte[] buff = new byte[1024 * 4];

    public ExtractorRendererBuilder(Context context, String userAgent, Uri uri) {
        this.context = context;
        this.userAgent = userAgent;
        this.uri = uri;
        this.mediaFile = new File(context.getCacheDir(), "mySuperVideo.mp4");
    }

    @Override
    public void buildRenderers(DemoPlayer player) {
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        Handler mainHandler = player.getMainHandler();

        // Build the video and audio renderers.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mainHandler, null);
        DataSource dataSource = new DefaultUriDataSource(context,this,userAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri,dataSource,allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE, mainHandler, player, 0);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context,
                sampleSource, MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
                mainHandler, player, 50);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                MediaCodecSelector.DEFAULT, null, true, mainHandler, player,
                AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);
        TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, player,
                mainHandler.getLooper());

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[DemoPlayer.RENDERER_COUNT];
        renderers[DemoPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[DemoPlayer.TYPE_AUDIO] = audioRenderer;
        renderers[DemoPlayer.TYPE_TEXT] = textRenderer;
        player.onRenderers(renderers, bandwidthMeter);
    }

    @Override
    public void cancel() {
        // Do nothing.
    }

    public Boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        }
        NetworkInfo.State network = networkInfo.getState();
        return (network == NetworkInfo.State.CONNECTED || network == NetworkInfo.State.CONNECTING);
    }

    @Override
    public void onTransferStart() {

    }

    @Override
    public void onBytesTransferred(int bytesTransferred) {
        Log.v("bytestransferred", Integer.toString(bytesTransferred));
        final int bytes = bytesTransferred;

        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    OutputStream output = new FileOutputStream(mediaFile);
                    output.write(buff, 0, bytes);
                }catch(IOException exception){
                    exception.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onTransferEnd() {

    }
}
