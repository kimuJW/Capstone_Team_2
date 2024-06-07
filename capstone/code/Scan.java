package com.example.capstone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

public class Scan extends AppCompatActivity {
    //카메라 관련
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

        //촬영 버튼
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
        // 메인 스레드의 Looper를 사용하여 Handler 인스턴스 생성
        Handler handler;
        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 여기에 지연 후 실행할 코드 작성
                String textToPass = myTextView.getText().toString();
                Intent intent = new Intent(getApplicationContext(), Scanning.class);
                intent.putExtra("TEXT_TO_PASS", textToPass);
                startActivity(intent);


            }
        }, 2000); // 2초 지연

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

/*
    private void recognizeBarcode(Bitmap bitmap) {
        // ML Kit 바코드 스캐닝 API 사용
        TextView myTextView = findViewById(R.id.review);
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.QR_CODE, Barcode.EAN_13, Barcode.CODE_128)
                .build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        // 이미지를 InputImage 객체로 변환
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // 바코드 스캔 작업 실행
        Task<List<Barcode>> result = scanner.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> detectedBarcodes) {
                        // 인식된 바코드 개수 확인
                        if (detectedBarcodes.size() > 0) {
                            // 인식된 바코드 처리
                            StringBuilder barcodeValues = new StringBuilder();
                            for (Barcode barcode : detectedBarcodes) {
                                String barcodeValue = barcode.getRawValue();
                                if (barcodeValue != null) {
                                    // 바코드 값을 TextView에 추가
                                    barcodeValues.append(barcodeValue).append("\n");
                                }
                            }
                            // 모든 바코드 값을 TextView에 설정
                            myTextView.setText(barcodeValues.toString());
                        } else {
                            // 바코드가 인식되지 않은 경우 처리
                            myTextView.setText("바코드가 인식되지 않았습니다.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // 바코드 스캔 실패 처리
                        Log.e("BarcodeScanning", "Barcode scanning failed: " + e.getMessage());
                    }
                });
    }
*/

    // 텍스트 추출 관련 _ 기본 텍스트 인식
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
                    Toast.makeText(Scan.this, "추출된 텍스트:\n" + stringBuilder.toString(), Toast.LENGTH_LONG).show();
                    myTextView.setText(stringBuilder.toString());
                })
                .addOnFailureListener(e -> {
                    // 텍스트 인식에 실패한 경우
                    Toast.makeText(Scan.this, "텍스트 인식 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

/*

    // 텍스트 추출관련 isbn 코드
    private void recognizeText(Bitmap bitmap) {


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
                    boolean foundIsbn = false;
                    for (Text.TextBlock textBlock : text.getTextBlocks()) {
                        String blockText = textBlock.getText();
                        if (blockText.toLowerCase().contains("isbn") || blockText.toLowerCase().contains("1sbn")) {
                            // ISBN 또는 1SBN을 포함하는 텍스트에서 숫자만 추출
                            String numbersOnly = blockText.replaceAll("[^0-9]", "");
                            if (blockText.toLowerCase().contains("1sbn")) {
                                // 1sbn의 "1"만 제거
                                numbersOnly = numbersOnly.replaceFirst("^1", "");
                            }
                            stringBuilder.append(numbersOnly);
                            stringBuilder.append("\n");
                            foundIsbn = true;
                        }
                    }

                    if (foundIsbn) {
                        // ISBN 또는 1SBN을 포함하는 텍스트 블록에서 추출된 숫자를 myTextView에 출력
                        myTextView.setText(stringBuilder.toString());
                    } else {
                        // ISBN 또는 1SBN을 포함하는 텍스트 블록이 없는 경우
                        myTextView.setText("ISBN 또는 1SBN을 포함하는 텍스트를 찾을 수 없습니다.");
                    }
                })
                .addOnFailureListener(e -> {
                    // 텍스트 인식에 실패한 경우
                    Toast.makeText(Scan.this, "텍스트 인식 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
*/
//바코드로 숫자인식



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