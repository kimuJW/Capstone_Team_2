package com.example.capstone;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Scanning extends AppCompatActivity {
    private Book book; // 전역 변수로 Book 객체 선언
    private EditText editTextSearch;
    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);

        editTextSearch = findViewById(R.id.editTextSearch);
        Button buttonSearch = findViewById(R.id.buttonSearch);
        resultTextView = findViewById(R.id.result_TextView);

        Intent intent = getIntent();
        String receivedText = intent.getStringExtra("TEXT_TO_PASS");

        // EditText에 데이터 설정
        editTextSearch.setText(receivedText);

        Button buttonBack = findViewById(R.id.back_button);
        Button buttonNext = findViewById(R.id.next_button);

        //돌아가기
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), Scan.class));
            }
        });

        //다음버튼(메인화면 연결) or 내부 저장
        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (book == null) {
                    Toast.makeText(Scanning.this, "책 정보를 먼저 검색하세요.", Toast.LENGTH_SHORT).show();
                    return; // 객체가 생성되지 않았으므로 메서드 종료
                }

                Intent intent = new Intent(Scanning.this, InsertActivity.class);
                intent.putExtra("title", book.getTitle());
                intent.putExtra("publisher", book.getPublisher());
                intent.putExtra("author", book.getAuthor());
                intent.putExtra("totalPages", book.getTotalPages());
                intent.putExtra("coverImageURL", book.getCoverImageURL());
                startActivity(intent);




            }
        });




        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = editTextSearch.getText().toString();
                new GetBookInfo().execute(query);
            }
        });
    }
    // 알라딘 api 접속 관련
    private class GetBookInfo extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... queries) {
            try {
                String query = queries[0];
                String ttbKey = "ttbrudals25902146001"; // 여기에 TTBKey를 입력하세요.
                String urlString = "http://www.aladin.co.kr/ttb/api/ItemSearch.aspx?ttbkey=" + ttbKey + "&Query=" + query + "&QueryType=Title&cover=big&MaxResults=10&start=1&SearchTarget=Book&output=js&Version=20131101";
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(String response) {
            ImageView bookImage=findViewById(R.id.book_cover);

            super.onPostExecute(response);
            if (response == null) {
                response = "THERE WAS AN ERROR";
            }
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray itemsArray = jsonObject.getJSONArray("item");
                StringBuilder resultBuilder = new StringBuilder();
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject bookJson = itemsArray.getJSONObject(i);
                    String title = bookJson.getString("title");
                    String publisher = bookJson.optString("publisher", "출판사 정보 없음");
                    String author = bookJson.optString("author", "저자 정보 없음");
                    int totalPages = bookJson.optInt("startIndex", 0); // 페이지 수 정보가 없는 경우를 대비해 기본값을 0으로 설정
                    String coverImageURL = bookJson.optString("cover", "표지 이미지 없음");

                    book = new Book(title, publisher, author, totalPages, coverImageURL);
                    // 여기서 Book 객체를 활용하는 코드 부분. ex 결과 문자열에 책 제목을 추가:

                    ImageDownloadTask task = new ImageDownloadTask(bookImage); //cover 이미지 등록
                    task.execute(book.getCoverImageURL());
                    resultBuilder.append(book.getTitle()).append("\n");





                }
                resultTextView.setText(resultBuilder.toString());
            } catch (Exception e) {
                e.printStackTrace();
                resultTextView.setText("Error: Can't process the JSON results.");
            }
        }
    }
}