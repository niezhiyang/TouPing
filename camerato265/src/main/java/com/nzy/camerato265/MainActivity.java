package com.nzy.camerato265;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

// 默认把
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final int REQUEST_CODE_CAMERA = 100;
    private SurfaceHolder mSurfaceHolder;
    private PushSocket mPushSocket;
    private static final String TAG = "MediaCodecInfo";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView surfaceview = findViewById(R.id.surfaceview);
        surfaceview.getHolder().addCallback(this);
        int codecCount = MediaCodecList.getCodecCount();
        MediaCodecInfo[] codecInfos = new MediaCodecList(1).getCodecInfos();

        int length = codecInfos.length;
        // 可以同时解码多个视频，但是肯定不是这么多的
        Log.e(TAG,codecCount+"---"+codecCount);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //申请成功，可以拍照
           initSocket();

            Toast.makeText(this, "有权限了", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "CAMERA PERMISSION DENIED", Toast.LENGTH_SHORT).show();
        }
    }

    private void initSocket() {
        mPushSocket = new PushSocket(this,mSurfaceHolder);
        mPushSocket.start();
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





    public void start(View view) {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        } else {
            initSocket();
        }
    }
}