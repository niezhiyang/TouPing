package com.nzy.camerato264;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final int REQUEST_CODE_CAMERA = 100;
    private Camera mCamera;
    private Camera.Size mSize;
    private SurfaceHolder mSurfaceHolder;
    private byte[] mBuffer;
    private byte[] nv12;
    private MediaCodec mMediaCodec;
    private byte[] mYuv420;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView surfaceview = findViewById(R.id.surfaceview);
        surfaceview.getHolder().addCallback(this);


    }

    private void initEncode() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //申请成功，可以拍照
            initCamera();
            Toast.makeText(this, "有权限了", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "CAMERA PERMISSION DENIED", Toast.LENGTH_SHORT).show();
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



    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    /**
     * 预览数据
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        encodeFrame(data);
        mCamera.addCallbackBuffer(data);
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
//            dealFrame(outputBuffer, bufferInfo);
            saveFile(outputBuffer,bufferInfo);
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);

        }
        return 0;
    }

    private void saveFile(ByteBuffer buffer,MediaCodec.BufferInfo bufferInfo) {
        byte[] bytes = new byte[bufferInfo.size];
        buffer.get(bytes);
        YuvUtils.writeBytes(bytes,this);
        YuvUtils.writeContent(bytes,this);
    }

    public void start(View view) {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        } else {
            initCamera();
        }
    }
}