package com.example.capstone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.SparseArray;
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
import java.io.IOException;




import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

public class ReadScan extends AppCompatActivity {
    //카메라 관련
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Button mCaptureButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_scan);

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

        //촬영 버튼
        mCaptureButton = findViewById(R.id.button3);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();
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
                    //카메라 찍은 사진 정보
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    // 이미지(bitmap)에서 텍스트 추출
                    recognizeText(bitmap);

                    // 카메라 미리 보기 재시작
                    camera.startPreview();
                }
            });
        }
    }

    // 텍스트 추출 관련
    private void recognizeText(Bitmap bitmap) {
        TextView myTextView = findViewById(R.id.review);

        // ML Kit Vision API를 사용하여 TextRecognizer 객체 생성
        TextRecognizer recognizer =
                TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());


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

                    // 추출된 텍스트를 토스트 메시지로 출력
                    Toast.makeText(ReadScan.this, "추출된 텍스트:\n" + stringBuilder.toString(), Toast.LENGTH_LONG).show();
                    myTextView.setText(stringBuilder.toString());
                })
                .addOnFailureListener(e -> {
                    // 텍스트 인식에 실패한 경우
                    Toast.makeText(ReadScan.this, "텍스트 인식 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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