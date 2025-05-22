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
import java.util.ArrayList;

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
        // Filter out completed items to ensure we only show active items
        List<GroceryItem> activeItems = new ArrayList<>();
        for (GroceryItem item : items) {
            if (!item.isCompleted()) {
                activeItems.add(item);
            }
        }

        // Don't show notification if no active items
        if (activeItems.isEmpty()) {
            // Cancel any existing notification since there are no active items
            notificationManager.cancel(NOTIFICATION_ID);
            return;
        }

        // Create intent to open grocery list
        Intent intent = new Intent(context, GroceryListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build content text based on active items only
        StringBuilder contentText = new StringBuilder();
        int itemCount = Math.min(activeItems.size(), 3); // Show max 3 items

        for (int i = 0; i < itemCount; i++) {
            contentText.append("• ").append(activeItems.get(i).getName());
            if (activeItems.get(i).getQuantity() > 1) {
                contentText.append(" (").append(activeItems.get(i).getQuantity()).append(")");
            }
            contentText.append("\n");
        }

        if (activeItems.size() > 3) {
            contentText.append("• and ").append(activeItems.size() - 3).append(" more...");
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

    // Add method to cancel notification when no active items remain
    public void cancelGroceryReminderNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}