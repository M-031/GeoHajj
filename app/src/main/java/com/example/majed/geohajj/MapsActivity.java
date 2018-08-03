package com.example.majed.geohajj;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import com.google.gson.Gson;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Messages;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import android.view.View;
import android.view.WindowManager;

import android.app.AlertDialog;
import android.content.DialogInterface;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener, ActivityCompat.OnRequestPermissionsResultCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final Gson gson = new Gson();

    private View mView;

    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;

    private static final int TTL_IN_SECONDS = 3 * 60; // Three minutes.
    // Key used in writing to and reading from SharedPreferences.
    private static final String KEY_UUID = "key_uuid";
    private static final String TAG = MapsActivity.class.getSimpleName();

    /**
     * Sets the time in seconds for a published message or a subscription to live. Set to three
     * minutes in this sample.
     */
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();

    /**
     * Creates a UUID and saves it to {@link SharedPreferences}. The UUID is added to the published
     * message to avoid it being undelivered due to de-duplication. See {@link DeviceMessage} for
     * details.
     */
    private static String getUUID(SharedPreferences sharedPreferences) {
        String uuid = sharedPreferences.getString(KEY_UUID, "");
        if (TextUtils.isEmpty(uuid)) {
            uuid = UUID.randomUUID().toString();
            sharedPreferences.edit().putString(KEY_UUID, uuid).apply();
        }
        return uuid;
    }

    /**
     * The entry point to Google Play Services.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * The {@link Message} object used to broadcast information about the device to nearby devices.
     */
    private Message mPubMessage;

    /**
     * A {@link MessageListener} for processing messages from nearby devices.
     */
    private MessageListener mMessageListener;

    /**
     * Adapter for working with messages from nearby publishers.
     */
    private ArrayList<String> mPinsArrayList;


    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;
    private LocationManager locationManager;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    private GoogleMap mMap;
    FusedLocationProviderClient mFusedLocationClient;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

        // Build the message that is going to be published. This contains the device name and a
        // UUID.
        mPubMessage = DeviceMessage.newNearbyMessage(getUUID(getSharedPreferences(
                getApplicationContext().getPackageName(), Context.MODE_PRIVATE)));

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
               logAndShowSnackbar(message.getContent().toString());
                // Called when a new message is found.
//                logAndShowSnackbar(DeviceMessage.fromNearbyMessage(message).getMessageBody() + "WOOOOOW");
                String msg = message.getContent().toString();
                extractMarker(message.getContent().toString());
            }

            @Override
            public void onLost(final Message message) {
                // Called when a message is no longer detectable nearby.

                mPinsArrayList.remove(
                        DeviceMessage.fromNearbyMessage(message).getMessageBody());
            }
        };
        subscribe();
    }


//    /**
//     * Builds {@link GoogleApiClient}, enabling automatic lifecycle management using
//     * {@link GoogleApiClient.Builder#enableAutoManage(FragmentActivity,
//     * int, GoogleApiClient.OnConnectionFailedListener)}. I.e., GoogleApiClient connects in
//     * {@link AppCompatActivity#onStart}, or if onStart() has already happened, it connects
//     * immediately, and disconnects automatically in {@link AppCompatActivity#onStop}.
//     */
//    private void buildGoogleApiClient() {
//        if (mGoogleApiClient != null) {
//            return;
//        }
//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addApi(Nearby.MESSAGES_API)
//                .addConnectionCallbacks(this)
//                .enableAutoManage(this, this)
//                .build();
//    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        logAndShowSnackbar("Exception while connecting to Google Play services: " +
                connectionResult.getErrorMessage());
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
        logAndShowSnackbar("Connection suspended. Error code: " + i);
    }

    /**
     * Subscribes to messages from nearby devices and updates the UI if the subscription either
     * fails or TTLs.
     */
    private void subscribe() {
        Log.i(TAG, "Subscribing");
//        if(mNearbyDevicesArrayAdapter == Null) {
//            mNearbyDevicesArrayAdapter.clear();
//        }
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer subscribing");
                    }
                }).build();

        Nearby.getMessagesClient(this).subscribe(mMessageListener, options);
//                .setResultCallback(new ResultCallback<Status>() {
//                    @Override
//                    public void onResult(@NonNull Status status) {
//                        if (status.isSuccess()) {
//                            Log.i(TAG, "Subscribed successfully.");
//                        } else {
//                            logAndShowSnackbar("Could not subscribe, status = " + status);
//                        }
//                    }
//                });
    }

    /**
     * Publishes a message to nearby devices and updates the UI if the publication either fails or
     * TTLs.
     */
    private void publish() {
        Log.i(TAG, "Publishing");
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer publishing");
                    }
                }).build();

        Nearby.getMessagesClient(this).publish(mPubMessage, options).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                logAndShowSnackbar("Failed to publish");
                e.printStackTrace();
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                logAndShowSnackbar("Published Sucessfully");

            }
        });
//                .setResultCallback(new ResultCallback<Status>() {
//                    @Override
//                    public void onResult(@NonNull Status status) {
//                        if (status.isSuccess()) {
//                            Log.i(TAG, "Published successfully.");
//                        } else {
//                            logAndShowSnackbar("Could not publish, status = " + status);
//                        }
//                    }
//                });
    }
    /**
     * Stops subscribing to messages from nearby devices.
     */
    private void unsubscribe() {
        Log.i(TAG, "Unsubscribing.");
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
    }

    /**
     * Stops publishing message to nearby devices.
     */
    private void unpublish() {
        Log.i(TAG, "Unpublishing.");
        Nearby.Messages.unpublish(mGoogleApiClient, mPubMessage);
    }

    /**
     * Logs a message and shows a {@link Snackbar} using {@code text};
     *
     * @param text The text used in the Log message and the SnackBar.
     */
    private void logAndShowSnackbar(final String text) {
        Log.w(TAG, text);
        View container = findViewById(R.id.map);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
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
        mMap = googleMap;
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        createMenu();
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        mMap.animateCamera(cameraUpdate);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    public void createMenu() {

        SpeedDialView speedDialView = findViewById(R.id.speedDial);
        speedDialView.addActionItem(

                new SpeedDialActionItem.Builder(R.id.fab_bus, R.drawable.ic_bus).create()
        );
        speedDialView.addActionItem(
                new SpeedDialActionItem.Builder(R.id.fab_camp, R.drawable.ic_camp).create()
        );
        speedDialView.addActionItem(
                new SpeedDialActionItem.Builder(R.id.fab_resturant, R.drawable.ic_resturant).create()
        );
        speedDialView.addActionItem(
                new SpeedDialActionItem.Builder(R.id.fab_metro, R.drawable.ic_metro).create()
        );

        final MapsActivity that = this;
        speedDialView.setOnActionSelectedListener(new SpeedDialView.OnActionSelectedListener() {
            @Override
            public boolean onActionSelected(SpeedDialActionItem speedDialActionItem) {
                switch (speedDialActionItem.getId()) {
                    case R.id.fab_bus:
                        placePin(that, R.drawable.ic_bus);
                        return false;
                    case R.id.fab_camp:
                        placePin(that, R.drawable.ic_camp);
                        return false;

                    case R.id.fab_resturant:
                        placePin(that, R.drawable.ic_resturant);
                        return false;

                    case R.id.fab_metro:
                        placePin(that, R.drawable.ic_metro);
                        return false;

                    default:
                        return false; // true to keep the Speed Dial open
                }
            }
        });
    }

    private void placePin(final MapsActivity that, @DrawableRes final int icon) {
        final String[] mLabel = new String[1];
        final EditText input = new EditText(that);
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(that)
                .setTitle("Please put your comment")
                .setView(input).setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                mLabel[0] = input.getText().toString();
                                createMarker(mLabel, that, icon);
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        // Remember, create doesn't show the dialog
        AlertDialog helpDialog = helpBuilder.create();
        helpDialog.show();
    }

    private void extractMarker(String pinString){
//        logAndShowSnackbar(pinString.substring(0, pinString.indexOf(' ')));
//        int tempIcon = R.drawable.ic_fire;
//        if (Integer.parseInt(pinString.substring(0, pinString.indexOf(' '))) == 1){
//             tempIcon = R.drawable.ic_bus;
//        }


        @DrawableRes final int icon = R.drawable.ic_bus;
        final String[] label = new String[1];
        label[0] = "New!";
        final MapsActivity that = this;
        createMarker(label,that,icon);
    }

    private void createMarker(final String[] mLabel, final MapsActivity that, @DrawableRes final int icon) {
        OnSuccessListener OSL = new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                if (location != null) {
                    mMap.addMarker(
                            new MarkerOptions()
                                    .position(
                                            new LatLng(location.getLatitude(), location.getLongitude())
                                    ).title(mLabel[0])
                                    .icon(bitmapDescriptorFromVector(that, icon))
                    );
                }
            }
        };
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(that, OSL);
        String pinString = "1 Hello";
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer publishing");
                    }
                }).build();

        Nearby.getMessagesClient(this).publish(new Message(pinString.getBytes()),options);

    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    @Override
    public void onStart() {
        super.onStart();
        logAndShowSnackbar("Started");
        Nearby.getMessagesClient(this).subscribe(mMessageListener);
    }

    @Override
    public void onStop() {
        logAndShowSnackbar("Stopped");
        Nearby.getMessagesClient(this).unsubscribe(mMessageListener);
        super.onStop();
    }


}
