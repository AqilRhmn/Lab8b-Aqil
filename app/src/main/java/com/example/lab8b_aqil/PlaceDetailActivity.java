package com.example.lab8b_aqil;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONObject;

public class PlaceDetailActivity extends AppCompatActivity {

    private ImageView ivPlacePhoto;
    private TextView tvPlaceName, tvPlaceAddress, tvPlacePhone, tvPlaceRating, tvPlaceStatus, tvPlaceWebsite;
    private static final String API_KEY = "API_KEY"; // Provided Key

    private double placeLat = 0.0;
    private double placeLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        // FIX: Dynamically handle window insets to prevent elements from sliding under the notification/status bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind layout views
        ivPlacePhoto = findViewById(R.id.ivPlacePhoto);
        tvPlaceName = findViewById(R.id.tvPlaceName);
        tvPlaceAddress = findViewById(R.id.tvPlaceAddress);
        tvPlacePhone = findViewById(R.id.tvPlacePhone);
        tvPlaceRating = findViewById(R.id.tvPlaceRating);
        tvPlaceStatus = findViewById(R.id.tvPlaceStatus);
        tvPlaceWebsite = findViewById(R.id.tvPlaceWebsite);

        // Extract parameters from intent payload
        String placeId = getIntent().getStringExtra("PLACE_ID");

        if (placeId != null) {
            fetchPlaceDetails(placeId);
        } else {
            Toast.makeText(this, "Place data missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchPlaceDetails(String placeId) {
        String url = "https://maps.googleapis.com/maps/api/place/details/json?place_id="
                + placeId + "&key=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject result = response.getJSONObject("result");

                        // Extract Strings Safely
                        String name = result.optString("name", "N/A");
                        String address = result.optString("formatted_address", "No address available.");
                        String phone = result.optString("formatted_phone_number", "No contact number.");
                        double rating = result.optDouble("rating", 0.0);
                        String website = result.optString("website", "No website provided.");

                        // Extract Geometry Coordinates
                        JSONObject locationObj = result.getJSONObject("geometry").getJSONObject("location");
                        placeLat = locationObj.getDouble("lat");
                        placeLng = locationObj.getDouble("lng");

                        // Extract Opening Hours Status
                        String statusStr = "Status: Closed/Unknown";
                        if (result.has("opening_hours")) {
                            boolean isOpenNow = result.getJSONObject("opening_hours").optBoolean("open_now", false);
                            statusStr = isOpenNow ? "Status: OPEN NOW" : "Status: CLOSED CURRENTLY";
                        }

                        // Display Data elements onto layout text fields
                        tvPlaceName.setText(name);
                        tvPlaceAddress.setText(address);
                        tvPlacePhone.setText(phone);
                        tvPlaceRating.setText("Rating: " + rating + " / 5.0");
                        tvPlaceStatus.setText(statusStr);
                        tvPlaceWebsite.setText(website);

                        // Extract Photos reference tag and display using Glide Library
                        if (result.has("photos")) {
                            String photoReference = result.getJSONArray("photos").getJSONObject(0).getString("photo_reference");
                            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference="
                                    + photoReference + "&key=" + API_KEY;

                            Glide.with(PlaceDetailActivity.this).load(photoUrl).into(ivPlacePhoto);
                        }

                        // Set up explicit Interactive Interactivity Click Hooks
                        setupClickListeners(phone, website, name);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to parse details", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error occurred", Toast.LENGTH_SHORT).show());

        queue.add(request);
    }

    private void setupClickListeners(String phone, String website, String name) {
        // Dial number intent
        tvPlacePhone.setOnClickListener(v -> {
            if (!phone.equals("No contact number.")) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(callIntent);
            }
        });

        // Web view url intent
        tvPlaceWebsite.setOnClickListener(v -> {
            if (!website.equals("No website provided.")) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(website));
                startActivity(webIntent);
            }
        });

        // Map navigation route intent
        tvPlaceAddress.setOnClickListener(v -> {
            if (placeLat != 0.0 && placeLng != 0.0) {
                String mapUri = "geo:" + placeLat + "," + placeLng + "?q=" + Uri.encode(name);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUri));
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });
    }
}
