package com.ibm.mobileclientaccess.clientsdk.android.auth.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationContext;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by iklein on 18/10/15.
 */
public class GoogleAuthenticationManager implements
        AuthenticationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static String GOOGLE_REALM = "wl_googleRealm";
    private static final String ACCESS_TOKEN_KEY = "accessToken";

    private Logger logger;

    private Activity ctx;

    /* Request code used to invoke sign in user interactions. */
    private static final int DEFAULT_GOOGLE_AUTHENTICATOR_RESOLVER_ID = 1234567890;

    /**
     * Default return code when cancel is pressed during fb authentication (info)
     */
    private static final String AUTH_CANCEL_CODE = "100";

    /**
     * Default return code when error occures (info)
     */
    private static final String AUTH_ERROR_CODE = "101";

    //    PlusClient

    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;

    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;

    /* A flag indicating that a PendingIntent is in progress and prevents
     * us from starting further intents.
     */
    private boolean mIntentInProgress;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    //singelton
    private static final Object lock = new Object();
    private static volatile GoogleAuthenticationManager instance;
    private AuthenticationContext authContext;

    /**
     * Manager singleton - used for registering and handling authentication
     * @return the GoogleAuthenticationManager singelton
     */
    public static GoogleAuthenticationManager getInstance() {
        GoogleAuthenticationManager tempManagerInstance = instance;
        if (tempManagerInstance == null) {
            synchronized (lock) {    // While we were waiting for the lock, another
                tempManagerInstance = instance;        // thread may have instantiated the object.
                if (tempManagerInstance == null) {
                    tempManagerInstance = new GoogleAuthenticationManager();
                    instance = tempManagerInstance;
                }
            }
        }
        return tempManagerInstance;
    }

    private GoogleAuthenticationManager() {
        this.logger = Logger.getInstance(GoogleAuthenticationManager.class.getSimpleName());
    }

    //////////////////////////////// Public API /////////////////////////////////////////
    /**
     * Supply context for initialization of Googles sdk
     *
     * @param ctx - needed to init google code - can be application context
     */
    public void register(Context ctx) {
        //register as authListener
        BMSClient.getInstance().registerAuthenticationListener(GOOGLE_REALM, this);

        // Build GoogleApiClient with access to basic profile
        mGoogleApiClient = new GoogleApiClient.Builder(ctx)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(new Scope(Scopes.PROFILE))
                .addScope(new Scope("email"))
                .addScope(new Scope("https://www.googleapis.com/auth/plus.profile.emails.read"))
                .addScope(new Scope(Scopes.PLUS_ME))
                .build();
    }

    /**
     * When the Google activity ends, it sends a result code to the activity, and that result needs to be transferred to the google code,
     * @param requestCode the intent request code
     * @param resultCode the result
     * @param data the data (if any)
     */
    public void onActivityResultCalled(int requestCode, int resultCode, Intent data) {
        // If the error resolution was not successful we should not resolve further.
        if (resultCode != Activity.RESULT_OK) {
            mShouldResolve = false;
        }

        mIsResolving = false;
        mGoogleApiClient.connect();
    }

    //////////////////////////////// Public API /////////////////////////////////////////

    /**
     * Signs-in to Google as identity provider and sends the access token back to the authentication handler.
     *
     * @param appId   The Google app id.
     * @param context context to pass for request resources
     */
    private void handleAuthentication(Context context, String appId) {
        if (context instanceof Activity) {
            this.ctx = (Activity)context;
            mShouldResolve = true;
            mGoogleApiClient.connect();
        } else {
            JSONObject obj = null;
            try {
                obj = createFailureResponse(AUTH_ERROR_CODE, "The context provided is not an ActivityContext, cannot proceed" );
            } catch (JSONException e) {
                logger.error("error creating JSON message");
            }

            onGoogleAuthenticationFailure(obj);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mShouldResolve = false;
        // We had to sign in - now we can finish off the token request.
        new RetrieveTokenTask().execute(Plus.AccountApi.getAccountName(mGoogleApiClient));
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (!mIntentInProgress && connectionResult.hasResolution()) {
            mIntentInProgress = true;
            try {
                connectionResult.startResolutionForResult((Activity)ctx, DEFAULT_GOOGLE_AUTHENTICATOR_RESOLVER_ID);
                mIsResolving = true;
            } catch (IntentSender.SendIntentException e) {
                logger.error("Error, Could not resolve ConnectionResult." + e.getLocalizedMessage());
                mIsResolving = false;
                mGoogleApiClient.connect();
            }
        } else {
            JSONObject obj = null;
            try {
                obj = createFailureResponse(AUTH_ERROR_CODE, "GoogleAuth - Connection Failed" );
            } catch (JSONException e) {
                logger.error("error creating JSON message");
            }
            onGoogleAuthenticationFailure(obj);
            this.ctx = null;
        }
    }

    private JSONObject createFailureResponse(String code, String msg) throws JSONException{
        JSONObject obj = new JSONObject();
        obj.put("errorCode", code);
        obj.put("msg", msg);
        return obj;
    }

    /**
     * Called when the authentication process has succeeded for Google, now we send the token as a response to BM
     * authentication challenge.
     * @param googleAccessToken the token response
     */
    public void onGoogleAccessTokenReceived(String googleAccessToken) {
        JSONObject object = new JSONObject();
        try {
            object.put(ACCESS_TOKEN_KEY, googleAccessToken);
            authContext.submitAuthenticationChallengeAnswer(object);
        } catch (JSONException e) {
            logger.error("Error in onGoogleAccessTokenReceived: " + e.getLocalizedMessage());
        }
    }

    public void onGoogleAuthenticationFailure(JSONObject userInfo) {
        authContext.submitAuthenticationFailure(userInfo);
        authContext = null;
    }

    void setAuthenticationContext(AuthenticationContext authContext) {
        this.authContext = authContext;
    }

    @Override
    public void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge, Context context) {
        setAuthenticationContext(authContext);
        handleAuthentication(context, null);
    }

    @Override
    public void onAuthenticationSuccess(Context ctx, JSONObject info) {
        authContext = null;
    }

    @Override
    public void onAuthenticationFailure(Context ctx, JSONObject info) {
        authContext = null;
    }

    private class RetrieveTokenTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String accountName = params[0];
            String scopes = "oauth2:profile email " + Scopes.PLUS_LOGIN + " " + Scopes.PLUS_ME + " https://www.googleapis.com/auth/plus.profile.emails.read";
            String token = null;
            Bundle appActivities = new Bundle();
            try {
                token = GoogleAuthUtil.getToken(ctx, accountName, scopes, appActivities);
            } catch (Exception e) {
                logger.error("Error getting google token: " + e.getLocalizedMessage());
            }
            return token;
        }

        @Override
        protected void onPostExecute(String token) {
            if (token == null) {
                JSONObject obj = null;
                try {
                    obj = createFailureResponse(AUTH_ERROR_CODE, "GoogleAuth - Token returned null, canont login" );
                } catch (JSONException e) {
                    logger.error("Error getting google token: " + e.getLocalizedMessage());
                }
                onGoogleAuthenticationFailure(obj);
                GoogleAuthenticationManager.this.ctx = null;
            }
            else {
                logger.debug("google token="+ token);
                onGoogleAccessTokenReceived(token);
                GoogleAuthenticationManager.this.ctx = null;
            }
        }
    }
}
