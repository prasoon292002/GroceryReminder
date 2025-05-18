package com.example.groceryreminder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.groceryreminder.data.GroceryDatabase;
import com.example.groceryreminder.data.GroceryItem;

import java.util.ArrayList;
import java.util.List;

public class GroceryListActivity extends AppCompatActivity implements GroceryItemAdapter.GroceryItemListener {
    private RecyclerView rvGroceryItems;
    private TextView tvEmptyList;
    private Button btnAddMore;
    private GroceryItemAdapter adapter;
    private GroceryDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_list);

        // Initialize views
        rvGroceryItems = findViewById(R.id.rvGroceryItems);
        tvEmptyList = findViewById(R.id.tvEmptyList);
        btnAddMore = findViewById(R.id.btnAddMore);

        // Initialize database
        db = Room.databaseBuilder(getApplicationContext(),
                GroceryDatabase.class, "grocery_database").build();

        // Setup RecyclerView
        adapter = new GroceryItemAdapter(new ArrayList<>(), this);
        rvGroceryItems.setLayoutManager(new LinearLayoutManager(this));
        rvGroceryItems.setAdapter(adapter);

        // Observe grocery items changes
        db.groceryDao().getAllActiveItems().observe(this, new Observer<List<GroceryItem>>() {
            @Override
            public void onChanged(List<GroceryItem> groceryItems) {
                adapter.updateItems(groceryItems);

                // Show empty view if list is empty
                if (groceryItems.isEmpty()) {
                    tvEmptyList.setVisibility(View.VISIBLE);
                    rvGroceryItems.setVisibility(View.GONE);
                } else {
                    tvEmptyList.setVisibility(View.GONE);
                    rvGroceryItems.setVisibility(View.VISIBLE);
                }
            }
        });

        // Add more button click listener
        btnAddMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GroceryListActivity.this, AddItemActivity.class));
            }
        });
    }

    @Override
    public void onItemStatusChanged(final GroceryItem item) {
        // Execute database operation in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.groceryDao().update(item);
            }
        }).start();
    }

    @Override
    public void onItemDeleted(final GroceryItem item) {
        // Execute database operation in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.groceryDao().delete(item);
            }
        }).start();
    }
}