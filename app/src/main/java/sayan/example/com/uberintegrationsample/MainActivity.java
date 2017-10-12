package sayan.example.com.uberintegrationsample;

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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.uber.sdk.android.core.auth.AccessTokenManager;
import com.uber.sdk.android.core.auth.AuthenticationError;
import com.uber.sdk.android.core.auth.LoginButton;
import com.uber.sdk.android.core.auth.LoginCallback;
import com.uber.sdk.android.core.auth.LoginManager;
import com.uber.sdk.core.auth.AccessToken;
import com.uber.sdk.core.auth.Scope;
import com.uber.sdk.rides.client.Session;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.UberRidesApi;
import com.uber.sdk.rides.client.error.ApiError;
import com.uber.sdk.rides.client.error.ErrorParser;
import com.uber.sdk.rides.client.model.UserProfile;
import com.uber.sdk.rides.client.services.RidesService;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    public static final String CLIENT_ID = "aDaqZMKKK0gZWJe_gtREtUZMFkC_ZGsK";
    public static final String REDIRECT_URI = "https://www.uber.com/en-IN";

    private static final String LOG_TAG = "LoginSampleActivity";

    private static final int CUSTOM_BUTTON_REQUEST_CODE = 1113;


    private LoginButton whiteButton;
    private Button customButton;
    private AccessTokenManager accessTokenManager;
    private LoginManager loginManager;
    private SessionConfiguration configuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configuration = new SessionConfiguration.Builder()
                .setClientId(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .setScopes(Arrays.asList(Scope.PROFILE, Scope.RIDE_WIDGETS))
                .build();

        validateConfiguration(configuration);

        accessTokenManager = new AccessTokenManager(this);

        //Create a button with a custom request code
        whiteButton = (LoginButton) findViewById(R.id.uber_button_white);
        whiteButton.setCallback(new SampleLoginCallback())
                .setSessionConfiguration(configuration);

        //Create a button using a custom AccessTokenManager
        //Custom Scopes are set using XML for this button as well in R.layout.activity_sample

        //Use a custom button with an onClickListener to call the LoginManager directly
        loginManager = new LoginManager(accessTokenManager,
                new SampleLoginCallback(),
                configuration,
                CUSTOM_BUTTON_REQUEST_CODE);

        customButton = (Button) findViewById(R.id.custom_uber_button);
        customButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginManager.login(MainActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loginManager.isAuthenticated()) {
            loadProfileInfo();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(LOG_TAG, String.format("onActivityResult requestCode:[%s] resultCode [%s]",
                requestCode, resultCode));

        //Allow each a chance to catch it.
        whiteButton.onActivityResult(requestCode, resultCode, data);

        loginManager.onActivityResult(this, requestCode, resultCode, data);
    }

    public void toNextActivity(View view) {
        startActivity(new Intent(this, RequestActivity.class));
    }

    private class SampleLoginCallback implements LoginCallback {

        @Override
        public void onLoginCancel() {
            Toast.makeText(MainActivity.this, "Login cancelled", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLoginError(@NonNull AuthenticationError error) {
            Toast.makeText(MainActivity.this,
                    "Error: "+error.name(), Toast.LENGTH_LONG)
                    .show();
        }

        @Override
        public void onLoginSuccess(@NonNull AccessToken accessToken) {
            loadProfileInfo();
        }

        @Override
        public void onAuthorizationCodeReceived(@NonNull String authorizationCode) {
            Toast.makeText(MainActivity.this, "Your Auth code is: "+authorizationCode,
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void loadProfileInfo() {
        Session session = loginManager.getSession();
        RidesService service = UberRidesApi.with(session).build().createService();

        service.getUserProfile()
                .enqueue(new Callback<UserProfile>() {
                    @Override
                    public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Welcome "+ response.body().getFirstName(),Toast.LENGTH_LONG).show();
                        } else {
                            ApiError error = ErrorParser.parseError(response);
                            Toast.makeText(MainActivity.this, error.getClientErrors().get(0).getTitle(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UserProfile> call, Throwable t) {

                    }
                });
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

        accessTokenManager = new AccessTokenManager(this);

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
        checkState(!configuration.getClientId().equals("insert_your_client_id_here"),
                String.format(sampleError, "Client ID"));
        checkState(!configuration.getRedirectUri().equals("insert_your_redirect_uri_here"),
                String.format(sampleError, "Redirect URI"));
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
