package com.nzy.camera2andcamerax;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class PushSocket {
    private static final String TAG = "PushSocket";
    private WebSocket mWebSocket;

    /**
     * 端口号
     */
    private static final int PORT = 13001;


    public void start() {
        webSocketServer.start();
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
}
