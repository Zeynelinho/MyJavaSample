package com.zeynelinho.myjavatry2.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.zeynelinho.myjavatry2.R;
import com.zeynelinho.myjavatry2.databinding.ActivityMapsBinding;
import com.zeynelinho.myjavatry2.model.Place;
import com.zeynelinho.myjavatry2.roomDB.PlaceDao;
import com.zeynelinho.myjavatry2.roomDB.PlaceDatabase;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    ActivityMapsBinding binding;
    ActivityResultLauncher<String> permissionLauncher;
    SharedPreferences sharedPreferences;
    LocationManager locationManager;
    LocationListener locationListener;
    Double selectedLatitude;
    Double selectedLongitude;
    boolean trackBoolean;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    PlaceDatabase placeDatabase;
    PlaceDao placeDao;
    Place placeFromMain;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        registerLauncher();

        trackBoolean = false;

        sharedPreferences = this.getSharedPreferences("com.zeynelinho.myjavatry2.view",MODE_PRIVATE);

        binding.saveButton.setEnabled(false);

        placeDatabase = Room.databaseBuilder(getApplicationContext(),PlaceDatabase.class,"Places").build();
        placeDao = placeDatabase.placeDao();

        selectedLatitude = 0.0;
        selectedLongitude = 0.0;




    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);


        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if (info.equals("new")) {

            binding.saveButton.setVisibility(View.VISIBLE);
            binding.deleteButton.setVisibility(View.GONE);

            locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {


                    trackBoolean = sharedPreferences.getBoolean("trackBoolean",false);
                    if (trackBoolean == false) {
                        LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));
                        sharedPreferences.edit().putBoolean("trackBoolean",true).apply();
                    }

                }
            };

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)) {

                    Snackbar.make(binding.getRoot(),"Permission needed for location!",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            //request permission
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);

                        }
                    }).show();

                }else {

                    //request permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);

                }


            }else {

                //permission granted
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                Location userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (userLocation != null) {
                    LatLng userLastLocation = new LatLng(userLocation.getLatitude(),userLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation,15));
                }
                mMap.setMyLocationEnabled(true);

            }

        }else {

            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(@NonNull LatLng latLng) {

                }
            });

            mMap.clear();

            placeFromMain = (Place) intent.getSerializableExtra("selectedPlace");
            LatLng latLng = new LatLng(placeFromMain.latitude,placeFromMain.longitude);
            binding.placeText.setText(placeFromMain.name);
            mMap.addMarker(new MarkerOptions().title(binding.placeText.getText().toString()).position(latLng));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15f));

            binding.saveButton.setVisibility(View.GONE);
            binding.deleteButton.setVisibility(View.VISIBLE);

        }






    }

    public void save(View view) {

        Place place = new Place(binding.placeText.getText().toString(),selectedLatitude,selectedLongitude);

        compositeDisposable.add(placeDao.insert(place)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::handleResponse));

    }

    private void handleResponse () {

        Intent intent = new Intent(this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    public void delete (View view) {

        if (placeFromMain != null) {

            compositeDisposable.add(placeDao.delete(placeFromMain)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(MapsActivity.this::handleResponse));

        }

    }

    public void registerLauncher() {

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastLocation != null) {
                            LatLng userLastLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation,15f));
                        }
                        mMap.isMyLocationEnabled();
                    }


                }else {

                    Toast.makeText(MapsActivity.this, "Permission needed!", Toast.LENGTH_SHORT).show();

                }
            }
        });

    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));

        selectedLatitude = latLng.latitude;
        selectedLongitude = latLng.longitude;

        binding.saveButton.setEnabled(true);

    }
}