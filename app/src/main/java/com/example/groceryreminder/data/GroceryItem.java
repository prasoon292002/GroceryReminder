package com.example.groceryreminder.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "grocery_items")
public class GroceryItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private int quantity;
    private boolean completed;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}