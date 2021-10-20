package com.nzy.camerato264;

import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class YuvUtils {
    private static final String TAG = "YuvUtils";
    static  byte[] yuv420;

    public static byte[] nv21toYUV420(byte[] nv21) {
        int  size = nv21.length;
         yuv420 = new byte[size];
        int len = size * 2 / 3;
        System.arraycopy(nv21, 0, yuv420, 0, len);
        int i = len;
        while(i < size - 1){
            yuv420[i] = nv21[i + 1];
            yuv420[i + 1] = nv21[i];
            i += 2;
        }
        return yuv420;
    }

    public static void portraitData2Raw(byte[] data,byte[] output,int width,int height) {
        int y_len = width * height;
        // uv数据高为y数据高的一半
        int uvHeight = height >> 1;
        int k = 0;
        for (int j = 0; j < width; j++) {
            for (int i = height - 1; i >= 0; i--) {
                output[k++] = data[width * i + j];
            }
        }
        for (int j = 0; j < width; j += 2) {
            for (int i = uvHeight - 1; i >= 0; i--) {
                output[k++] = data[y_len + width * i + j];
                output[k++] = data[y_len + width * i + j + 1];
            }
        }
    }
    public  static  void writeBytes(byte[] array, Context context) {
        FileOutputStream writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileOutputStream(context.getFilesDir() + "/codec.h265", true);
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

    public  static String writeContent(byte[] array, Context context) {
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
            writer = new FileWriter(context.getFilesDir() + "/codecH265.txt", true);
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


}
