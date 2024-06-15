package com.example.capstone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.io.IOException;
import java.util.List;

public class Scan extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Button mCaptureButton;
    private TextView myTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        myTextView = findViewById(R.id.review);
        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }

        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mCamera != null) {
                    try {
                        mCamera.setPreviewDisplay(holder);
                        mCamera.startPreview();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releaseCamera();
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    focusOnTouch(event);
                }
                return true;
            }
        });

        mCaptureButton = findViewById(R.id.button3);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImageAndProceed();
            }
        });
    }

    private void captureImageAndProceed() {
        captureImage();
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String textToPass = myTextView.getText().toString();
                Intent intent = new Intent(getApplicationContext(), Scanning.class);
                intent.putExtra("TEXT_TO_PASS", textToPass);
                startActivity(intent);
            }
        }, 2000);
    }

    private void openCamera() {
        mCamera = Camera.open();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void captureImage() {
        if (mCamera != null) {
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    recognizeText(bitmap);
                    camera.startPreview();
                }
            });
        }
    }

    private void recognizeText(Bitmap bitmap) {
        TextRecognizer recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    StringBuilder stringBuilder = new StringBuilder();
                    boolean foundIsbn = false;
                    for (Text.TextBlock textBlock : text.getTextBlocks()) {
                        String blockText = textBlock.getText();
                        if (blockText.toLowerCase().contains("isbn") || blockText.toLowerCase().contains("1sbn")) {
                            String numbersOnly = blockText.replaceAll("[^0-9]", "");
                            if (blockText.toLowerCase().contains("1sbn")) {
                                numbersOnly = numbersOnly.replaceFirst("^1", "");
                            }
                            stringBuilder.append(numbersOnly).append("\n");
                            foundIsbn = true;
                        }
                    }

                    if (foundIsbn) {
                        myTextView.setText(stringBuilder.toString());
                    } else {
                        myTextView.setText("ISBN 또는 1SBN을 포함하는 텍스트를 찾을 수 없습니다.");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Scan.this, "텍스트 인식 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void focusOnTouch(MotionEvent event) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getMaxNumFocusAreas() > 0) {
                Camera.Area focusArea = new Camera.Area(calculateFocusArea(event.getX(), event.getY()), 1000);
                parameters.setFocusAreas(java.util.Collections.singletonList(focusArea));
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(parameters);
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            //포커스 성공
                        }
                    }
                });
            } else {
                Toast.makeText(Scan.this, "포커스 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp((int) (x / mSurfaceView.getWidth() * 2000 - 1000), -1000, 1000);
        int top = clamp((int) (y / mSurfaceView.getHeight() * 2000 - 1000), -1000, 1000);
        return new Rect(left, top, left + 200, top + 200);
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "카메라 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}