package com.example.groceryreminder.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {GroceryItem.class}, version = 1, exportSchema = false)
public abstract class GroceryDatabase extends RoomDatabase {
    public abstract GroceryDao groceryDao();
}