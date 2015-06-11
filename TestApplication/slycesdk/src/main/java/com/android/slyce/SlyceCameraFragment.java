package com.android.slyce;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.slyce.enums.SlyceRequestStage;
import com.android.slyce.fragments.ImageProcessDialogFragment;
import com.android.slyce.fragments.ImageProcessDialogFragment.OnImageProcessDialogFragmentListener;
import com.android.slyce.fragments.ScanningTipsDialogFragment;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.utils.Buzzer;
import com.android.slyce.utils.SlyceLog;
import com.android.slyce.utils.Utils;
import com.android.slycesdk.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.android.slyce.listeners.OnSlyceCameraFragmentListener} interface
 * to handle interaction events.
 * Use the {@link SlyceCameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * SlyceCameraFragment provides an integrated XML layout with all Slyce SDK functionalities.
 */
public class SlyceCameraFragment extends Fragment implements OnClickListener{

    private static final String TAG = SlyceCameraFragment.class.getSimpleName();

    private static final String FRAGMENT_TAG = "ImageProcessDialogFragment";

    // the fragment initialization parameters
    private static final String ARG_OPTION_JSON = "arg_option_json";
    private static final String ARG_CONTINUOUS_RECOGNITION = "arg_continuous_recognition";
    private static final String ARG_SHOULD_PAUSE_SCANNER = "arg_should_pause_scanner";

    private static final int RESULT_LOAD_IMG = 1;

    /* Options Json from hosting application */
    private JSONObject mOptionsJson;

    private boolean isAttached;
    private boolean mContinuousRecognition;
    private boolean mShouldPauseScanner;

    /* Listeners */
    private com.android.slyce.listeners.OnSlyceCameraFragmentListener mListener;

    /* Camera surface view */
    private SurfaceView mPreview;

    /* Views */
    private ImageButton mCloseButton;
    private ImageButton mScanTipsButton;
    private ImageButton mGalleryButton;
    private CheckBox mFlashButton;
    private ImageButton mSnapButton;
    private ImageView mOnTapView;
    private RelativeLayout mDialogLayout;

    /* Slyce Camera object */
    private SlyceCamera mSlyceCamera;

    /*  */
    private ImageProcessDialogFragment mImageProcessDialogFragment;

    private Slyce mSlyce;

    // PUBLIC METHODS
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param options use this JSONObject to pass properties to Slyce servers. Can be null.
     * @param continuousRecognition boolean use this in order to stop the automatic scanner
     * @param shouldPauseScanner boolean use this to resume/pause the automatic scanner after detection
     *
     * @return A new instance of fragment SlyceCameraFragment.
     */
    public static SlyceCameraFragment newInstance(JSONObject options, boolean continuousRecognition, boolean shouldPauseScanner) {
        SlyceCameraFragment fragment = new SlyceCameraFragment();
        Bundle args = new Bundle();

        if(options != null){
            args.putString(ARG_OPTION_JSON, options.toString());
        }

        args.putBoolean(ARG_CONTINUOUS_RECOGNITION, continuousRecognition);
        args.putBoolean(ARG_SHOULD_PAUSE_SCANNER, shouldPauseScanner);

        fragment.setArguments(args);
        return fragment;
    }

    public SlyceCameraFragment() {
    }
    // PUBLIC METHODS END

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentArguments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_slyce, container, false);

        // Initialize views
        initViews(root);

        mSlyce = Slyce.getInstance(getActivity());

        // Create SlyceCamera object
        createSlyceCamera();

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mSlyceCamera != null){
            mSlyceCamera.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mSlyceCamera != null){
            mSlyceCamera.stop();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        isAttached = true;
        try {
            mListener = (com.android.slyce.listeners.OnSlyceCameraFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSlyceCameraFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isAttached = false;
        mListener = null;
    }

    // PRIVATE METHODS
    private void createSlyceCamera(){
        mSlyceCamera = new SlyceCamera(getActivity(), mSlyce, mPreview, mOptionsJson, new SlyceCameraListener());
        mSlyceCamera.setContinuousRecognition(mContinuousRecognition);
        mSlyceCamera.shouldPauseScanner(mShouldPauseScanner);
    }

    private void initViews(View view){
        mPreview = (SurfaceView) view.findViewById(R.id.preview);
        mCloseButton = (ImageButton) view.findViewById(R.id.close_button);
        mScanTipsButton = (ImageButton) view.findViewById(R.id.scan_tips_button);
        mGalleryButton = (ImageButton) view.findViewById(R.id.gallery_button);
        mFlashButton = (CheckBox) view.findViewById(R.id.flash_button);
        mSnapButton = (ImageButton) view.findViewById(R.id.snap_button);
        mOnTapView = (ImageView) view.findViewById(R.id.on_tap_view);
        mDialogLayout = (RelativeLayout) view.findViewById(R.id.dialog_layout);

        mCloseButton.setOnClickListener(this);
        mScanTipsButton.setOnClickListener(this);
        mGalleryButton.setOnClickListener(this);
        mFlashButton.setOnClickListener(this);
        mSnapButton.setOnClickListener(this);
    }

    private void getFragmentArguments(){
        if (getArguments() != null) {

            // Parameter 1. Set Options Json
            String options = getArguments().getString(ARG_OPTION_JSON);
            if(!TextUtils.isEmpty(options)){
                try {
                    mOptionsJson = new JSONObject(options);
                } catch (JSONException e) {
                    SlyceLog.i(TAG, "Failed to create options Json");
                }
            }

            // Parameter 2.
            mContinuousRecognition = getArguments().getBoolean(ARG_CONTINUOUS_RECOGNITION);

            // Parameter 3.
            mShouldPauseScanner = getArguments().getBoolean(ARG_SHOULD_PAUSE_SCANNER);
        }
    }

    private void close(){
        if(isAttached){
            // getActivity().getFragmentManager().beginTransaction().remove(this).commit();
            getActivity().getFragmentManager().popBackStack();
        }
    }

    private void cancelSlyceProductsRequests(){
        mSlyceCamera.cancel();
    }

    private ImageProcessDialogFragment showDialogFragment(
            String processType,
            String imageDecodableString,
            OnImageProcessDialogFragmentListener listener){

        // Create and show the dialog.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ImageProcessDialogFragment newFragment = ImageProcessDialogFragment.newInstance(processType, imageDecodableString);
        mImageProcessDialogFragment = newFragment;
        newFragment.setmOnImageProcessDialogFragmentListener(listener);
        newFragment.show(ft, FRAGMENT_TAG);

        return newFragment;
    }

    private class ImageProcessDialogFragmentListener implements OnImageProcessDialogFragmentListener {

        @Override
        public void onImageProcessBarcodeRecognition(SlyceBarcode barcode) {
            if(mListener != null){
                // Notify the host application of barcode recognition
                mListener.onCameraFragmentBarcodeRecognition(barcode);
            }

            // Close SDK
            close();
        }

        @Override
        public void onImageProcess2DRecognition(String irid, String productInfo) {
            if(mListener != null){

                mDialogLayout.setVisibility(View.VISIBLE);

                // Notify the host application of MS recognition
                mListener.onCameraFragment2DRecognition(irid, productInfo);
            }
        }

        @Override
        public void onImageProcess2DExtendedRecognition(JSONArray products) {
            if(mListener != null){

                mDialogLayout.setVisibility(View.GONE);

                // Notify the host application of extra products details
                mListener.onCameraFragment2DExtendedRecognition(products);
            }
        }

        @Override
        public void onImageProcess3DRecognition(JSONObject products) {

            // Close SDK
            close();

            if(mListener != null){
                // Notify the host application of found products
                mListener.onCameraFragment3DRecognition(products);
            }
        }

        @Override
        public void onImageProcessDialogFragmentDismiss() {

            cancelSlyceProductsRequests();
        }
    }

    private class SlyceCameraListener implements OnSlyceCameraListener{

        // OnSlyceCameraListener callbacks
        @Override
        public void onCameraResultsReceived(JSONObject products) {

            // Close SDK
            close();

            if(isAttached){
                // Notify the host application of found products
                mListener.onCameraFragment3DRecognition(products);

                // Notify ImageProcessDialogFragment for found products
                mImageProcessDialogFragment.onCameraResultsReceived(products);
            }
        }

        @Override
        public void onCameraBarcodeDetected(SlyceBarcode barcode) {
            if(isAttached){
                // Notify the host application of barcode recognition
                mListener.onCameraFragmentBarcodeRecognition(barcode);
            }
        }

        @Override
        public void onCameraImageDetected(String irId, String productInfo) {

            if(isAttached){

                mDialogLayout.setVisibility(View.VISIBLE);

                // Notify the host application of MS recognition
                mListener.onCameraFragment2DRecognition(irId, productInfo);

                if(mImageProcessDialogFragment != null){
                    // Close ImageProcessDialogFragment
                    mImageProcessDialogFragment.dismiss();
                }
            }
        }

        @Override
        public void onCameraImageInfoReceived(JSONArray products) {

            if(isAttached){

                mDialogLayout.setVisibility(View.GONE);

                // Notify the host application of extra products details
                mListener.onCameraFragment2DExtendedRecognition(products);
            }
        }

        @Override
        public void onCameraSlyceProgress(long progress, String message, String id) {
            if(isAttached){
                // Notify ImageProcessDialogFragment for searching progress
                mImageProcessDialogFragment.onProgress(progress, message);
            }
        }

        @Override
        public void onCameraSlyceRequestStage(SlyceRequestStage message) {
            if(isAttached){
                // Notify ImageProcessDialogFragment for request stage
                mImageProcessDialogFragment.onRequestStage(message);
            }
        }

        @Override
        public void onSlyceCameraError(String message) {
            if(isAttached){

                mDialogLayout.setVisibility(View.GONE);

                // Notify host application
                mListener.onCameraFragmentError(message);

                // Notify ImageProcessDialogFragment
                mImageProcessDialogFragment.onError(message);
            }
        }

        @Override
        public void onSnap(Bitmap bitmap) {
            if(isAttached) {
                // Notify ImageProcessDialogFragment that bitmap is ready
                mImageProcessDialogFragment.onSnap(bitmap);
            }
        }

        @Override
        public void onTap(float x, float y) {
            // Displays the touch point
            Utils.performAlphaAnimation(mOnTapView, x, y);
        }

        @Override
        public void onCameraFinished(){

        }
        // OnSlyceCameraListener callbacks END
    }
    // PRIVATE METHODS END

    @Override
    public void onClick(View v) {

        int id = v.getId();

        if(id == R.id.close_button){

            Buzzer.getInstance().buzz(getActivity(), R.raw.slyce_click_sound, mSlyce.isSoundOn(), false);

            close();

        }else if(id == R.id.scan_tips_button){

            Buzzer.getInstance().buzz(getActivity(), R.raw.slyce_click_sound, mSlyce.isSoundOn(), false);

            ScanningTipsDialogFragment dialogFragment = new ScanningTipsDialogFragment();
            dialogFragment.show(getFragmentManager(), null);

        }else if(id == R.id.gallery_button){

            Buzzer.getInstance().buzz(getActivity(), R.raw.slyce_click_sound, mSlyce.isSoundOn(), false);

            Utils.loadImageFromGallery(this, RESULT_LOAD_IMG);

        }else if(id == R.id.flash_button){

            Buzzer.getInstance().buzz(getActivity(), R.raw.slyce_flash_sound, mSlyce.isSoundOn(), false);

            mSlyceCamera.turnFlash();

        }else if(id == R.id.snap_button){

            Buzzer.getInstance().buzz(getActivity(), R.raw.slyce_click_sound, mSlyce.isSoundOn(), false);

            showDialogFragment(ImageProcessDialogFragment.CAMERA_BITMAP, null, new ImageProcessDialogFragmentListener());

            // Take a picture using SlyceCamera object
            mSlyceCamera.setContinuousRecognition(false);
            mSlyceCamera.snap();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // When an Image is picked
        if (requestCode == RESULT_LOAD_IMG && resultCode == getActivity().RESULT_OK && null != data) {

            // Extract Image String
            String imageDecodableString  = Utils.getImageDecodableString(data, getActivity().getApplicationContext());

            if(TextUtils.isEmpty(imageDecodableString)){

                SlyceLog.i(TAG, "Error occurred while picking an Image");

            }else{

                showDialogFragment(ImageProcessDialogFragment.GALLERY_BITMAP, imageDecodableString, new ImageProcessDialogFragmentListener());
            }

        } else {
            SlyceLog.i(TAG, "You haven't picked Image");
        }
    }
}
