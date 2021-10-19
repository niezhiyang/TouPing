package com.nzy.touping;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * Target 28 之后 录屏必须是前台服务
 * FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
 *
 * @author niezhiyang
 * since 10/19/21
 */
public class PushService extends Service {

    /**
     * 录屏的 manger
     */
    private MediaProjectionManager mProjectionManager;
    private PushSocket mSocket;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        int code =  intent.getIntExtra("code",0);
        Intent data = intent.getParcelableExtra("data");

        MediaProjection mediaProjection = mProjectionManager.getMediaProjection(code, data);
        mSocket = new PushSocket();
        mSocket.start(mediaProjection);

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.close();
        }
    }
    private void createNotificationChannel() {
        //获取一个Notification构造器
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
        //点击后跳转的界面，可以设置跳转数据
        Intent nfIntent = new Intent(this, MainActivity.class);

        // 设置PendingIntent
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0))
                // 设置下拉列表中的图标(大图标)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                // 设置下拉列表里的标题
                //.setContentTitle("SMI InstantView")

                // 设置状态栏内的小图标
                .setSmallIcon(R.mipmap.ic_launcher)
                // 设置上下文内容
                .setContentText("投屏中。。。")
                // 设置该通知发生的时间
                .setWhen(System.currentTimeMillis());

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        // 设置该通知发生的时间
        Notification notification = builder.build();
        //设置为默认的声音
        notification.defaults = Notification.DEFAULT_SOUND;
        startForeground(110, notification);

    }
}
