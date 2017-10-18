package sayan.example.com.uberintegrationsample;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.uber.sdk.android.core.auth.AccessTokenManager;
import com.uber.sdk.android.core.auth.AuthenticationError;
import com.uber.sdk.android.core.auth.LoginCallback;
import com.uber.sdk.android.core.auth.LoginManager;
import com.uber.sdk.core.auth.AccessToken;
import com.uber.sdk.core.auth.Scope;
import com.uber.sdk.rides.client.Session;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.UberRidesApi;
import com.uber.sdk.rides.client.error.ApiError;
import com.uber.sdk.rides.client.error.ErrorParser;
import com.uber.sdk.rides.client.model.Driver;
import com.uber.sdk.rides.client.model.Location;
import com.uber.sdk.rides.client.model.Product;
import com.uber.sdk.rides.client.model.ProductsResponse;
import com.uber.sdk.rides.client.model.Ride;
import com.uber.sdk.rides.client.model.RideEstimate;
import com.uber.sdk.rides.client.model.RideMap;
import com.uber.sdk.rides.client.model.RideRequestParameters;
import com.uber.sdk.rides.client.model.SandboxRideRequestParameters;
import com.uber.sdk.rides.client.model.UserProfile;
import com.uber.sdk.rides.client.model.Vehicle;
import com.uber.sdk.rides.client.services.RidesService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomActivity2 extends AppCompatActivity {

    private SessionConfiguration config;
    private AccessTokenManager accessTokenManager;
    private LoginManager loginManager;
    private Button customButton;

    private final float PICKUP_LATITUDE = 22.649023f;
    private final float PICKUP_LONGITUDE = 88.415887f;

    private final float DROPOFF_LATITUDE = 22.64614f;
    private final float DROPOFF_LONGITUDE = 88.4157f;
    private List<Product> products;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom2);

        config = initialiseUberSDK();
        accessTokenManager = new AccessTokenManager(this);
        loginManager = new LoginManager(accessTokenManager,
                new LoginCallback() {

                    @Override
                    public void onLoginCancel() {
                        Toast.makeText(CustomActivity2.this, "Login cancelled", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onLoginError(@NonNull AuthenticationError error) {
                        Toast.makeText(CustomActivity2.this,
                                "Error: "+error.name(), Toast.LENGTH_LONG)
                                .show();
                    }

                    @Override
                    public void onLoginSuccess(@NonNull AccessToken accessToken) {
                        Toast.makeText(CustomActivity2.this, "Login success",
                                Toast.LENGTH_LONG)
                                .show();
                    }

                    @Override
                    public void onAuthorizationCodeReceived(@NonNull String authorizationCode) {
                        Toast.makeText(CustomActivity2.this, "Your Auth code is: "+authorizationCode,
                                Toast.LENGTH_LONG)
                                .show();
                        Ion.with(getApplicationContext())
                                .load("POST", "https://login.uber.com/oauth/v2/token")
                                .setBodyParameter("client_id",getResources().getString(R.string.client_id))
                                .setBodyParameter("client_secret",getResources().getString(R.string.client_secret))
                                .setBodyParameter("grant_type","authorization_code")
                                .setBodyParameter("redirect_uri",getResources().getString(R.string.redirect_url))
                                .setBodyParameter("code",authorizationCode)
                                .setBodyParameter("scope","profile request all_trips request_receipt history")
                                .asString()
                                .setCallback(new FutureCallback<String>() {
                                    @Override
                                    public void onCompleted(Exception e, String result) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(result);
                                            if (result.contains("error")) {
                                                String error = jsonObject.getString("error");
                                                Toast.makeText(CustomActivity2.this, error, Toast.LENGTH_SHORT).show();
                                            }else {
                                                String accessTokenTemp = jsonObject.getString("access_token");
                                                String expiresInTemp = jsonObject.getString("expires_in");
                                                String lastAuthenticatedTemp = jsonObject.getString("last_authenticated");
                                                String refreshTokenTemp = jsonObject.getString("refresh_token");
                                                String scopeTemp = jsonObject.getString("scope");
                                                String tokenTypeTemp = jsonObject.getString("token_type");

                                                AccessTokenManager accessTokenManager = new AccessTokenManager(getApplicationContext());
                                                int expirationTime = Integer.parseInt(expiresInTemp);
                                                List<Scope> scopes = Arrays.asList(Scope.PROFILE, Scope.HISTORY, Scope.REQUEST);
                                                String token = accessTokenTemp;
                                                String refreshToken = refreshTokenTemp;
                                                String tokenType = tokenTypeTemp;
                                                AccessToken accessToken = new AccessToken(expirationTime, scopes, token, refreshToken, tokenType);
                                                accessTokenManager.setAccessToken(accessToken);

                                                if (accessTokenManager.getAccessToken() != null){
                                                    createSession();
                                                }
                                            }
                                        } catch (JSONException e1) {
                                            e1.printStackTrace();
                                        }
                                    }
                                });
                    }
                },
                config,
                1113).setRedirectForAuthorizationCode(true);
        customButton = (Button) findViewById(R.id.button);
        customButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginManager.login(CustomActivity2.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loginManager.isAuthenticated()) {
//            createSession();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("ubercustomsayan", String.format("onActivityResult requestCode:[%s] resultCode [%s]",
                requestCode, resultCode));
        loginManager.onActivityResult(this, requestCode, resultCode, data);
    }

    private SessionConfiguration initialiseUberSDK() {
        config = new SessionConfiguration.Builder()
                .setClientId(getResources().getString(R.string.client_id))
//                .setServerToken(getResources().getString(R.string.server_token))
//                .setClientSecret(getResources().getString(R.string.client_secret))
                .setRedirectUri(getResources().getString(R.string.redirect_url))
                .setEnvironment(SessionConfiguration.Environment.SANDBOX)
                .setScopes(Arrays.asList(Scope.PROFILE, Scope.REQUEST, Scope.ALL_TRIPS, Scope.REQUEST_RECEIPT, Scope.HISTORY))
                .build();
//        UberSdk.initialize(config);
        return config;
    }

    private void createSession() {
        Session session = loginManager.getSession();
        RidesService service = UberRidesApi.with(session).build().createService();
        getProfileInfo(service);
        getProducts(service);
//        CustomListAdapter adapter = new CustomListAdapter(products, service);
//        listView.setAdapter(adapter);
    }

    private void getProfileInfo(RidesService service){
        service.getUserProfile()
                .enqueue(new Callback<UserProfile>() {
                    @Override
                    public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(CustomActivity2.this, "Welcome "+ response.body().getFirstName(),Toast.LENGTH_LONG).show();
                        } else {
                            ApiError error = ErrorParser.parseError(response);
                            Toast.makeText(CustomActivity2.this, error.getClientErrors().get(0).getTitle(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UserProfile> call, Throwable t) {
                        Toast.makeText(CustomActivity2.this, "cannot get user info", Toast.LENGTH_LONG).show();
                    }
                });

    }

    private void getProducts(final RidesService service) {
        service.getProducts(PICKUP_LATITUDE, PICKUP_LONGITUDE).enqueue(new Callback<ProductsResponse>() {
            @Override
            public void onResponse(Call<ProductsResponse> call, Response<ProductsResponse> response) {
                if (response.isSuccessful()){
                    if (response.body() != null){
                        products = response.body().getProducts();
                        String productId = products.get(1).getProductId();
                        String productName = products.get(0).getDisplayName();
                        String productDescription = products.get(0).getDescription();
                        String productImage = products.get(0).getImage();
                        int productCapacity = products.get(0).getCapacity();
                        Toast.makeText(CustomActivity2.this, "Car: "+productName, Toast.LENGTH_SHORT).show();
                        Toast.makeText(CustomActivity2.this, "Description: "+productDescription, Toast.LENGTH_SHORT).show();
                        Toast.makeText(CustomActivity2.this, "Capacity: "+productCapacity, Toast.LENGTH_SHORT).show();
                        Log.d("uberProducts", "getProductImage: "+productImage);

//                        SandboxProductRequestParameters sandboxProductRequestParameters = new SandboxProductRequestParameters.Builder().setDriversAvailable(true)
//                                .setSurgeMultiplier(1.5f).build();
//                        service.updateSandboxProduct(productId, sandboxProductRequestParameters);
                        Toast.makeText(CustomActivity2.this, "Product ID: " + productId, Toast.LENGTH_SHORT).show();
                        getUpfrontFare(service, productId);
                    }else {
                        Toast.makeText(CustomActivity2.this, "Code: "+response.code()+" Error: "+response.errorBody().toString(), Toast.LENGTH_SHORT)
                                .show();
                    }
                }else {
                    Toast.makeText(CustomActivity2.this, "Cannot fetch nearest cars", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProductsResponse> call, Throwable t) {
                Toast.makeText(CustomActivity2.this, "Failed to get nearest cars' info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private RideEstimate.Price getUpfrontFare(final RidesService service, final String productId){
        final RideEstimate.Price[] price = new RideEstimate.Price[1];
        RideRequestParameters rideRequestParameters = new RideRequestParameters.Builder().setPickupCoordinates(PICKUP_LATITUDE, PICKUP_LONGITUDE)
                .setProductId(productId)
                .setDropoffCoordinates(DROPOFF_LATITUDE, DROPOFF_LONGITUDE)
                .build();
        service.estimateRide(rideRequestParameters).enqueue(new Callback<RideEstimate>() {
            @Override
            public void onResponse(Call<RideEstimate> call, Response<RideEstimate> response) {
                if (call.isExecuted()){
                    try {
                        RideEstimate.Price farePrice = response.body().getPrice();
                        Integer fareTime = response.body().getPickupEstimate();
                        RideEstimate.Trip fareTrip = response.body().getTrip();
                        Toast.makeText(CustomActivity2.this, "estimated Distance of trip: "+fareTrip.getDistanceEstimate()+fareTrip.getDistanceUnit
                                (), Toast.LENGTH_SHORT).show();
                        Toast.makeText(CustomActivity2.this, "estimated duration of trip: "+fareTrip.getDurationEstimate(), Toast.LENGTH_SHORT)
                                .show();
                        requestForNewRide(service, productId, response.body().getPrice().getFareId(), response.body().getPrice().getSurgeConfirmationId());
                        price[0] = farePrice;
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onFailure(Call<RideEstimate> call, Throwable t) {
                Toast.makeText(CustomActivity2.this, "failed", Toast.LENGTH_SHORT).show();
            }
        });
        return price[0];
    }

    private void requestForNewRide(final RidesService service, String productId, String fareId, String surgeConfirmationId){
        RideRequestParameters rideRequestParameters = new RideRequestParameters.Builder().setPickupCoordinates(PICKUP_LATITUDE, PICKUP_LONGITUDE)
                .setProductId(productId)
                .setFareId(fareId)
                .setSurgeConfirmationId(surgeConfirmationId)
                .setDropoffCoordinates(DROPOFF_LATITUDE, DROPOFF_LONGITUDE)
                .build();
        service.requestRide(rideRequestParameters).enqueue(new Callback<Ride>() {
            @Override
            public void onResponse(Call<Ride> call, Response<Ride> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(CustomActivity2.this, "Request ride success", Toast.LENGTH_SHORT).show();
                    try {
                        //ride details
                        String rideId = response.body().getRideId();
                        String rideStatus = response.body().getStatus();
                        Log.d("uberridedetails", "rideId: " + rideId);
                        Log.d("uberridedetails", "rideStatus: " + rideStatus);

                        Integer rideEta = response.body().getEta();                           //estimated time of arrival in min
                        Float rideSurgeMultiplier = response.body().getSurgeMultiplier();     //rise in price

                        Log.d("uberridedetails", "rideEta: " + rideEta);
                        Log.d("uberridedetails", "rideSurgeMultiplier: " + rideSurgeMultiplier);

                        if (rideStatus.equals("processing")) {
                            Toast.makeText(CustomActivity2.this, "Processing...", Toast.LENGTH_SHORT).show();
                            service.getCurrentRide().enqueue(new RideDetailsCallback(service, rideId));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    Toast.makeText(CustomActivity2.this, "Error: "+response.message()+", " +response.errorBody()+", " +response.code()+", " +response
                            .raw()+", " +response.headers(), Toast.LENGTH_SHORT).show();
                    Log.d("uberErrorsayan", "Error: "+response.message()+", " +response.errorBody()+", " +response.code()+", " +response
                            .raw()+", " +response.headers());
//                    service.getCurrentRide().enqueue(new RideDetailsCallback(service));
                    service.cancelCurrentRide().enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()){
                                Toast.makeText(CustomActivity2.this, "successfully canceled current ride!", Toast.LENGTH_SHORT).show();
                            }else {
                                Toast.makeText(CustomActivity2.this, "Cancel current ride unsuccessful!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(CustomActivity2.this, "Could not cancel current ride!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    accessTokenManager.removeAccessToken();
                }
            }

            @Override
            public void onFailure(Call<Ride> call, Throwable t) {
                Toast.makeText(CustomActivity2.this, "Failed to request ride", Toast.LENGTH_SHORT).show();
            }
        });
    }

    class RideDetailsCallback implements Callback<Ride> {
        private final RidesService service;
        private final String rideId;

        RideDetailsCallback(RidesService service, String rideId){
            this.service = service;
            this.rideId = rideId;
        }

        @Override
        public void onResponse(Call<Ride> call, Response<Ride> response) {
            if (response.isSuccessful()) {
//                Toast.makeText(CustomActivity2.this, "successfully get Ride details", Toast.LENGTH_SHORT).show();
                switch (response.body().getStatus()) {
                    case "processing":
//                        Toast.makeText(CustomActivity2.this, "Processing...", Toast.LENGTH_SHORT).show();
                        call.clone().enqueue(this);
                        Handler handler =  new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SandboxRideRequestParameters rideParameters =
                                        new SandboxRideRequestParameters
                                                .Builder()
                                                .setStatus("accepted")
                                                .build();
                                service.updateSandboxRide(rideId, rideParameters)
                                                .enqueue(new Callback<Void>() {
                                                    @Override
                                                    public void onResponse(Call<Void> call, Response<Void> response) {

                                                    }

                                                    @Override
                                                    public void onFailure(Call<Void> call, Throwable t) {

                                                    }
                                                });
                            }
                        },5000);
                        break;
                    case "accepted":
//                        Toast.makeText(CustomActivity2.this, "Ride request accepted", Toast.LENGTH_SHORT).show();

                        String rideId = response.body().getRideId();
                        String rideStatus = response.body().getStatus();
                        Integer rideEta = response.body().getEta();                           //estimated time of arrival in min
                        Float rideSurgeMultiplier = response.body().getSurgeMultiplier();     //rise in price


                        Driver rideDriver = response.body().getDriver();
                        Location rideLocation = response.body().getLocation();
                        Vehicle rideVehicle = response.body().getVehicle();

                        //ride driver details
                        String driverName = rideDriver.getName();
                        String driverPhoneNumber = rideDriver.getPhoneNumber();
                        String driverPictureUri = rideDriver.getPictureUrl();
                        Float driverRating = rideDriver.getRating();

                        //ride Location details
                        Float rideLocationLatitude = rideLocation.getLatitude();
                        Float rideLocationLongitude = rideLocation.getLongitude();
                        Integer rideLocationBearing = rideLocation.getBearing();

                        //ride Vehicle details
                        String rideVehicleLicencePlate = rideVehicle.getLicensePlate();
                        String rideVehicleMake = rideVehicle.getMake();
                        String rideVehicleModel = rideVehicle.getModel();
                        String rideVehiclePictureUrl = rideVehicle.getPictureUrl();

                        //Log
                        Log.d("uberridedetails", "rideId: " + rideId);
                        Log.d("uberridedetails", "rideStatus: " + rideStatus);
                        Log.d("uberridedetails", "rideEta: " + rideEta);
                        Log.d("uberridedetails", "rideSurgeMultiplier: " + rideSurgeMultiplier);
                        Log.d("uberridedetails", "driverName: " + driverName);
                        Log.d("uberridedetails", "driverPhoneNumber: " + driverPhoneNumber);
                        Log.d("uberridedetails", "driverPictureUri: " + driverPictureUri);
                        Log.d("uberridedetails", "driverRating: " + driverRating);
                        Log.d("uberridedetails", "rideLocationLatitude: " + rideLocationLatitude);
                        Log.d("uberridedetails", "rideLocationLongitude: " + rideLocationLongitude);
                        Log.d("uberridedetails", "rideLocationBearing: " + rideLocationBearing);
                        Log.d("uberridedetails", "rideVehicleLicencePlate: " + rideVehicleLicencePlate);
                        Log.d("uberridedetails", "rideVehicleMake: " + rideVehicleMake);
                        Log.d("uberridedetails", "rideVehicleModel: " + rideVehicleModel);
                        Log.d("uberridedetails", "rideVehiclePictureUrl: " + rideVehiclePictureUrl);
                        call.clone().enqueue(this);
                        Handler handlerAccept =  new Handler();
                        handlerAccept.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SandboxRideRequestParameters rideParameters =
                                        new SandboxRideRequestParameters
                                                .Builder()
                                                .setStatus("arriving")
                                                .build();
                                service.updateSandboxRide(RideDetailsCallback.this.rideId, rideParameters)
                                        .enqueue(new Callback<Void>() {
                                            @Override
                                            public void onResponse(Call<Void> call, Response<Void> response) {

                                            }

                                            @Override
                                            public void onFailure(Call<Void> call, Throwable t) {

                                            }
                                        });
                            }
                        },1000);
                        break;
                    case "arriving":
//                        Toast.makeText(CustomActivity2.this, "Car arriving...", Toast.LENGTH_SHORT).show();
                        call.clone().enqueue(this);
                        Handler handlerArriving =  new Handler();
                        handlerArriving.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SandboxRideRequestParameters rideParameters =
                                        new SandboxRideRequestParameters
                                                .Builder()
                                                .setStatus("in_progress")
                                                .build();
                                service.updateSandboxRide(RideDetailsCallback.this.rideId, rideParameters)
                                        .enqueue(new Callback<Void>() {
                                            @Override
                                            public void onResponse(Call<Void> call, Response<Void> response) {

                                            }

                                            @Override
                                            public void onFailure(Call<Void> call, Throwable t) {

                                            }
                                        });
                            }
                        },10000);
                        break;
                    case "in_progress":
//                        Toast.makeText(CustomActivity2.this, "Ride in progress", Toast.LENGTH_SHORT).show();
                        call.clone().enqueue(this);

                        String rideId1 = response.body().getRideId();
                        String rideStatus1 = response.body().getStatus();
                        Integer rideEta1 = response.body().getEta();                           //estimated time of arrival in min
                        Float rideSurgeMultiplier1 = response.body().getSurgeMultiplier();     //rise in price


                        Driver rideDriver1 = response.body().getDriver();
                        Location rideLocation1 = response.body().getLocation();
                        Vehicle rideVehicle1 = response.body().getVehicle();

                        //ride driver details
                        String driverName1 = rideDriver1.getName();
                        String driverPhoneNumber1 = rideDriver1.getPhoneNumber();
                        String driverPictureUri1 = rideDriver1.getPictureUrl();
                        Float driverRating1 = rideDriver1.getRating();

                        //ride Location details
                        Float rideLocationLatitude1 = rideLocation1.getLatitude();
                        Float rideLocationLongitude1 = rideLocation1.getLongitude();
                        Integer rideLocationBearing1 = rideLocation1.getBearing();

                        //ride Vehicle details
                        String rideVehicleLicencePlate1 = rideVehicle1.getLicensePlate();
                        String rideVehicleMake1 = rideVehicle1.getMake();
                        String rideVehicleModel1 = rideVehicle1.getModel();
                        String rideVehiclePictureUrl1 = rideVehicle1.getPictureUrl();

                        //Log
                        Log.d("uberridedetails", "rideId: " + rideId1);
                        Log.d("uberridedetails", "rideStatus: " + rideStatus1);
                        Log.d("uberridedetails", "rideEta: " + rideEta1);
                        Log.d("uberridedetails", "rideSurgeMultiplier: " + rideSurgeMultiplier1);
                        Log.d("uberridedetails", "driverName: " + driverName1);
                        Log.d("uberridedetails", "driverPhoneNumber: " + driverPhoneNumber1);
                        Log.d("uberridedetails", "driverPictureUri: " + driverPictureUri1);
                        Log.d("uberridedetails", "driverRating: " + driverRating1);
                        Log.d("uberridedetails", "rideLocationLatitude: " + rideLocationLatitude1);
                        Log.d("uberridedetails", "rideLocationLongitude: " + rideLocationLongitude1);
                        Log.d("uberridedetails", "rideLocationBearing: " + rideLocationBearing1);
                        Log.d("uberridedetails", "rideVehicleLicencePlate: " + rideVehicleLicencePlate1);
                        Log.d("uberridedetails", "rideVehicleMake: " + rideVehicleMake1);
                        Log.d("uberridedetails", "rideVehicleModel: " + rideVehicleModel1);
                        Log.d("uberridedetails", "rideVehiclePictureUrl: " + rideVehiclePictureUrl1);

                        service.getRideMap(rideId1).enqueue(new Callback<RideMap>() {
                            @Override
                            public void onResponse(Call<RideMap> call, Response<RideMap> response) {
                                if (response.isSuccessful()) {
                                    Log.d("sayanuberridemap", "ride details status: " + response.body() == null ? "" : response.body().getHref());
                                }
                            }

                            @Override
                            public void onFailure(Call<RideMap> call, Throwable t) {

                            }
                        });

                        Handler handlerInProgress =  new Handler();
                        handlerInProgress.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SandboxRideRequestParameters rideParameters =
                                        new SandboxRideRequestParameters
                                                .Builder()
                                                .setStatus("completed")
                                                .build();
                                service.updateSandboxRide(RideDetailsCallback.this.rideId, rideParameters)
                                        .enqueue(new Callback<Void>() {
                                            @Override
                                            public void onResponse(Call<Void> call, Response<Void> response) {

                                            }

                                            @Override
                                            public void onFailure(Call<Void> call, Throwable t) {

                                            }
                                        });
                            }
                        },20000);
                        break;
                    case "no_drivers_available":
                        Toast.makeText(CustomActivity2.this, "no drivers available", Toast.LENGTH_SHORT).show();
                        break;
                    case "rider_canceled":
                        Toast.makeText(CustomActivity2.this, "Rider canceled the ride", Toast.LENGTH_SHORT).show();
                        break;
                    case "driver_canceled":
                        Toast.makeText(CustomActivity2.this, "Driver canceled the ride", Toast.LENGTH_SHORT).show();
                        break;
                    case "completed":
                        Toast.makeText(CustomActivity2.this, "Ride completed", Toast.LENGTH_SHORT).show();
                        Log.d("uberridedetails", "Completed");
                        break;
                }
            }else {
                Toast.makeText(CustomActivity2.this, "cannot get Ride details", Toast.LENGTH_SHORT).show();
                service.getRideDetails(rideId).enqueue(new Callback<Ride>() {
                    @Override
                    public void onResponse(Call<Ride> call, Response<Ride> response) {
                        if (response.isSuccessful()){
                            Log.d("sayanuberridedetails", "ride details status: "+response.body().getStatus());
//                            Log.d("sayanuberridedetails", "ride details getLocation: "+response.body().getLocation());
                            if (response.body().getStatus().equals("completed")){
                                Ion.with(getApplicationContext())
                                    .load("GET", "https://sandbox-api.uber.com/v1.2/requests/"+rideId+"/receipt")
                                    .addHeader("Authorization", accessTokenManager.getAccessToken().getTokenType()+" "+accessTokenManager.getAccessToken().getToken())
                                    .asString()
                                    .setCallback(new FutureCallback<String>() {
                                        @Override
                                        public void onCompleted(Exception e, String result) {
                                            try {
                                                JSONObject jsonObject = new JSONObject(result);
                                                String total_charged = jsonObject.getString("total_charged");
                                                Log.d("sayanuberridedetails", "Reciept: "+total_charged);

                                                String total_fare = jsonObject.getString("total_fare");
                                                Log.d("sayanuberridedetails", "Reciept: "+total_fare);

                                                String currency_code = jsonObject.getString("currency_code");
                                                Log.d("sayanuberridedetails", "Reciept: "+currency_code);

                                                String duration = jsonObject.getString("duration");
                                                Log.d("sayanuberridedetails", "Reciept: "+duration);

                                                String distance = jsonObject.getString("distance");
                                                Log.d("sayanuberridedetails", "Reciept: "+distance);

                                                String distance_label = jsonObject.getString("distance_label");
                                                Log.d("sayanuberridedetails", "Reciept: "+distance_label);

                                                JSONArray charge_adjustments = jsonObject.getJSONArray("charge_adjustments");
                                                for (int i = 0; i < charge_adjustments.length();i++){
                                                    JSONObject charge_adjustmentObject = charge_adjustments.getJSONObject(i);
                                                    String amount = charge_adjustmentObject.getString("amount");
                                                    Log.d("sayanuberridedetails", "Reciept: "+amount);

                                                    String name = charge_adjustmentObject.getString("name");
                                                    Log.d("sayanuberridedetails", "Reciept: "+name);

                                                    String type = charge_adjustmentObject.getString("type");
                                                    Log.d("sayanuberridedetails", "Reciept: "+type);
                                                }

                                                String total_owed = jsonObject.getString("total_owed")==null?"":jsonObject.getString("total_owed");
                                                Log.d("sayanuberridedetails", "Reciept: "+total_owed);

                                                String subtotal = jsonObject.getString("subtotal");
                                                Log.d("sayanuberridedetails", "Reciept: "+subtotal);
                                            } catch (JSONException e1) {
                                                e1.printStackTrace();
                                            }
                                        }
                                    });
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Ride> call, Throwable t) {

                    }
                });
            }
        }

        @Override
        public void onFailure(Call<Ride> call, Throwable t) {
            Toast.makeText(CustomActivity2.this, "Failed to get ride details", Toast.LENGTH_SHORT).show();
        }
    }
}
