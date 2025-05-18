package com.example.groceryreminder.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

import com.example.groceryreminder.MainActivity;
import com.example.groceryreminder.R;
import com.example.groceryreminder.data.GroceryDatabase;
import com.example.groceryreminder.data.GroceryItem;
import com.example.groceryreminder.location.GroceryStoreDetector;
import com.example.groceryreminder.notification.ReminderNotificationManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_service_channel";
    private static final int NOTIFICATION_ID = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ExecutorService executorService;
    private GroceryDatabase db;
    private GroceryStoreDetector storeDetector;
    private ReminderNotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize database
        db = Room.databaseBuilder(getApplicationContext(),
                GroceryDatabase.class, "grocery_database").build();

        // Initialize store detector
        storeDetector = new GroceryStoreDetector(this);

        // Initialize notification manager
        notificationManager = new ReminderNotificationManager(this);

        // Initialize executor service
        executorService = Executors.newSingleThreadExecutor();

        // Create notification channel for foreground service
        createNotificationChannel();

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Define location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    handleNewLocation(location);
                }
            }
        };
    }

    private void handleNewLocation(final Location location) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // Get active grocery items
                List<GroceryItem> items = db.groceryDao().getAllItemsSync();

                if (!items.isEmpty()) {
                    // Check if there are grocery stores nearby
                    storeDetector.detectNearbyGroceryStores(
                            location.getLatitude(),
                            location.getLongitude(),
                            new GroceryStoreDetector.StoreDetectionCallback() {
                                @Override
                                public void onStoreFound(String storeName, double distance) {
                                    // Send notification to user
                                    notificationManager.showGroceryReminderNotification(
                                            storeName, items);
                                }

                                @Override
                                public void onNoStoresFound() {
                                    Log.d(TAG, "No stores found nearby");
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    Log.e(TAG, "Error detecting stores: " + errorMessage);
                                }
                            });
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as a foreground service
        startForeground(NOTIFICATION_ID, createNotification());

        // Request location updates
        requestLocationUpdates();

        // If service is killed, restart it
        return START_STICKY;
    }

    private void requestLocationUpdates() {
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setIntervalMillis(60000)          // Update interval: 1 minute
                    .setMinUpdateIntervalMillis(30000) // Fastest update interval: 30 seconds
                    .build();

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Lost location permission: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Grocery Reminder")
                .setContentText("Looking for grocery stores nearby")
                .setSmallIcon(R.drawable.ic_menu_shopping_cart)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback);

        // Shutdown executor
        executorService.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
