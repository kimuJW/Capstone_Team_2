package com.example.capstone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import java.util.List;

public class ViewActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        userDao = db.userDao();

        loadData();
    }

    private void loadData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<User> userList = userDao.getAll();
                // UI 스레드에서 UI 업데이트 수행
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        userAdapter = new UserAdapter(userList, userDao, ViewActivity.this); // this: ViewActivity 인스턴스 전달
                        recyclerView.setAdapter(userAdapter);
                    }
                });
            }
        }).start();
    }
}