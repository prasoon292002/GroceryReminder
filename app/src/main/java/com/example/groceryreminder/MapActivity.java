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

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + lat + "," + lng +
                "&radius=1500" +
                "&type=grocery_or_supermarket" +
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
                                    JSONObject geometry = place.getJSONObject("geometry");
                                    JSONObject loc = geometry.getJSONObject("location");
                                    double placeLat = loc.getDouble("lat");
                                    double placeLng = loc.getDouble("lng");

                                    mMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(placeLat, placeLng))
                                            .title(name));
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
