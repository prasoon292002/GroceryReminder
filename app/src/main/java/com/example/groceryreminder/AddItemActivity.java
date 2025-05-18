package com.example.groceryreminder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.groceryreminder.data.GroceryDatabase;
import com.example.groceryreminder.data.GroceryItem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddItemActivity extends AppCompatActivity {
    private EditText etItemName;
    private EditText etItemQuantity;
    private Button btnSave;
    private GroceryDatabase db;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        etItemName = findViewById(R.id.etItemName);
        etItemQuantity = findViewById(R.id.etItemQuantity);
        btnSave = findViewById(R.id.btnSave);

        db = Room.databaseBuilder(getApplicationContext(),
                GroceryDatabase.class, "grocery_database").build();
        executorService = Executors.newSingleThreadExecutor();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveGroceryItem();
            }
        });
    }

    private void saveGroceryItem() {
        String name = etItemName.getText().toString().trim();
        String quantityStr = etItemQuantity.getText().toString().trim();

        if (name.isEmpty()) {
            etItemName.setError("Item name is required");
            return;
        }

        int quantity = 1;
        if (!quantityStr.isEmpty()) {
            try {
                quantity = Integer.parseInt(quantityStr);
            } catch (NumberFormatException e) {
                etItemQuantity.setError("Please enter a valid number");
                return;
            }
        }

        final GroceryItem item = new GroceryItem();
        item.setName(name);
        item.setQuantity(quantity);
        item.setCompleted(false);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                db.groceryDao().insert(item);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AddItemActivity.this, "Item added to your list",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
