package com.example.groceryreminder.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GroceryDao {
    @Insert
    void insert(GroceryItem item);

    @Update
    void update(GroceryItem item);

    @Delete
    void delete(GroceryItem item);

    @Query("SELECT * FROM grocery_items WHERE completed = 0")
    LiveData<List<GroceryItem>> getAllActiveItems();

    @Query("SELECT * FROM grocery_items")
    List<GroceryItem> getAllItemsSync();
}