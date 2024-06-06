package com.example.capstone;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;
@Dao
public interface UserDao
{
    @Query("SELECT * FROM user")
    List<User> getAll();    //getAll로 테이블 모든 값 가져오기
    @Insert
    void insertAll(User... users);  //insertAll로 값 삽입
    @Delete
    void delete(User user);   //delete로 삭제

    @Insert
    void insertUser(User user);

    @Delete
    void deleteUser(User user);

    @Query("SELECT * FROM user")
    List<User> getAllUsers();
    @Query("SELECT * FROM User WHERE uid = :userId")
    User getUserById(int userId);

}
