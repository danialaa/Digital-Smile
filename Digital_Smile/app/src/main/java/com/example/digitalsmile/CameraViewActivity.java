package com.example.digitalsmile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;
import java.util.Arrays;

public class CameraViewActivity extends AppCompatActivity {
    TextureView cameraView;
    String[] permissions = new String[]{"android.permission.CAMERA"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_view);

        cameraView = findViewById(R.id.cameraView);

        if (arePermissionsGranted()) {
            viewCamera();
        } else {
            ActivityCompat.requestPermissions(this, permissions, 101);
        }
    }

    private void viewCamera() {
        CameraX.unbindAll();

        Size screenSize = new Size(cameraView.getWidth(), cameraView.getHeight());
        Rational screenRatio = new Rational(cameraView.getWidth(), cameraView.getHeight());
        PreviewConfig previewConfig = new PreviewConfig.Builder().setTargetAspectRatio(screenRatio)
                .setTargetResolution(screenSize).setLensFacing(CameraX.LensFacing.FRONT).build();

        Preview cameraPreview = new Preview(previewConfig);
        cameraPreview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                cameraView.setSurfaceTexture(output.getSurfaceTexture());
            }
        });

        ImageCaptureConfig captureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setLensFacing(CameraX.LensFacing.FRONT).build();
        ImageCapture capture = new ImageCapture(captureConfig);

        CameraX.bindToLifecycle(this, cameraPreview, capture);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (arePermissionsGranted()) {
                viewCamera();
            } else {
                Toast.makeText(this, "Camera permission not granted", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean arePermissionsGranted() {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }
}
