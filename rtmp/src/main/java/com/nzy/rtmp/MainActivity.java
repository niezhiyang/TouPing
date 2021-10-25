package com.nzy.rtmp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    static {
        // 加载so
        System.loadLibrary("native-lib");
        // System.load(全路径加.so)
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stringFromJNI();
    }
    public native String stringFromJNI();
}