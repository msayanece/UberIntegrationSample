package sayan.example.com.uberintegrationsample;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.uber.sdk.android.core.auth.AccessTokenManager;
import com.uber.sdk.android.core.auth.AuthenticationError;
import com.uber.sdk.android.rides.RideParameters;
import com.uber.sdk.android.rides.RideRequestActivity;
import com.uber.sdk.android.rides.RideRequestActivityBehavior;
import com.uber.sdk.android.rides.RideRequestButton;
import com.uber.sdk.android.rides.RideRequestButtonCallback;
import com.uber.sdk.android.rides.RideRequestViewError;
import com.uber.sdk.core.auth.AccessToken;
import com.uber.sdk.rides.client.ServerTokenSession;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.error.ApiError;

public class RequestActivity extends AppCompatActivity implements RideRequestButtonCallback {
    private static final String DROPOFF_ADDR = null;
    private static final Double DROPOFF_LAT = 22.63478;
    private static final Double DROPOFF_LONG = 88.434167;
    private static final String DROPOFF_NICK = null;
    private static final String ERROR_LOG_TAG = "UberSDK-SampleActivity";
    private static final String PICKUP_ADDR = null;
    private static final Double PICKUP_LAT = 22.6489295;
    private static final Double PICKUP_LONG = 88.4159275;
    private static final String PICKUP_NICK = null;
    private static final String UBERX_PRODUCT_ID = "a1111c8c-c720-46c3-8534-2fcdd730040d";
    private static final int WIDGET_REQUEST_CODE = 1234;

    public static final String CLIENT_ID = "aDaqZMKKK0gZWJe_gtREtUZMFkC_ZGsK";
    public static final String REDIRECT_URI = "https://www.uber.com/en-IN";
    private static final String SERVER_TOKEN = "ghSAqmQlrr8Hpg-JoS42cnlidjElTNpECBZydWkM";

    private SessionConfiguration configuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        configuration = new SessionConfiguration.Builder()
                .setRedirectUri(REDIRECT_URI)
                .setClientId(CLIENT_ID)
                .setServerToken(SERVER_TOKEN)
                .build();

        validateConfiguration(configuration);
        ServerTokenSession session = new ServerTokenSession(configuration);

        RideParameters rideParametersForProduct = new RideParameters.Builder()
                .setProductId(UBERX_PRODUCT_ID)
                .setPickupLocation(PICKUP_LAT, PICKUP_LONG, PICKUP_NICK, PICKUP_ADDR)
                .setDropoffLocation(DROPOFF_LAT, DROPOFF_LONG, DROPOFF_NICK, DROPOFF_ADDR)
//                .setPickupToMyLocation()
                .build();
        // This button demonstrates launching the RideRequestActivity (customized button behavior).
        // You can optionally setRideParameters for pre-filled pickup and dropoff locations.
        RideRequestButton uberButtonWhite = (RideRequestButton) findViewById(R.id.uber_button_white);
        RideRequestActivityBehavior rideRequestActivityBehavior = new RideRequestActivityBehavior(this,
                WIDGET_REQUEST_CODE, configuration);
        uberButtonWhite.setRequestBehavior(rideRequestActivityBehavior)
                .setRideParameters(rideParametersForProduct)
                .setSession(session)
                .setCallback(new RideRequestButtonCallback() {
                    @Override
                    public void onRideInformationLoaded() {
                        Toast.makeText(RequestActivity.this, "onRideInformationLoaded", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(ApiError apiError) {
                        Toast.makeText(RequestActivity.this, "onApiError", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Toast.makeText(RequestActivity.this, "onError", Toast.LENGTH_SHORT).show();
                    }
                })
                .loadRideInformation();

    }

    @Override
    public void onRideInformationLoaded() {
        Toast.makeText(this, "Estimates have been refreshed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(ApiError apiError) {
        Toast.makeText(this, apiError.getClientErrors().get(0).getTitle(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(Throwable throwable) {
        Log.e("SampleActivity", "Error obtaining Metadata", throwable);
        Toast.makeText(this, "Connection error", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == WIDGET_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED && data != null) {
            if (data.getSerializableExtra(RideRequestActivity.AUTHENTICATION_ERROR) != null) {
                AuthenticationError error = (AuthenticationError) data.getSerializableExtra(RideRequestActivity
                        .AUTHENTICATION_ERROR);
                Toast.makeText(RequestActivity.this, "Auth error " + error.name(), Toast.LENGTH_SHORT).show();
                Log.d(ERROR_LOG_TAG, "Error occurred during authentication: " + error.toString
                        ().toLowerCase());
            } else if (data.getSerializableExtra(RideRequestActivity.RIDE_REQUEST_ERROR) != null) {
                RideRequestViewError error = (RideRequestViewError) data.getSerializableExtra(RideRequestActivity
                        .RIDE_REQUEST_ERROR);
                Toast.makeText(RequestActivity.this, "RideRequest error " + error.name(), Toast.LENGTH_SHORT).show();
                Log.d(ERROR_LOG_TAG, "Error occurred in the Ride Request Widget: " + error.toString().toLowerCase());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        AccessTokenManager accessTokenManager = new AccessTokenManager(this);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear) {
            accessTokenManager.removeAccessToken();
            Toast.makeText(this, "AccessToken cleared", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_copy) {
            AccessToken accessToken = accessTokenManager.getAccessToken();

            String message = accessToken == null ? "No AccessToken stored" : "AccessToken copied to clipboard";
            if (accessToken != null) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("UberSampleAccessToken", accessToken.getToken());
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.action_refresh_meta_data) {
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Validates the local variables needed by the Uber SDK used in the sample project
     * @param configuration
     */
    private void validateConfiguration(SessionConfiguration configuration) {
        String nullError = "%s must not be null";
        String sampleError = "Please update your %s in the gradle.properties of the project before " +
                "using the Uber SDK Sample app. For a more secure storage location, " +
                "please investigate storing in your user home gradle.properties ";

        checkNotNull(configuration, String.format(nullError, "SessionConfiguration"));
        checkNotNull(configuration.getClientId(), String.format(nullError, "Client ID"));
        checkNotNull(configuration.getRedirectUri(), String.format(nullError, "Redirect URI"));
        checkNotNull(configuration.getServerToken(), String.format(nullError, "Server Token"));
        checkState(!configuration.getClientId().equals("insert_your_client_id_here"),
                String.format(sampleError, "Client ID"));
        checkState(!configuration.getRedirectUri().equals("insert_your_redirect_uri_here"),
                String.format(sampleError, "Redirect URI"));
        checkState(!configuration.getRedirectUri().equals("insert_your_server_token_here"),
                String.format(sampleError, "Server Token"));
    }


    @NonNull
    public static <T> T checkNotNull(T value, @NonNull String errorMessage) {
        if (value == null) {
            throw new NullPointerException(errorMessage);
        }
        return value;
    }

    public static void checkState(final boolean expression, @NonNull String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
