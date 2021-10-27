package com.nzy.rtmp;

import android.media.projection.MediaProjection;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 用来和 rtmp 层做交流
 * 主要是来链接 以及 发送数据
 */
public class ScreenLive extends Thread {
    private static final String TAG = "ScreenLive";

    static {
        System.loadLibrary("native-lib");
    }
    private boolean isLiving;
    // 一个队列，生产者模式
    // AudioCodec 和 VideoCodec 添加
    // 这里用来取 给到 rtmp 中
    private LinkedBlockingQueue<RTMPPackage> queue = new LinkedBlockingQueue<>();
    private String url;
    private MediaProjection mediaProjection;
    public void startLive(String url, MediaProjection mediaProjection) {
        this.url = url;
        this.mediaProjection = mediaProjection;
        LiveTaskManager.getInstance().execute(this);
    }

    /**
     *  把 RTMPPackage 包 添加到队列
     * @param rtmpPackage
     */
    public void addPackage(RTMPPackage rtmpPackage) {
        if (!isLiving) {
            return;
        }
        queue.add(rtmpPackage);
    }

    @Override
    public void run() {
        //1推送到
        if (!connect(url)) {
            Log.i(TAG, "run: ----------->推送失败");
            return;
        }

        VideoCodec videoCodec = new VideoCodec(this);
        videoCodec.startLive(mediaProjection);
        AudioCodec audioCodec = new AudioCodec(this);
        audioCodec.startLive();
        isLiving = true;
        while (isLiving) {
            RTMPPackage rtmpPackage = null;
            try {
                rtmpPackage = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "取出数据" );
            if (rtmpPackage.getBuffer() != null && rtmpPackage.getBuffer().length != 0) {
                Log.i(TAG, "run: ----------->推送 "+ rtmpPackage.getBuffer().length);
                sendData(rtmpPackage.getBuffer(), rtmpPackage.getBuffer()
                        .length, rtmpPackage.getTms(), rtmpPackage.getType());
            }
        }
    }
    private native boolean sendData(byte[] data, int len, long tms, int type);

    private native boolean connect(String url);
}
