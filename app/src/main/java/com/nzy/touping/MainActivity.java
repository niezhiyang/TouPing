package com.nzy.touping;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * push 端
 */
public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 1;

    /**
     * 录屏的 manger
     */
    private MediaProjectionManager mProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    public void start(View view) {
        // 请求录屏权限
        Intent intent = mProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Intent service = new Intent(this, PushService.class);
            service.putExtra("code", resultCode);
            service.putExtra("data", data);
            startForegroundService(service);

        }else{
            Toast.makeText(this,"请打开录屏权限",Toast.LENGTH_SHORT).show();
        }
    }


}