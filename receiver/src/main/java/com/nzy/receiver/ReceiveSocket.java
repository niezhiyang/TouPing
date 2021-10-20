package com.nzy.receiver;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class ReceiveSocket {
    private static final String TAG = "ReceiveSocket";
    private final SocketCallback mSocketCallback;
    MyWebSocketClient myWebSocketClient;
    private static final int PORT = 13001;
    //todo 这里填写自己发送端的wifiIp
//    private static final String IP = "172.16.149.72";
    private static final String IP = "";
    private final Context mContext;

    public ReceiveSocket(Context context, SocketCallback socketCallback) {
        mContext = context;
        mSocketCallback = socketCallback;
    }

    public void start() {
        if (IP.isEmpty()) {
            Toast.makeText(mContext, "请填写发送端的wifi的IP", Toast.LENGTH_SHORT).show();
            throw new RuntimeException("请填写发送端的wifi的IP");
        }
        try {
            URI url = new URI("ws://"+IP+":" + PORT);
            myWebSocketClient = new MyWebSocketClient(url);
            myWebSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class MyWebSocketClient extends WebSocketClient {

        public MyWebSocketClient(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            Log.i(TAG, "打开 socket  onOpen: ");
        }

        @Override
        public void onMessage(String s) {
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            Log.i(TAG, "消息长度  : " + bytes.remaining());
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            mSocketCallback.callBack(buf);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.i(TAG, "onClose: " + code + "----" + reason + "----" + remote);
        }

        @Override
        public void onError(Exception e) {
            Log.i(TAG, "onError: " + e);
        }
    }

    public interface SocketCallback {
        /**
         * 返回给SurfaceView的数据
         * @param data
         */
        void callBack(byte[] data);
    }
}
