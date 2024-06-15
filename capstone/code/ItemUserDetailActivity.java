package com.example.capstone;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ItemUserDetailActivity extends AppCompatActivity {

    private TextView textViewBookName;
    private TextView textViewPage;
    private TextView textViewImageUrl;
    private ImageView imageViewImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_user_detail);

        textViewBookName = findViewById(R.id.textViewBookName);
        textViewPage = findViewById(R.id.textViewPage);
        imageViewImageUrl = findViewById(R.id.imageViewImageUrl);

        ImageDownloadTask task = new ImageDownloadTask(imageViewImageUrl);

        Button btn1=findViewById(R.id.start_button);
        btn1.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                startActivity(new Intent(getApplicationContext(), GPTActivity.class));
            }
        });

        int userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId != -1) {
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            UserDao userDao = db.userDao();

            // 백그라운드 스레드에서 데이터 로딩
            new Thread(new Runnable() {
                @Override
                public void run() {
                    User user = userDao.getUserById(userId); // userDao에 getUserById() 메서드 추가 (아래 참조)

                    // UI 스레드에서 UI 업데이트
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (user != null) {
                                textViewBookName.setText(user.bookName);
                                textViewPage.setText("Page: " + user.page);
                                task.execute(user.imageurl);
                            } else {
                                // userId에 해당하는 User를 찾지 못한 경우 처리
                                textViewBookName.setText("User not found");
                            }
                        }
                    });
                }
            }).start();
        }
    }
}