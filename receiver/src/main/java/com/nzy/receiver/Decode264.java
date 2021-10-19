package com.nzy.receiver;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author niezhiyang
 * since 10/19/21
 */
public class Decode264 implements ReceiveSocket.SocketCallback {
    private static final String TAG = "Decode";
    private Surface mSurface;
    MediaCodec mMediaCodec;
    public Decode264(Surface surface) {
        mSurface = surface;
        initCodec();
    }

    private void initCodec() {
        try {
            // 把 h264 解码成 yuv视频
            ////////////////////////////////更改MIMETYPE_VIDEO_AVC//////////////////////////////////////
            mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            final MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 720, 1280);
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
        Log.i(TAG, Arrays.toString(data));
        int index = mMediaCodec.dequeueInputBuffer(100000);
        if (index >= 0) {
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(data, 0, data.length);
            mMediaCodec.queueInputBuffer(index,
                    0, data.length, System.currentTimeMillis(), 0);
        }
        //  获取数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 100000);

        while (outputBufferIndex > 0) {
            // true 就是显示在surface上
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
}
