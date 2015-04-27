package com.android.slyce.handler;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.MoodStocksProgress;
import com.android.slyce.models.SlyceProgress;

import org.json.JSONArray;

/**
 * Created by davidsvilem on 4/21/15.
 */
public class CameraSynchronizer extends Handler {

    private OnSlyceCameraListener mCameraListener;

    public CameraSynchronizer(OnSlyceCameraListener listener){
        super(Looper.getMainLooper());

        mCameraListener = listener;
    }

    public void onBarcodeRecognition(String barcode){
        obtainMessage(1, barcode).sendToTarget();
    }

    public void on2DRecognition(String irId, String productInfo){
        MoodStocksProgress moodStocksProgress = new MoodStocksProgress(irId, productInfo);
        obtainMessage(2, moodStocksProgress).sendToTarget();
    }

    public void on2DExtendedRecognition(JSONArray products){
        obtainMessage(3, products).sendToTarget();
    }

    public void onError(String message){
        obtainMessage(4, message).sendToTarget();
    }

    public void onSnap(Bitmap bitmap){
        obtainMessage(5, bitmap).sendToTarget();
    }

    public void onTap(){
        obtainMessage(6).sendToTarget();
    }

    public void onSlyceProgress(long progress, String message, String token){
        SlyceProgress slyceProgress = new SlyceProgress(progress, message, token);
        obtainMessage(7, slyceProgress).sendToTarget();
    }

    public void on3DRecognition(JSONArray products){
        obtainMessage(8, products).sendToTarget();
    }

    public void onStageLevelFinish(OnSlyceRequestListener.StageMessage message){
        obtainMessage(9, message).sendToTarget();
    }

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what){

            case 1:

                mCameraListener.onBarcodeRecognition((String) msg.obj);

                break;

            case 2:

                MoodStocksProgress moodStocksProgress = (MoodStocksProgress) msg.obj;
                mCameraListener.on2DRecognition(moodStocksProgress.irId, moodStocksProgress.productInfo);

                break;

            case 3:

                mCameraListener.on2DExtendedRecognition((JSONArray) msg.obj);

                break;

            case 4:

                mCameraListener.onError((String) msg.obj);

                break;

            case 5:

                mCameraListener.onSnap((Bitmap) msg.obj);

                break;

            case 6:

                mCameraListener.onTap();

            case 7:

                SlyceProgress slyceProgress = (SlyceProgress) msg.obj;
                mCameraListener.onSlyceProgress(slyceProgress.progress, slyceProgress.message, slyceProgress.token);

                break;

            case 8:

                mCameraListener.on3DRecognition((JSONArray) msg.obj);

                break;

            case 9:

                mCameraListener.onStageLevelFinish((OnSlyceRequestListener.StageMessage) msg.obj);

                break;
        }
    }
}
