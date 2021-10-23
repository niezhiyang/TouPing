package com.nzy.camera2andcamerax;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

/**
 * @author niezhiyang
 * since 10/23/21
 */
public class Camera2Helper {
    private static final String TAG = "Camera2Helper";
    private CameraDevice mCameraDevice;
    private Context mContext;
    private Size mPreviewSize;
    private Point previewViewSize;
    private ImageReader mImageReader;

    private Handler mBackGroundHandler;
    private final HandlerThread mHandlerThread;

    private TextureView mTextureView;
    private CaptureRequest.Builder mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private Camera2Listener mCamera2Listener;

    public Camera2Helper(Context context,Camera2Listener camera2Listener) {
        mContext = context;
        mCamera2Listener= camera2Listener;
        mHandlerThread = new HandlerThread("Camera");
        mHandlerThread.start();
        mBackGroundHandler = new Handler(mHandlerThread.getLooper());
    }

    public synchronized void start(TextureView textureView) throws CameraAccessException {
        mTextureView = textureView;
        // 通过 CameraService
        // 摄像头管理类
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

        // 获取 CameraId的列表
        String[] cameraIdList = cameraManager.getCameraIdList();
        Log.e(TAG, Arrays.toString(cameraIdList));
        // todo 需要判断那个id 是前置 或者 后置，一般是 0 是前置，先写死

        // 摄像头的配置信息 0 是前置摄像头
        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics("0");

        // 获取支持 那些格式 ，获取到预览尺寸 和 textureView，肯定是不一样的，所以寻找一个最合适的尺寸
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        mPreviewSize = getBestSupportedSize(new ArrayList<Size>(Arrays.asList(map.getOutputSizes(SurfaceTexture.class))));

        //第四个参数maxImages 表示 几个数据需要记录，比如 渲染surface 一个 ，直播推流一个（保存文件）
        // 第三个参数 在Camera以前的api中是只支持NV21，这里传入 YUV-420,是系统底层帮我们做的，并不是摄像头支持 YUV420
        // 记住 摄像头 只能是nv21格式
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
        // 第二个 表示 输出的数据在那个线程
        mImageReader.setOnImageAvailableListener(new OnImageAvailableListener(), mBackGroundHandler);

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, "请打开摄像头", Toast.LENGTH_SHORT).show();
            return;
        }
        cameraManager.openCamera("0", mStateCallback, mBackGroundHandler);
    }


    private  class OnImageAvailableListener implements ImageReader.OnImageAvailableListener {

        private byte[] y;
        private byte[] u;
        private byte[] v;
        @Override
        public void onImageAvailable(ImageReader reader) {
            // 可用的话，会回调给我们，就想 Camera1 中 的 onPreviewFrame(),只不过哦以前是数组，现在是reader

            // 我们需要把数据取出来 close掉，不取出来，就会只有一帧，别人不会再给了
            Image image = reader.acquireNextImage();


            // 把 image 转化成 yuv数据，yuv 再转化成h264 ，发送出去
            Image.Plane[] planes = image.getPlanes();
            if(y==null){
                // limit 是所有的大小，position是起始大小
                y = new byte[planes[0].getBuffer().limit()-planes[0].getBuffer().position()];
                u = new byte[planes[1].getBuffer().limit()-planes[1].getBuffer().position()];
                v = new byte[planes[2].getBuffer().limit()-planes[2].getBuffer().position()];
            }
            if(image.getPlanes()[0].getBuffer().remaining() == y.length){
                // 添加到yuv中
                planes[0].getBuffer().get(y);
                planes[1].getBuffer().get(u);
                planes[2].getBuffer().get(v);

                // 转成 yuv420
               if(mCamera2Listener!=null){
                   mCamera2Listener.onPreview(y,u,y,mPreviewSize,planes[0].getRowStride());
               }
            }
            image.close();



        }
    }

    /**
     * 监听是否打开相机成功
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // 打开成功
            mCameraDevice = camera;

            // 开始建立会话
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            // 弄成空
            mCameraDevice = null;

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    private void createCameraPreviewSession() {

        try {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            // 设置预览宽高
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            // 创建一个 SurfaceView
            Surface surface = new Surface(surfaceTexture);
            // 创建一个预览请求
            mCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            // 渲染到 Surface，surface 又跟 mTextureView进行关联，所以直接是到了 TextureView渲染了
            mCaptureRequest.addTarget(surface);

            //todo  保存摄像头数据，h264 ,获取数据流 给 mImageReader 在 OnImageAvailableListener 中拿到
            mCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 给了 mImageReader，在 OnImageAvailableListener 回调中
            mCaptureRequest.addTarget(mImageReader.getSurface());

            // 上面都是请求，这里是建立会话
            List<Surface> surfaces = Arrays.asList(surface, mImageReader.getSurface());
            mCameraDevice.createCaptureSession(surfaces,mSessionStateCallback,mBackGroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }


    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            // 建立会话成功
            if(mCameraDevice!=null){
                mCameraCaptureSession = session;
                try {
                    // 持续 请求里面的 请求
                    mCameraCaptureSession.setRepeatingRequest(mCaptureRequest.build(),new CameraCaptureSession.CaptureCallback(){},mBackGroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };
    private Size getBestSupportedSize(List<Size> sizes) {
        Point maxPreviewSize = new Point(1920, 1080);
        Point minPreviewSize = new Point(1280, 720);
        Size defaultSize = sizes.get(0);
        Size[] tempSizes = sizes.toArray(new Size[0]);
        Arrays.sort(tempSizes, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                if (o1.getWidth() > o2.getWidth()) {
                    return -1;
                } else if (o1.getWidth() == o2.getWidth()) {
                    return o1.getHeight() > o2.getHeight() ? -1 : 1;
                } else {
                    return 1;
                }
            }
        });
        sizes = new ArrayList<>(Arrays.asList(tempSizes));
        for (int i = sizes.size() - 1; i >= 0; i--) {
            if (maxPreviewSize != null) {
                if (sizes.get(i).getWidth() > maxPreviewSize.x || sizes.get(i).getHeight() > maxPreviewSize.y) {
                    sizes.remove(i);
                    continue;
                }
            }
            if (minPreviewSize != null) {
                if (sizes.get(i).getWidth() < minPreviewSize.x || sizes.get(i).getHeight() < minPreviewSize.y) {
                    sizes.remove(i);
                }
            }
        }
        if (sizes.size() == 0) {
            return defaultSize;
        }
        Size bestSize = sizes.get(0);
        float previewViewRatio;
        if (previewViewSize != null) {
            previewViewRatio = (float) previewViewSize.x / (float) previewViewSize.y;
        } else {
            previewViewRatio = (float) bestSize.getWidth() / (float) bestSize.getHeight();
        }

        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio;
        }

        for (Size s : sizes) {
            if (Math.abs((s.getHeight() / (float) s.getWidth()) - previewViewRatio) < Math.abs(bestSize.getHeight() / (float) bestSize.getWidth() - previewViewRatio)) {
                bestSize = s;
            }
        }
        return bestSize;
    }

    public void openCamera() {

    }


    public interface Camera2Listener {
        /**
         * 预览数据回调
         * @param y 预览数据，Y分量
         * @param u 预览数据，U分量
         * @param v 预览数据，V分量
         * @param previewSize  预览尺寸
         * @param stride    步长
         */
        void onPreview(byte[] y, byte[] u, byte[] v, Size previewSize, int stride);
    }
}
