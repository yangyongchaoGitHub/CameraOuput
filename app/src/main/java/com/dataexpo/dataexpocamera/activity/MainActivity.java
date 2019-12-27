package com.dataexpo.dataexpocamera.activity;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;

import com.dataexpo.dataexpocamera.R;
import com.dataexpo.dataexpocamera.netty.UDPClient;

import java.io.IOException;
import java.util.List;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class MainActivity extends BaseActivity implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    TextureView ttv;
    SurfaceTexture mSurfaceTexture;
    Camera mCamera = null;
    UDPClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        //initSocket();
        client = new UDPClient();
        //new Thread(client).start();
    }

    private void initSocket() {
        //初始化线程组
        NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioDatagramChannel.class);
    }

    private void initView() {
        ttv = findViewById(R.id.main_ttv);
        mSurfaceTexture = ttv.getSurfaceTexture();
        ttv.setSurfaceTextureListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPreview();
        //openCamera();
    }

    private void startPreview() {

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        openCamera(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


    void openCamera(SurfaceTexture surface) {
        int cameraId = 1;
        int previewWidth = 640;
        int previewHeight = 480;
        int videoWidth;
        int videoHeight;

        try {
            if (mCamera == null) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == 1) {
                        cameraId = i;
                    }
                    Log.e(TAG, "initCamera " + cameraInfo.facing);
                }
                mCamera = Camera.open(0);
                Log.e(TAG, "initCamera---open camera");
            }

            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> sizeList = params.getSupportedPreviewSizes(); // 获取所有支持的camera尺寸
            final Camera.Size optionSize = getOptimalPreviewSize(sizeList, previewWidth,
                    previewHeight); // 获取一个最为适配的camera.size

            videoWidth = optionSize.width;
            videoHeight = optionSize.height;

            params.setPreviewSize(videoWidth, videoHeight);
            mCamera.setParameters(params);
            try {
                mCamera.setPreviewTexture(surface);
                mCamera.startPreview();

                mCamera.setPreviewCallback(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解决预览变形问题
     *
     * @param sizes
     * @param w
     * @param h
     * @return
     */
    Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        //Log.i(TAG, "setOptimalPreviewSize w:" + w + " h:" + h);

        final double aspectTolerance = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) {
            return null;
        }
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            //Log.i(TAG, "Camera size.width:" + size.width + " height: " + size.height);
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > aspectTolerance) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        //Log.i(TAG, "end  --:" + optimalSize.width + " height: " + optimalSize.height);
        return optimalSize;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        client.sendTo(data, 1280, 720);
    }
}
