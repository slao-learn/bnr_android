package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by slao on 1/24/16.
 */
public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_PRELOAD = 1;
    private static final int CACHE_SIZE = 1000;

    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
    private LruCache<String, Bitmap> mBitmapCache = new LruCache<>(CACHE_SIZE);

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownload(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                } else if (msg.what == MESSAGE_PRELOAD) {
                    String url = (String) msg.obj;
                    Log.i(TAG, "Preloading URL: " + url);
                    retrieveBitmap(url);
                }
            }
        };
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    public void preloadBitmaps(String[] urls, int length) {
        for (int i = 0; i < length; i++) {
            String url = urls[i];
            if (url != null) {
                mRequestHandler.obtainMessage(MESSAGE_PRELOAD, url).sendToTarget();
            }
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    private void handleRequest(final T target) {
        final String url = mRequestMap.get(target);
        if (url == null) {
            return;
        }
        final Bitmap bitmap = retrieveBitmap(url);
        mResponseHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRequestMap.get(target) != url) {
                    return;
                }

                mRequestMap.remove(target);
                mThumbnailDownloadListener.onThumbnailDownload(target, bitmap);
            }
        });
    }

    private Bitmap retrieveBitmap(String url) {
        Bitmap bitmap = mBitmapCache.get(url);
        if (bitmap == null) {
            try {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                Log.i(TAG, "Bitmap created");
                mBitmapCache.put(url, bitmap);
            } catch (IOException ioe) {
                Log.e(TAG, "Error downloading image", ioe);
            }
        } else {
            Log.i(TAG, "Bitmap from cache");
        }
        return bitmap;
    }
}
