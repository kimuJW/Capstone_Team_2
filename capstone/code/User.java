package com.example.capstone;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "User")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int uid;
    public String bookName;
    public int page;
    public String imageurl;
}