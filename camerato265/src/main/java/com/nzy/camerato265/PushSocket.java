package com.nzy.camerato265;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PushSocket implements Camera.PreviewCallback {
    private static final String TAG = "PushSocket";
    private WebSocket mWebSocket;

    private Camera mCamera;
    private Camera.Size mSize;

    private SurfaceHolder mSurfaceHolder;
    private byte[] mBuffer;
    private byte[] nv12;
    private MediaCodec mMediaCodec;
    private byte[] mYuv420;
    /**
     * 端口号
     */
    private static final int PORT = 13001;

    private Context mContext;

    public PushSocket(Context context, SurfaceHolder surfaceHolder) {
        mContext = context;
        mSurfaceHolder = surfaceHolder;
    }

    public void start() {

        webSocketServer.start();
        initCamera();
    }

    private WebSocketServer webSocketServer = new WebSocketServer(new InetSocketAddress(PORT)) {
        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
            mWebSocket = webSocket;
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            Log.i(TAG, "onClose: 关闭 socket ");
        }

        @Override
        public void onMessage(WebSocket webSocket, String message) {
        }

        @Override
        public void onError(WebSocket conn, Exception e) {
            Log.i(TAG, "onError:  " + e.toString());
        }

        @Override
        public void onStart() {

        }
    };

    /**
     * 发送数据
     *
     * @param bytes
     */
    public void sendData(byte[] bytes) {
        if (mWebSocket != null && mWebSocket.isOpen()) {
            mWebSocket.send(bytes);
        }
    }

    /**
     * 关闭 Socket
     */
    public void close() {
        try {
            mWebSocket.close();
            webSocketServer.stop();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void initCamera() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters parameters = mCamera.getParameters();
        mSize = parameters.getPreviewSize();
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            // 因为摄像头厂家都是横着的 所以把预览方向 调整
            mCamera.setDisplayOrientation(90);
            // 缓冲数据
            mBuffer = new byte[mSize.width * mSize.height * 3 / 2];
            mCamera.addCallbackBuffer(mBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);
//            输出数据怎么办
            mCamera.startPreview();

            // 因为用到了摄像头的宽高，所以写在这里
            initEncode();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initEncode() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            // 因为 摄像头数据 旋转了，所以这里的宽高就会变成高宽
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, mSize.height, mSize.width);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1080 * 1920);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
            // 因为 YUV420 每个像素占用 3/2 个字节
            int bufferLength = mSize.width*mSize.height*3/2;
            nv12 = new byte[bufferLength];
            mYuv420 = new byte[bufferLength];
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public int encodeFrame(byte[] input) {

        // 因为摄像头的数据是 NV21，只有摄像头是这个格式，在硬件编码根本没有这个码
        // 所以要转成YUV420
        nv12 =YuvUtils.nv21toYUV420(input);
        // 因为 摄像头是横着的，所以 数据也是横着的，把数据旋正
        YuvUtils.portraitData2Raw(nv12, mYuv420, mSize.width, mSize.height);

        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(100000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(mYuv420);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, mYuv420.length, System.currentTimeMillis(), 0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
            dealFrame(outputBuffer, bufferInfo);
//            saveFile(outputBuffer,bufferInfo);
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);

        }
        return 0;
    }
    public static final int NAL_I = 19;
    public static final int NAL_VPS = 32;
    private byte[] vps_sps_pps_buf;

    private void dealFrame(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        int offset = 4;
        if (byteBuffer.get(2) == 0x01) {
            offset = 3;
        }
        int type = (byteBuffer.get(offset) & 0x7E) >> 1;
        // vps_sps_pps 帧记录下来
        if (type == NAL_VPS) {
            vps_sps_pps_buf = new byte[bufferInfo.size];
            byteBuffer.get(vps_sps_pps_buf);
        } else if (type == NAL_I) {
            // I 帧 ，把 vps_sps_pps 帧塞到 I帧之前一起发出去
            final byte[] bytes = new byte[bufferInfo.size];
            byteBuffer.get(bytes);

            byte[] newBuf = new byte[vps_sps_pps_buf.length + bytes.length];
            System.arraycopy(vps_sps_pps_buf, 0, newBuf, 0, vps_sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, vps_sps_pps_buf.length, bytes.length);
            sendData(newBuf);
            Log.v(TAG, "I帧 视频数据  " + Arrays.toString(bytes));
        } else {
            // B 帧 P 帧 直接发送
            final byte[] bytes = new byte[bufferInfo.size];
            byteBuffer.get(bytes);
            sendData(bytes);

        }

    }
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        encodeFrame(data);
        mCamera.addCallbackBuffer(data);
    }
    private void saveFile(ByteBuffer buffer,MediaCodec.BufferInfo bufferInfo) {
        byte[] bytes = new byte[bufferInfo.size];
        buffer.get(bytes);
        YuvUtils.writeBytes(bytes,mContext);
        YuvUtils.writeContent(bytes,mContext);
    }
}
