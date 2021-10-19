package com.nzy.receiver;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements ReceiveSocket.SocketCallback {
    private Surface mSurface;
    MediaCodec mMediaCodec;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                mSurface = holder.getSurface();
                initSocket();
                initDecoder();
            }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });

    }
    private void initSocket() {
        ReceiveSocket screenLive = new ReceiveSocket(this,this);
        screenLive.start();
    }

    public void initDecoder() {
        try {
            // 把 h265 解码成 yuv视频
            mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            final MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 720, 1280);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 720 * 1280);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            // 解码之后直接显示在 Surface 上
            mMediaCodec.configure(format,
                    mSurface,
                    null, 0);
            mMediaCodec.start();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void callBack(byte[] data) {
        int index = mMediaCodec.dequeueInputBuffer(100000);
        if (index >= 0) {
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(data, 0, data.length);
            mMediaCodec.queueInputBuffer(index,
                    0, data.length, System.currentTimeMillis(), 0);
        }
        //        获取数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 100000);

        while (outputBufferIndex > 0) {
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
}