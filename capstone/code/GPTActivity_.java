package com.example.capstone;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;



public class GPTActivity extends AppCompatActivity {
    private static final String TAG = "GPTActivity";
    private RadioGroup radioGroupMode;
    private RadioButton radioSummary, radioDefinition, radioTranslation;
    private static final String API_KEY = "sk-proj-sGT2WtAGgLxsxqdCjERIT3BlbkFJAXjMc8f29crut3I8QtlC";
    private ImageView imageView;
    private TextView textViewGptResponse;
    private Button btnScanPage;
    private static final int REQUEST_CODE_SCAN = 101;
    private volatile String lastCopiedText = null; // 마지막 복사된 텍스트 저장 (동기화를 위해 volatile 사용)
    private String currentMode = "요약해줘"; // 기본 모드


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpt);
        textViewGptResponse = findViewById(R.id.textViewGptResponse);
        imageView = findViewById(R.id.imageView);

        btnScanPage = findViewById(R.id.btnScanPage);
        // "페이지 스캔" 버튼 클릭 시 Scan 액티비티로 이동
        btnScanPage.setOnClickListener(v -> {
            Intent intent = new Intent(GPTActivity.this, gptScan.class);
            startActivityForResult(intent, REQUEST_CODE_SCAN);
        });

        radioGroupMode = findViewById(R.id.radioGroupMode);
        radioSummary = findViewById(R.id.radioSummary);
        radioDefinition = findViewById(R.id.radioDefinition);
        radioTranslation = findViewById(R.id.radioTranslation);


        // 독서 시간 관련 SharedPreferences 설정
        elapsedTimeTextView = findViewById(R.id.elapsedTimeTextView);  //시간 측정하는 부분
        sharedPreferences = getSharedPreferences("ActivityTime", Context.MODE_PRIVATE);  //1
        elapsedTime = sharedPreferences.getLong("elapsedTime", 0);  //2
        displayElapsedTime(); //3

        // 라디오 버튼의 선택 상태에 따라 모드 설정
        radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedButton = findViewById(checkedId);
            currentMode = selectedButton.getTag().toString(); // 태그를 가져와 모드 설정
            Log.d(TAG, "현재 모드: " + currentMode);
        });

        Button endReadingButton = findViewById(R.id.btnEndReading);
        endReadingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GPTActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // 클립보드에 복사된 텍스트를 감지하여 GPT로 전송
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.addPrimaryClipChangedListener(() -> {
            if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence copiedText = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (copiedText != null) {
                    String text = copiedText.toString().trim(); // 공백 제거
                    Log.d(TAG, "클립보드에 복사된 텍스트: " + text);
                    if (!text.isEmpty() && !text.equals(lastCopiedText)) {
                        lastCopiedText = text;
                        sendToGpt(composePrompt(text));
                    }
                }
            }
        });
    }

    // Scan 액티비티의 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (data != null) {
                String extractedText = data.getStringExtra("EXTRACTED_TEXT");
                if (extractedText != null) {
                    textViewGptResponse.setText(extractedText);
                } else {
                    Toast.makeText(this, "추출된 텍스트가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String composePrompt(String text) {
        return String.format("\"%s\"에 대해 %s", text, currentMode);
    }
    private void sendToGpt(String prompt) {
        new Thread(() -> {
            Log.d(TAG, "GPT로 전송할 프롬프트: " + prompt);
            try {
                URL url = new URL("https://api.openai.com/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000); // 타임아웃 10초 설정
                conn.setReadTimeout(10000);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("model", "gpt-3.5-turbo");

                JSONArray messages = new JSONArray();
                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", prompt);
                messages.put(message);

                jsonInput.put("messages", messages);
                jsonInput.put("max_tokens", 500);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                StringBuilder response = new StringBuilder();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        if (choices.length() > 0) {
                            String responseText = choices.getJSONObject(0).getJSONObject("message").getString("content");
                            runOnUiThread(() -> textViewGptResponse.setText(responseText));
                        }
                    }
                } else {
                    Log.e("GPT-Error", "HTTP error code: " + responseCode);
                    runOnUiThread(() -> textViewGptResponse.setText("HTTP 오류: " + responseCode));
                }
            } catch (Exception e) {
                Log.e("GPT-Error", "Error sending text to GPT: " + e.getMessage(), e);
                runOnUiThread(() -> textViewGptResponse.setText("오류: " + e.getMessage()));
            }
        }).start();
    }




    private long startTime;
    private long elapsedTime;
    private SharedPreferences sharedPreferences;
    private TextView elapsedTimeTextView;
    private static final long IMAGE_CHANGE_THRESHOLD = 60000; // 1분 (60000ms)
    private static final long IMAGE_CHANGE_LEVEL = 1; // 이미지 변경 시간 기준 (6분)



    private Runnable imageChangeRunnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long timeElapsedSinceStart = currentTime - startTime;
            long totalElapsedTime = elapsedTime + timeElapsedSinceStart;

            // 경과 시간을 분과 초로 변환하여 표시
            long seconds = totalElapsedTime / 1000;
            long minutes = seconds / 60;
            long exp = seconds / 10;
            long level = exp / 100;
            exp = exp % 100;
            String levelmessage = String.format("LEVEL: %d  %d%%", level, exp);
            elapsedTimeTextView.setText(levelmessage);
            //elapsedTimeTextView.setText(String.format(String.format("Level:%d  %d", minutes, seconds)));
            // 3분 이상 경과했을 경우 이미지 변경
            if (level >= IMAGE_CHANGE_LEVEL) {
                imageView.setImageResource(R.drawable.image2);
            }

            // 1초마다 실행
            elapsedTimeTextView.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // 액티비티가 활성화될 때 시작 시간 기록
        startTime = System.currentTimeMillis();
        elapsedTimeTextView.postDelayed(imageChangeRunnable, 1000); // 1초 후 실행

    }

    @Override
    protected void onPause() {
        super.onPause();
        // 액티비티가 비활성화될 때 머무른 시간 계산 및 저장
        long endTime = System.currentTimeMillis();
        elapsedTime += endTime - startTime;
        elapsedTimeTextView.removeCallbacks(imageChangeRunnable); // 실행 중인 Runnable 제거


        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("elapsedTime", elapsedTime);
        editor.apply();

    }

    private void displayElapsedTime() { //독서 시간 보여주는 기능
        long seconds = elapsedTime / 1000;
        long minutes = seconds / 60;
        long exp = seconds / 10;
        long level = exp / 100;
        exp = exp % 100;
        String levelmessage = String.format("LEVEL: %d  %d%%", level, exp);
        elapsedTimeTextView.setText(levelmessage);
    }

}
