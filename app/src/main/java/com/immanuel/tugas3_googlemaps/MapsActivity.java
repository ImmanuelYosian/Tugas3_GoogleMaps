package com.immanuel.tugas3_googlemaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.immanuel.tugas3_googlemaps.databinding.ActivityMapsBinding;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private static final int REQUEST_LOCATION = 1;
    LocationManager locationManager;
    Location locationGPS;
    FirebaseFirestore db;
    String orderId;
    private Boolean isNewOrder = true;
    private Marker selectedMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActivityCompat.requestPermissions( this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            OnGPS();
        } else {
            getLocation();
        }
        db = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void OnGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("Yes", new  DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(
                MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (locationGPS == null) {
                Toast.makeText(this, "Unable to find location. you will use default location Salatiga", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng firstMarker = new LatLng(-7.3305, 110.5084);
        mMap = googleMap;
        if(locationGPS != null){
            firstMarker = new LatLng(locationGPS.getLatitude(), locationGPS.getLongitude());
        }

        mMap.addMarker(new MarkerOptions().position(firstMarker).title(getFullyAddress(firstMarker)));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(firstMarker));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstMarker,15.0f));

        mMap.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        if(selectedMarker == null) {
            selectedMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(getFullyAddress(latLng)));
        } else {
            selectedMarker.setPosition(latLng);
            selectedMarker.setTitle(getFullyAddress(latLng));
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15.0f));
        binding.etPlaceName.getEditText().setText(getFullyAddress(latLng));
        binding.etLatitude.getEditText().setText(String.valueOf(latLng.latitude));
        binding.etLongitude.getEditText().setText(String.valueOf(latLng.longitude));
        binding.btnSave.setOnClickListener(view -> {
            saveOrder();
        });

        binding.btnGetData.setOnClickListener(view -> {
            updateOrder();
        });

        binding.btnResetOrder.setOnClickListener(view -> {
            resetOrder();
        });

    }

    private void resetOrder() {
        isNewOrder = true;
        binding.tvOrderId.setText("");
        binding.etName.getEditText().setText("");
        binding.etLongitude.getEditText().setText("");
        binding.etLatitude.getEditText().setText("");
        binding.etPlaceName.getEditText().setText("");
        binding.btnGetData.setVisibility(View.GONE);
        binding.btnResetOrder.setVisibility(View.GONE);
        binding.btnSave.setText("New Order");
    }

    private void updateOrder() {
        isNewOrder = false;
        orderId = binding.tvOrderId.getText().toString();
        DocumentReference order = db.collection("orders").document(orderId);
        order.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DocumentSnapshot documentSnapshot = task.getResult();
                if(documentSnapshot.exists()){

                    String name = documentSnapshot.get("name").toString();
                    Map<String,Object> place = (HashMap<String,Object>) documentSnapshot.get("place");

                    LatLng resultPlace = new LatLng(Double.parseDouble(place.get("latitude").toString()), Double.parseDouble(place.get("longitude").toString()));
                    selectedMarker.setPosition(resultPlace);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(resultPlace,15.0f));

                    binding.etName.getEditText().setText(name);
                    binding.etPlaceName.getEditText().setText(place.get("address").toString());
                    binding.btnGetData.setVisibility(View.GONE);
                    binding.btnResetOrder.setVisibility(View.VISIBLE);
                    binding.etLatitude.getEditText().setText(place.get("latitude").toString());
                    binding.etLongitude.getEditText().setText(place.get("longitude").toString());
                    binding.btnSave.setText("Update Data");
                } else {
                    isNewOrder = true;
                    Toast.makeText(this, "Document doesnt exist", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Oops! error when read the database", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveOrder() {
        Map<String, Object> order = new HashMap<>();
        Map<String, Object> place = new HashMap<>();

        String name = binding.etPlaceName.getEditText().getText().toString();

        place.put("address", binding.etPlaceName.getEditText().getText().toString());
        place.put("latitude", binding.etLatitude.getEditText().getText().toString());
        place.put("longitude", binding.etLongitude.getEditText().getText().toString());

        order.put("name", binding.etName.getEditText().getText().toString());
        order.put("createdData", new Date());
        order.put("place",place);

        orderId = binding.tvOrderId.getText().toString();

        if (isNewOrder){
            db.collection("orders")
                    .add(order)
                    .addOnSuccessListener(documentReference -> {
                        binding.etName.getEditText().setText("");
                        binding.etLongitude.getEditText().setText("");
                        binding.btnResetOrder.setVisibility(View.GONE);
                        binding.etLatitude.getEditText().setText("");
                        binding.etPlaceName.getEditText().setText("");
                        binding.tvOrderId.setText(documentReference.getId());
                        binding.btnGetData.setVisibility(View.VISIBLE);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal tambah order", Toast.LENGTH_SHORT).show();
                    });
        } else {
            db.collection("orders").document(orderId)
                    .set(order)
                    .addOnSuccessListener(unused -> {
                        binding.etName.getEditText().setText("");
                        binding.etLongitude.getEditText().setText("");
                        binding.etLatitude.getEditText().setText("");
                        binding.etPlaceName.getEditText().setText("");
                        binding.tvOrderId.setText(orderId);
                        binding.btnGetData.setVisibility(View.VISIBLE);

                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal Update order", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private String getFullyAddress(LatLng latLng) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder();

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                strAdd = strReturnedAddress.toString();
                Log.w("Current location adress", strReturnedAddress.toString());
            } else {
                Log.w("Current location adress", "No Address returned!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("Current loction address", "Canont get Address!");
        }
        return strAdd;
    }
}