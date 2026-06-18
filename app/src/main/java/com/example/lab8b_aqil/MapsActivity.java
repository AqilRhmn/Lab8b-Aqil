package com.example.lab8b_aqil;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.lab8b_aqil.PlaceDetailActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

// Import the auto-generated binding class for your activity_maps layout
import com.example.lab8b_aqil.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding; // Modern layout binding object
    private FusedLocationProviderClient client;
    private LatLng currentLatLng;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 44;
    private static final String API_KEY = "API_KEY"; // Provided API Key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Correctly inflate and bind the layout
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // FIX: Handle Window Insets dynamically to prevent overlapping with the status/notification bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize fused location provider context
        client = LocationServices.getFusedLocationProviderClient(this);

        // Bind layout category search buttons directly using the initialized binding object
        binding.btnMosque.setOnClickListener(v -> searchNearby("mosque"));
        binding.btnSchool.setOnClickListener(v -> searchNearby("school"));
        binding.btnHospital.setOnClickListener(v -> searchNearby("hospital"));

        // Check and request runtime tracking permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set Map view option to Hybrid initially (combination of normal & satellite)
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // Define initial multiple LatLng coordinates (Sydney regional landmarks as specified in lab)
        LatLng sydney = new LatLng(-34, 151);
        LatLng tamWorth = new LatLng(-31.083332, 150.916672);
        LatLng newCastle = new LatLng(-32.916668, 151.750000);
        LatLng brisbane = new LatLng(-27.470125, 153.021072);

        // Draw initial regional placeholder markers onto map view
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.addMarker(new MarkerOptions().position(tamWorth).title("Marker in Tamworth"));
        mMap.addMarker(new MarkerOptions().position(newCastle).title("Marker in Newcastle"));
        mMap.addMarker(new MarkerOptions().position(brisbane).title("Marker in Brisbane"));

        // Move camera view and center on default regional position
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 6f));

        // Enable user indicator blue dot if permission is matching
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }

        // Interactive Handler: Triggers when user clicks on a popup marker bubble info-window
        mMap.setOnInfoWindowClickListener(marker -> {
            if (marker.getTag() != null) {
                String placeId = marker.getTag().toString();

                // Intent transition packing the structural unique place identifier
                Intent intent = new Intent(MapsActivity.this, PlaceDetailActivity.class);
                intent.putExtra("PLACE_ID", placeId);
                startActivity(intent);
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Clear initial placeholder markers, re-center and zoom onto active user position
                mMap.clear();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));
            } else {
                Toast.makeText(MapsActivity.this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Custom method bound via layout XML to search queried manual text locations
    public void onMapSearch(View view) {
        // Access layout elements directly using the layout binding layer safely
        String location = binding.btnHospital.getText().toString();
        List<Address> addressList = null;

        if (location != null && !location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                // Fetch coordinates matching the input string
                addressList = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                mMap.addMarker(new MarkerOptions().position(latLng).title("Search Result: " + location));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    getCurrentLocation();
                }
            }
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    // Volley operational handler executing structural Google Places API lookup
    private void searchNearby(String type) {
        if (currentLatLng == null) {
            Toast.makeText(this, "Location not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Web service endpoint URL string composition
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + currentLatLng.latitude + "," + currentLatLng.longitude +
                "&radius=2000&type=" + type + "&key=" + API_KEY;

        // Reset map canvas elements, retaining only user location marker pin
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        // Parse up to a maximum of 10 targeted place entities
                        for (int i = 0; i < Math.min(results.length(), 10); i++) {
                            JSONObject place = results.getJSONObject(i);
                            JSONObject locationObj = place.getJSONObject("geometry").getJSONObject("location");
                            String name = place.getString("name");

                            // Extract the unique place identifier key for detail activity usage
                            String placeId = place.getString("place_id");

                            LatLng latLng = new LatLng(locationObj.getDouble("lat"), locationObj.getDouble("lng"));

                            // Generate customized Azure color pin marker layout allocations
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                            // Essential: Inject the extracted place_id context directly into marker metadata tag
                            if (marker != null) {
                                marker.setTag(placeId);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing results", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Request failed", Toast.LENGTH_SHORT).show());

        queue.add(request);
    }
}
