package com.example.capstone;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class InsertActivity extends AppCompatActivity {

    private EditText editTextBookName, editTextPage, editTextImageUrl;
    private Button buttonInsert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert);

        editTextBookName = findViewById(R.id.editTextBookName);
        editTextPage = findViewById(R.id.editTextPage);
        editTextImageUrl = findViewById(R.id.editTextImageUrl);
        buttonInsert = findViewById(R.id.buttonInsert);
        Button buttonViewAll = findViewById(R.id.buttonViewAll); // 새로운 버튼 추가

        Intent intent = getIntent();

        if (intent != null) {
            String title = intent.getStringExtra("title");
            String publisher = intent.getStringExtra("publisher");
            String author = intent.getStringExtra("author");
            int totalPages = intent.getIntExtra("totalPages", 0);
            String coverImageURL = intent.getStringExtra("coverImageURL");


            // Book 정보를 EditText에 설정
            editTextBookName.setText(title);
            editTextPage.setText(String.valueOf(totalPages));
            editTextImageUrl.setText(coverImageURL);
        }




        buttonViewAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InsertActivity.this, ViewActivity.class);
                startActivity(intent);
            }
        });
        buttonInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertData();
            }


        });
    }

    private void insertData() {
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        UserDao bookDao = db.userDao();

        new Thread(new Runnable() {
            @Override
            public void run() {
                User book = new User();
                book.bookName = editTextBookName.getText().toString().trim();
                book.page = Integer.parseInt(editTextPage.getText().toString().trim());
                book.imageurl = editTextImageUrl.getText().toString().trim();

                bookDao.insertAll(book);
                // UI 스레드에서 UI 업데이트 수행
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Book inserted", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }
}