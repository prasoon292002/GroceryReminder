package com.example.groceryreminder.location;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GroceryStoreDetector {
    private static final String TAG = "GroceryStoreDetector";
    private static final String PLACES_API_BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
    private static final String API_KEY = "AIzaSyAtuRjuo-r4hRv1e27U4PmNOSPDdIFlk2g"; // Replace with your actual API key
    private static final int SEARCH_RADIUS = 500; // Search radius in meters

    private RequestQueue requestQueue;

    public interface StoreDetectionCallback {
        void onStoreFound(String storeName, double distance);
        void onNoStoresFound();
        void onError(String errorMessage);
    }

    public GroceryStoreDetector(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public void detectNearbyGroceryStores(double latitude, double longitude,
                                          final StoreDetectionCallback callback) {
        String url = PLACES_API_BASE_URL +
                "location=" + latitude + "," + longitude +
                "&radius=" + SEARCH_RADIUS +
                "&type=grocery_or_supermarket" +
                "&key=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String status = response.getString("status");
                            if ("OK".equals(status)) {
                                JSONArray results = response.getJSONArray("results");
                                if (results.length() > 0) {
                                    JSONObject nearestStore = results.getJSONObject(0);
                                    String storeName = nearestStore.getString("name");

                                    // Calculate distance (simplified)
                                    JSONObject location = nearestStore.getJSONObject("geometry")
                                            .getJSONObject("location");
                                    double storeLat = location.getDouble("lat");
                                    double storeLng = location.getDouble("lng");
                                    double distance = calculateDistance(
                                            latitude, longitude, storeLat, storeLng);

                                    callback.onStoreFound(storeName, distance);
                                } else {
                                    callback.onNoStoresFound();
                                }
                            } else {
                                callback.onError("API returned status: " + status);
                            }
                        } catch (JSONException e) {
                            callback.onError("Error parsing JSON: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onError("Network error: " + error.getMessage());
                    }
                }
        );

        requestQueue.add(request);
    }

    // Simple distance calculation using Haversine formula
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000; // Convert to meters
    }
}