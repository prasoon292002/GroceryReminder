package com.example.groceryreminder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final String TAG = "MapActivity";

    // Replace this with your actual Google Maps + Places API key
    private static final String API_KEY = "AIzaSyA2J3Li2keWgDAOjgS8IQaP61Ta3CwxCrs";

    // Keywords that indicate a place is likely a grocery store
    private static final Set<String> GROCERY_KEYWORDS = new HashSet<>(Arrays.asList(
            "grocery", "supermarket", "market", "food", "fresh", "provisions",
            "walmart", "target", "kroger", "safeway", "publix", "wegmans",
            "whole foods", "trader joe", "costco", "sam's club", "aldi",
            "giant", "stop & shop", "harris teeter", "food lion", "piggly wiggly",
            "heb", "meijer", "shoprite", "acme", "winn-dixie", "bi-lo",
            "farm", "mart", "shop", "store", "super", "hyper"
    ));

    // Keywords that indicate a place is NOT a grocery store (to exclude)
    private static final Set<String> EXCLUDE_KEYWORDS = new HashSet<>(Arrays.asList(
            "pharmacy", "drug", "hospital", "clinic", "medical", "health",
            "bank", "atm", "gas", "station", "restaurant", "cafe", "bar",
            "hotel", "motel", "church", "school", "office", "auto", "car"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        enableUserLocation();
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
                        fetchNearbyStores(location);
                    } else {
                        Log.e(TAG, "Location is null");
                    }
                });
    }

    private void fetchNearbyStores(Location location) {
        OkHttpClient client = new OkHttpClient();

        double lat = location.getLatitude();
        double lng = location.getLongitude();

        // More specific API call for grocery stores only
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + lat + "," + lng +
                "&radius=2000" + // Increased radius slightly for better coverage
                "&type=grocery_or_supermarket" +
                "&keyword=grocery supermarket food" + // Added keyword filter
                "&key=" + API_KEY;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API call failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        JSONArray results = jsonObject.getJSONArray("results");

                        runOnUiThread(() -> {
                            for (int i = 0; i < results.length(); i++) {
                                try {
                                    JSONObject place = results.getJSONObject(i);
                                    String name = place.getString("name");

                                    // Additional filtering to ensure it's actually a grocery store
                                    if (isGroceryStore(name, place)) {
                                        JSONObject geometry = place.getJSONObject("geometry");
                                        JSONObject loc = geometry.getJSONObject("location");
                                        double placeLat = loc.getDouble("lat");
                                        double placeLng = loc.getDouble("lng");

                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(placeLat, placeLng))
                                                .title(name));

                                        Log.d(TAG, "Added grocery store: " + name);
                                    } else {
                                        Log.d(TAG, "Filtered out non-grocery place: " + name);
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "Error parsing place JSON: " + e.getMessage());
                                }
                            }
                        });

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "API response failed: " + response.message());
                }
            }
        });
    }

    /**
     * Determines if a place is likely a grocery store based on its name and other attributes
     */
    private boolean isGroceryStore(String name, JSONObject place) {
        String nameLower = name.toLowerCase();

        // First check if it contains any exclude keywords (immediate rejection)
        for (String excludeKeyword : EXCLUDE_KEYWORDS) {
            if (nameLower.contains(excludeKeyword)) {
                return false;
            }
        }

        // Check if it contains grocery store keywords
        for (String groceryKeyword : GROCERY_KEYWORDS) {
            if (nameLower.contains(groceryKeyword)) {
                return true;
            }
        }

        // Additional check using place types if available
        try {
            if (place.has("types")) {
                JSONArray types = place.getJSONArray("types");
                for (int i = 0; i < types.length(); i++) {
                    String type = types.getString(i);
                    if (type.equals("grocery_or_supermarket") ||
                            type.equals("supermarket") ||
                            type.equals("food")) {
                        return true;
                    }
                    // Exclude specific non-grocery types
                    if (type.equals("pharmacy") ||
                            type.equals("hospital") ||
                            type.equals("gas_station") ||
                            type.equals("restaurant") ||
                            type.equals("bank")) {
                        return false;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error checking place types: " + e.getMessage());
        }

        // Default to false if no grocery keywords found
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        }
    }
}