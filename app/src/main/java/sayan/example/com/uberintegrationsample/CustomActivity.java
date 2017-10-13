package sayan.example.com.uberintegrationsample;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.uber.sdk.android.core.UberSdk;
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
import com.uber.sdk.rides.client.model.Ride;
import com.uber.sdk.rides.client.model.RideEstimate;
import com.uber.sdk.rides.client.model.RideRequestParameters;
import com.uber.sdk.rides.client.model.UserProfile;
import com.uber.sdk.rides.client.model.Vehicle;
import com.uber.sdk.rides.client.services.RidesService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomActivity extends AppCompatActivity {

    private LoginManager mLoginManager;
    private AccessTokenManager mAccessTokenManager;
    private ListView listView;
    private TextView textCarDescription;

    private final float PICKUP_LATITUDE = 22.649023f;
    private final float PICKUP_LONGITUDE = 88.415887f;

    private final float DROPOFF_LATITUDE = 22.627706f;
    private final float DROPOFF_LONGITUDE = 88.433186f;

    private List<String> productIds = new ArrayList<>();
    private List<String> fareIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom);
        initialiseUberSDK();
        performLogin();
        initialiseViews();
    }

    private void initialiseViews() {
        listView = (ListView) findViewById(R.id.listview_products);
        textCarDescription = (TextView) findViewById(R.id.textView_description);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        mLoginManager.onActivityResult(this, requestCode, resultCode, data);
    }

    private void initialiseUberSDK() {
        SessionConfiguration config = new SessionConfiguration.Builder()
                .setClientId(getResources().getString(R.string.client_id))
                .setRedirectUri(getResources().getString(R.string.redirect_url))
                .setEnvironment(SessionConfiguration.Environment.SANDBOX)
                .setScopes(Arrays.asList(Scope.PROFILE, Scope.RIDE_WIDGETS, Scope.REQUEST, Scope.REQUEST_RECEIPT))
                .build();
        UberSdk.initialize(config);
    }

    private void performLogin() {
        LoginCallback loginCallback = new LoginCallback() {
            @Override
            public void onLoginCancel() {
                // User canceled login
                Toast.makeText(CustomActivity.this, "User canceled login", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onLoginError(@NonNull AuthenticationError error) {
                // Error occurred during login
                Toast.makeText(CustomActivity.this, "Error occurred during login", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onLoginSuccess(@NonNull AccessToken accessToken) {
                // Successful login!  The AccessToken will have already been saved.
                Toast.makeText(CustomActivity.this, "Successful login!  The AccessToken will have already been saved.", Toast.LENGTH_SHORT).show();
                createSession();
            }
            @Override
            public void onAuthorizationCodeReceived(@NonNull String authorizationCode) {
                Toast.makeText(CustomActivity.this, "Authorization code received", Toast.LENGTH_SHORT).show();
                createSession();
            }
        };
        AccessTokenManager accessTokenManager = new AccessTokenManager(getApplicationContext());
        LoginManager loginManager = new LoginManager(accessTokenManager, loginCallback);
        loginManager.setRedirectForAuthorizationCode(true);
        loginManager.login(this);
        mAccessTokenManager = accessTokenManager;
        mLoginManager = loginManager;
    }

    private AccessToken getAccessToken(){
        return mAccessTokenManager.getAccessToken();
    }

    private void removeAccessToken(){
        mAccessTokenManager.removeAccessToken();
    }

    private void setAccessToken(AccessToken accessToken){
        mAccessTokenManager.setAccessToken(accessToken);
    }

    private void createSession() {
        Session session = mLoginManager.getSession();
        RidesService service = UberRidesApi.with(session).build().createService();
        GetUserProfileAsync getUserProfileAsync = new GetUserProfileAsync();
        getUserProfileAsync.execute(service);
    }

    private void getProducts(final RidesService service) throws IOException {
        final List<Product> products = service.getProducts(PICKUP_LATITUDE, PICKUP_LONGITUDE).execute().body().getProducts();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String productId = products.get(0).getProductId();
                String productName = products.get(0).getDisplayName();
                String productDescription = products.get(0).getDescription();
                String productImage = products.get(0).getImage();
                int productCapacity = products.get(0).getCapacity();

                Toast.makeText(CustomActivity.this, "Car: "+productName, Toast.LENGTH_SHORT).show();
                Toast.makeText(CustomActivity.this, "Description: "+productDescription, Toast.LENGTH_SHORT).show();
                Toast.makeText(CustomActivity.this, "Capacity: "+productCapacity, Toast.LENGTH_SHORT).show();
                Log.d("uberProducts", "getProductImage: "+productImage);
                CustomListAdapter adapter = new CustomListAdapter(products, service);
                listView.setAdapter(adapter);
            }
        });
    }

    private RideEstimate.Price getUpfrontFare(final TextView fare, RidesService service, String productId){
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
                            Toast.makeText(CustomActivity.this, "estimated Distance of trip: "+fareTrip.getDistanceEstimate()+fareTrip.getDistanceUnit
                                    (), Toast.LENGTH_SHORT).show();
                            Toast.makeText(CustomActivity.this, "estimated duration of trip: "+fareTrip.getDurationEstimate(), Toast.LENGTH_SHORT)
                                    .show();
                        price[0] = farePrice;
                            fareIds.add(response.body().getPrice().getFareId());
                            fare.setText(farePrice.getLowEstimate()+" - "+farePrice.getHighEstimate()+" in "+(fareTime == null?"":fareTime)+" min");
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
                @Override
                public void onFailure(Call<RideEstimate> call, Throwable t) {
                    Toast.makeText(CustomActivity.this, "failed", Toast.LENGTH_SHORT).show();
                }
            });
        return price[0];
    }

    private void requestForNewRide(RidesService service, int position){
        RideRequestParameters rideRequestParameters = new RideRequestParameters.Builder().setPickupCoordinates(PICKUP_LATITUDE, PICKUP_LONGITUDE)
                .setProductId(productIds.get(position))
                .setFareId(fareIds.get(position))
                .setDropoffCoordinates(DROPOFF_LATITUDE, DROPOFF_LONGITUDE)
                .build();
        service.requestRide(rideRequestParameters).enqueue(new Callback<Ride>() {
            @Override
            public void onResponse(Call<Ride> call, Response<Ride> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(CustomActivity.this, "Request ride success", Toast.LENGTH_SHORT).show();

                    try {
                        //ride details
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    Toast.makeText(CustomActivity.this, "Error: "+response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Ride> call, Throwable t) {
                Toast.makeText(CustomActivity.this, "Failed to request ride", Toast.LENGTH_SHORT).show();
            }
        });
    }




    private class GetUserProfileAsync extends AsyncTask<RidesService, String, String> {

        @Override
        protected String doInBackground(RidesService... ridesServices) {
            RidesService service = ridesServices[0];
            Response<UserProfile> response = null;
            UserProfile profile = null;
            try {
                response = service.getUserProfile().execute();
                if (response.isSuccessful()) {
                    //Success
                    profile = response.body();
                    getProducts(service);
                } else {
                    //Failure
                    ApiError error = ErrorParser.parseError(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return profile.getFirstName();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(CustomActivity.this, s, Toast.LENGTH_SHORT).show();
        }
    }

    private class CustomListAdapter extends BaseAdapter {

        private final RidesService service;
        private TextView title, seat, fare;
        List<Product> products;

        public CustomListAdapter(List<Product> products, RidesService service) {
            this.products = products;
            this.service = service;
        }

        @Override
        public int getCount() {
            return products.size();
        }

        @Override
        public Object getItem(int position) {
            return products.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = CustomActivity.this.getLayoutInflater();
            View rowView = layoutInflater.inflate(R.layout.child_list_products, null, true);
            title = rowView.findViewById(R.id.textView_name_child_list);
            seat = rowView.findViewById(R.id.textView_seat_child_list);
            fare = rowView.findViewById(R.id.textView_Fare_child_list);

            title.setText(products.get(position).getDisplayName());
            seat.setText(products.get(position).getCapacity()+"");
            getUpfrontFare(fare, service, products.get(position).getProductId());
            productIds.add(products.get(position).getProductId());
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    textCarDescription.setText(products.get(position).getDescription());
                    requestForNewRide(service, position);
                }
            });
            return rowView;
        }
    }

}
