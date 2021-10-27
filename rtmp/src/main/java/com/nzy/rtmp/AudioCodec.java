package com.nzy.rtmp;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.nio.ByteBuffer;

public class AudioCodec extends Thread {
    private static final String TAG = "AudioCodec";
    private MediaCodec mediaCodec;

    private int minBufferSize;
    private boolean isRecoding;
    private AudioRecord audioRecord;
    private long startTime;
//传输层
    private  ScreenLive screenLive;
    public AudioCodec(ScreenLive screenLive) {
        this.screenLive = screenLive;
    }
    public void startLive() {

        MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1);
//录音质量
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel
                .AACObjectLC);
//一秒的码率 aac
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64_000);
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
//录音工具类  采样位数 通道数   采样评率   固定了   设备没关系  录音 数据一样的
            minBufferSize = AudioRecord.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, 44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, minBufferSize);

        } catch (Exception e) {
        }
        LiveTaskManager.getInstance().execute(this);
    }
    @Override
    public void run() {
//        编码耗时  api   傻瓜式 调用
        isRecoding = true;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        没有开始编码音频    空   数据头
        RTMPPackage rtmpPackage = new RTMPPackage();
//        发音频  另外一段准备
        byte[] audioDecoderSpecificInfo = {0x12, 0x08};
        rtmpPackage.setBuffer(audioDecoderSpecificInfo);
        rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_AUDIO_HEAD);
        screenLive.addPackage(rtmpPackage);

        //        开始录音
        Log.i(TAG, "开始录音  minBufferSize： "+minBufferSize);
        audioRecord.startRecording();
//        容器 固定
        byte[] buffer = new byte[minBufferSize];
        while (isRecoding) {
//            麦克风的数据读取出来   pcm   buffer  aac
            int len =audioRecord.read(buffer, 0, buffer.length);
//pcm 数据编码

            if (len <= 0) {
                continue;
            }
            //立即得到有效输入缓冲区
            int index = mediaCodec.dequeueInputBuffer(0);
            if (index >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
                inputBuffer.clear();
                inputBuffer.put(buffer, 0, len);
                //填充数据后再加入队列
                mediaCodec.queueInputBuffer(index, 0, len,
                        System.nanoTime() / 1000, 0);
            }
            index = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
//            没有疑问

            while (index >= 0 && isRecoding) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index);
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData); //编码好的数据
                if (startTime == 0) {
                    startTime = bufferInfo.presentationTimeUs / 1000;
                }
                rtmpPackage = new RTMPPackage();
                rtmpPackage.setBuffer(outData);
//                native    音频数据
                rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_AUDIO_DATA);
//设置时间戳
                long tms = (bufferInfo.presentationTimeUs / 1000) - startTime;
                rtmpPackage.setTms(tms);
                screenLive.addPackage(rtmpPackage);
                mediaCodec.releaseOutputBuffer(index, false);
                index = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        }
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        startTime = 0;
        isRecoding = false;

    }
}
