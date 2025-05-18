package com.example.groceryreminder.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.groceryreminder.GroceryListActivity;
import com.example.groceryreminder.R;
import com.example.groceryreminder.data.GroceryItem;

import java.util.List;

public class ReminderNotificationManager {
    private static final String CHANNEL_ID = "grocery_reminders";
    private static final int NOTIFICATION_ID = 2000;

    private Context context;
    private NotificationManager notificationManager;

    public ReminderNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Grocery Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminds you to buy groceries when near a store");
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showGroceryReminderNotification(String storeName, List<GroceryItem> items) {
        // Create intent to open grocery list
        Intent intent = new Intent(context, GroceryListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build content text based on items
        StringBuilder contentText = new StringBuilder();
        int itemCount = Math.min(items.size(), 3); // Show max 3 items

        for (int i = 0; i < itemCount; i++) {
            contentText.append("• ").append(items.get(i).getName());
            if (items.get(i).getQuantity() > 1) {
                contentText.append(" (").append(items.get(i).getQuantity()).append(")");
            }
            contentText.append("\n");
        }

        if (items.size() > 3) {
            contentText.append("• and ").append(items.size() - 3).append(" more...");
        }

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_menu_shopping_cart)
                .setContentTitle("You're near " + storeName)
                .setContentText("Don't forget to buy your grocery items!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contentText.toString()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}