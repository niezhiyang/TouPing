package com.nzy.camera2andcamerax;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/**
 * 因为发展太快，以前就一个camera，现在有好几个摄像头，所以升级了 camera2 或者 cameraX
 * android 11 一下 不支持 开个前置 摄像头 ，开个后摄像头，
 * 但是 微信一个用前置 ，支付宝用后置
 * <p>
 * <p>
 * SurfaceView 不支持动画，
 * TextureView 支持动画，
 * 两个都是通过surface 来渲染的
 */
public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Camera2Helper.Camera2Listener {
    private static final int REQUEST_CODE_CAMERA = 100;
    private static final String TAG = "MainActivity";
    TextureView textureview;
    private MediaCodec mediaCodec;
    private PushSocket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureview = findViewById(R.id.textureview);
        textureview.setSurfaceTextureListener(this);
        mSocket = new PushSocket();
        mSocket.start();
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {

    }

    private void initCamera() {
        try {
            new Camera2Helper(this, this).start(textureview);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

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

    public void start(View view) {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        } else {
            // 打开摄像头
            initCamera();
        }
    }

    private byte[] nv21;
    byte[] nv21_rotated;
    byte[] nv12;

    /**
     * 回调过来的yuv
     *
     * @param y           预览数据，Y分量
     * @param u           预览数据，U分量
     * @param v           预览数据，V分量
     * @param previewSize 预览尺寸
     * @param stride      步长
     */
    @Override
    public void onPreview(byte[] y, byte[] u, byte[] v, Size previewSize, int stride) {
        // 无论用 Camrae 1 还是 2 都是横着的，所以要旋转

        if (nv21 == null) {
            // 每一行的数据*高度 总的像素值，yuv420 一个像素占用 3/2 个字节
            nv21 = new byte[stride * previewSize.getHeight() * 3 / 2];

            nv21_rotated = new byte[stride * previewSize.getHeight() * 3 / 2];
        }
        if(mediaCodec==null){
            initCodec(previewSize);
        }
        // 转化成相机原始数据
        ImageUtil.yuvToNv21(y,u,v,nv21,stride,previewSize.getHeight());
        // 旋转，YUV420 ，就是 nv21
        ImageUtil.nv21_rotate_to_90(nv21,nv21_rotated,stride,previewSize.getHeight());

        byte[] temp = ImageUtil.nv21toNV12(nv21_rotated, nv12);

        //输出成H264的码流
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inIndex = mediaCodec.dequeueInputBuffer(100000);
        if (inIndex >= 0) {
            ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inIndex);
            byteBuffer.clear();
            byteBuffer.put(temp, 0, temp.length);
            mediaCodec.queueInputBuffer(inIndex, 0, temp.length,
                    0, 0);
        }
        int outIndex = mediaCodec.dequeueOutputBuffer(info, 100000);
        if (outIndex >= 0) {
            ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
//            byte[] ba = new byte[byteBuffer.remaining()];
//            byteBuffer.get(ba);
//            Log.e("rtmp", "ba = " + ba.length + "");
            // 写在文件中 方便查看
//            writeContent(ba);
//            writeBytes(ba);
            // 发送出去
            dealFrame(byteBuffer,info);
            mediaCodec.releaseOutputBuffer(outIndex, false);
        }
    }
    private void initCodec(Size size) {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");

            final MediaFormat format = MediaFormat.createVideoFormat("video/avc",
                    size.getHeight(), size.getWidth());
            //设置帧率  手动触发一个I帧
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            // 一秒 15帧
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 4000_000);
            //2s一个I帧
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final int NAL_I = 5;
    public static final int NAL_SPS = 7;
    private byte[] sps_pps_buf;
    private void dealFrame(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        int offset = 4;
        if (byteBuffer.get(2) == 0x01) {
            offset = 3;
        }

        int type = byteBuffer.get(offset) & 0x1f;
        /////////////////////////////////// 如果是H265 这里type 要换 //////////////////////////////////////////////
//        int type = (byteBuffer.get(offset) & 0x7E) >> 1;
        // sps_pps_buf 帧记录下来
        if (type == NAL_SPS) {
            sps_pps_buf = new byte[bufferInfo.size];
            byteBuffer.get(sps_pps_buf);
        } else if (type == NAL_I) {
            // I 帧 ，把 vps_sps_pps 帧塞到 I帧之前一起发出去
            final byte[] bytes = new byte[bufferInfo.size];
            byteBuffer.get(bytes);

            byte[] newBuf = new byte[sps_pps_buf.length + bytes.length];
            System.arraycopy(sps_pps_buf, 0, newBuf, 0, sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, sps_pps_buf.length, bytes.length);
            mSocket.sendData(newBuf);
            Log.v(TAG, "I帧 视频数据  " + Arrays.toString(bytes));
        } else {
            // B 帧 P 帧 直接发送
            final byte[] bytes = new byte[bufferInfo.size];
            byteBuffer.get(bytes);
            mSocket.sendData(bytes);

        }

    }

    public String writeContent(byte[] array) {
        char[] HEX_CHAR_TABLE = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        Log.i(TAG, "writeContent: " + sb.toString());
        FileWriter writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileWriter(getFilesDir() + "/codec.txt", true);
            writer.write(sb.toString());
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public void writeBytes(byte[] array) {
        FileOutputStream writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileOutputStream(getFilesDir() + "/codec.h264", true);
            writer.write(array);
            writer.write('\n');


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}