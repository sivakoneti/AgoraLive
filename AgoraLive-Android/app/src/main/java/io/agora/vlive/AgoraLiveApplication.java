package io.agora.vlive;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.faceunity.FURenderer;

import io.agora.capture.video.camera.CameraManager;
import io.agora.framework.PreprocessorFaceUnity;
import io.agora.rtc.RtcEngine;
import io.agora.rtm.RtmClient;
import io.agora.vlive.agora.AgoraEngine;
import io.agora.vlive.agora.rtc.RtcEventHandler;
import io.agora.vlive.proxy.ClientProxy;
import io.agora.vlive.utils.Global;

public class AgoraLiveApplication extends Application {
    private static final String TAG = AgoraLiveApplication.class.getSimpleName();

    private SharedPreferences mPref;
    private Config mConfig;
    private AgoraEngine mAgoraEngine;
    private CameraManager mCameraVideoManager;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();
        mPref = getSharedPreferences(Global.Constants.SF_NAME, Context.MODE_PRIVATE);
        mConfig = new Config(this);
        initVideoGlobally();
    }

    public Config config() {
        return mConfig;
    }

    public SharedPreferences preferences() {
        return mPref;
    }

    public void initEngine(String appId) {
        mAgoraEngine = new AgoraEngine(this, appId);
    }

    public RtcEngine rtcEngine() {
        return mAgoraEngine != null ? mAgoraEngine.rtcEngine() : null;
    }

    public RtmClient rtmClient() {
        return mAgoraEngine != null ? mAgoraEngine.rtmClient() : null;
    }

    public ClientProxy proxy() {
        return ClientProxy.instance();
    }

    public void registerRtcHandler(RtcEventHandler handler) {
        mAgoraEngine.registerRtcHandler(handler);
    }

    public void removeRtcHandler(RtcEventHandler handler) {
        mAgoraEngine.removeRtcHandler(handler);
    }

    public CameraManager cameraVideoManager() {
        return mCameraVideoManager;
    }

    private void initVideoGlobally() {
        new Thread(() -> {
            FURenderer.initFURenderer(getApplicationContext());
            PreprocessorFaceUnity preprocessor =
                    new PreprocessorFaceUnity(this);
            mCameraVideoManager = new CameraManager(
                    this, preprocessor);
            mCameraVideoManager.setCameraStateListener(preprocessor);
            Log.d(TAG, "CameraVideoManager initialized");
        }).start();
    }

    @Override
    public void onTerminate() {
        Log.i(TAG, "onCreate");
        super.onTerminate();
        mAgoraEngine.release();
    }
}