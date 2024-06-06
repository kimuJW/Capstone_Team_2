package com.example.capstone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
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

public class gptScan extends AppCompatActivity {
    // 카메라 관련
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Button mCaptureButton;

    private TextView myTextView; // TextView 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        myTextView = findViewById(R.id.review); // TextView 초기화

        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();

        // 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없는 경우 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // 권한이 있는 경우 카메라 열기
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

            @Override //카메라 전환 관련
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


        // 촬영 버튼
        mCaptureButton = findViewById(R.id.button3);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage(); // 버튼 클릭 시 captureImage 메서드 호출
            }
        });
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

    // 카메라 찍은 후 처리 관련
    private void captureImage() {
        if (mCamera != null) {
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    // 카메라 찍은 사진 정보
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    // 이미지(bitmap)에서 텍스트 추출
                    recognizeText(bitmap);

                    // 카메라 미리 보기 재시작
                    camera.startPreview();
                }
            });
        }
    }

    // 텍스트 추출 관련 _ 기본 텍스트 인식
    private void recognizeText(Bitmap bitmap) {
        // TextView 가져오기
        TextView myTextView = findViewById(R.id.review);

        // ML Kit Vision API를 사용하여 TextRecognizer 객체 생성
        TextRecognizer recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());

        // InputImage 객체 생성
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // 이미지에서 텍스트 인식
        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    // 텍스트 인식에 성공한 경우
                    StringBuilder stringBuilder = new StringBuilder();
                    for (Text.TextBlock textBlock : text.getTextBlocks()) {
                        stringBuilder.append(textBlock.getText());
                        stringBuilder.append("\n");
                    }

                    // 추출된 텍스트를 결과로 설정하고 액티비티 종료
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("EXTRACTED_TEXT", stringBuilder.toString());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // 텍스트 인식에 실패한 경우
                    Toast.makeText(gptScan.this, "텍스트 인식 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(gptScan.this, "포커스 실패", Toast.LENGTH_SHORT).show();
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

    // 카메라 권한 관련
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 부여되었으면 카메라 열기
                openCamera();
            } else {
                // 권한이 거부되면 메시지 표시
                Toast.makeText(this, "카메라 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}