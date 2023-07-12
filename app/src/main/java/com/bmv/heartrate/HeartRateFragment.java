package com.bmv.heartrate;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class HeartRateFragment extends Fragment  {
    private OutputAnalyzer analyzer;

    private final int REQUEST_CODE_CAMERA = 0;
    public static final int MESSAGE_UPDATE_REALTIME = 1;
    public static final int MESSAGE_UPDATE_FINAL = 2;
    public static final int MESSAGE_CAMERA_NOT_AVAILABLE = 3;
    private Button startScan;
    private TextView txtScanValue, txtMessage;
    private TextureView graphTextureView, cameraTextureView;
    private boolean justShared = false;

    private Context mAppContext;
//    private View mainView;

    public HeartRateFragment() {
        // Required empty public constructor
    }

    public static String getCurrentDateTime() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
            sdf.setTimeZone(TimeZone.getDefault());
            return sdf.format(new Date(System.currentTimeMillis()));
        } catch (Exception e) {

        }
        return "NA";
    }
    @SuppressLint("HandlerLeak")
    private final Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (msg.what == MESSAGE_UPDATE_REALTIME) {
                txtScanValue.setText(msg.obj.toString());
            }

            if (msg.what == MESSAGE_UPDATE_FINAL) {
                String finalString =  msg.obj.toString()+ mAppContext.getString(R.string.on) +  getCurrentDateTime();
                txtMessage.setText(finalString);
                startScan.setEnabled(true);
                startScan.setAlpha(1f);
                startScan.setText(getString(R.string.scanAgainStr));
                stopScan();
            }

            if (msg.what == MESSAGE_CAMERA_NOT_AVAILABLE) {
                Log.println(Log.WARN, "camera", msg.obj.toString());

                txtScanValue.setText(
                        R.string.camera_not_found
                );
                analyzer.stop();
            }
        }
    };
    CameraService cameraService = new CameraService(mainHandler);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.fragment_heart_rate, container, false);
        startScan = mainView.findViewById(R.id.startScan);
        txtScanValue = mainView.findViewById(R.id.txtScanValue);
        txtMessage = mainView.findViewById(R.id.txtMessage);
        graphTextureView = mainView.findViewById(R.id.graphTextureView);
        cameraTextureView = mainView.findViewById(R.id.cameraTextureView);
        startScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg = new Message();
                msg.what = MESSAGE_UPDATE_REALTIME;
                msg.obj = getString(R.string.startingScan);
                mainHandler.sendMessage(msg);
                startScan();
                v.setEnabled(false);
                v.setAlpha(0.3f);
            }
        });
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CODE_CAMERA);
        return mainView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Snackbar.make(
                        txtMessage,
                        getString(R.string.cameraPermissionRequired),
                        Snackbar.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAppContext = context;
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    private void startScan() {
        analyzer = new OutputAnalyzer(getActivity(), graphTextureView, mainHandler);

        SurfaceTexture previewSurfaceTexture = cameraTextureView.getSurfaceTexture();

        // justShared is set if one clicks the share button.
        if ((previewSurfaceTexture != null) && !justShared) {
            // this first appears when we close the application and switch back
            // - TextureView isn't quite ready at the first onResume.
            Surface previewSurface = new Surface(previewSurfaceTexture);

            // show warning when there is no flash
            if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                Snackbar.make(
                        txtMessage,
                        getString(R.string.noFlashWarning),
                        Snackbar.LENGTH_LONG
                ).show();
            }
            cameraService.start(getActivity(), previewSurface);
            analyzer.measurePulse(cameraTextureView);
        }
    }

    private void stopScan() {
        cameraService.stop();
        if (analyzer != null) analyzer.stop();
        analyzer = new OutputAnalyzer(getActivity(), graphTextureView, mainHandler);
    }

    @Override
    public void onResume() {
        super.onResume();
    }



}
