package com.example.capstone;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private UserDao userDao;

    private Activity activity;


    public UserAdapter(List<User> userList, UserDao userDao, Activity activity) {
        this.userList = userList;
        this.userDao = userDao;
        this.activity = activity;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textViewBookName.setText(user.bookName);
        // Delete 버튼에 클릭 리스너 추가
        holder.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    deleteUser(userList.get(currentPosition), currentPosition);
                }
            }
        });
        // item_user.xml 레이아웃 전체에 클릭 리스너 추가
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    Intent intent = new Intent(view.getContext(), ItemUserDetailActivity.class);
                    intent.putExtra("USER_ID", userList.get(currentPosition).uid); // 사용자 ID 전달
                    view.getContext().startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void deleteUser(User user, int position) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                userDao.delete(user);
                // UI 스레드에서 UI 업데이트 수행
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        userList.remove(position);
                        notifyItemRemoved(position);
                    }
                });
            }
        }).start();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewBookName;
        public Button buttonDelete;

        public UserViewHolder(View itemView) {
            super(itemView);
            textViewBookName = itemView.findViewById(R.id.textViewBookName);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }

}