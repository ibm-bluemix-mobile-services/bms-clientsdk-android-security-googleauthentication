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
public class CombinedGoogleAuthentication implements
        AuthenticationListener,
        GoogleAuthenticationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static String GOOGLE_REALM = "wl_googleRealm";
    private static final String ACCESS_TOKEN_KEY = "accessToken";

    /* Request code used to invoke sign in user interactions. */
    public static final int DEFAULT_GOOGLE_AUTHENTICATOR_RSOLVER_ID = 1234567890;

    private Logger logger;
    private GoogleAuthenticationListener googleAuthenticationListener;

    private Activity ctx;

    /* Request code used to invoke sign in user interactions. */
    public static final int DEFAULT_GOOGLE_AUTHENTICATOR_RESOLVER_ID = 1234567890;

    /**
     * Default return code when cancel is pressed during fb authentication (info)
     */
    public static final String AUTH_CANCEL_CODE = "100";

    /**
     * Default return code when error occures (info)
     */
    public static final String AUTH_ERROR_CODE = "101";

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
    private static volatile CombinedGoogleAuthentication instance;
    private AuthenticationContext authContext;

    /**
     * Manager singleton - used for registering and handling authentication
     * @return the GoogleAuthenticationManager singelton
     */
    public static CombinedGoogleAuthentication getInstance() {
        CombinedGoogleAuthentication tempManagerInstance = instance;
        if (tempManagerInstance == null) {
            synchronized (lock) {    // While we were waiting for the lock, another
                tempManagerInstance = instance;        // thread may have instantiated the object.
                if (tempManagerInstance == null) {
                    tempManagerInstance = new CombinedGoogleAuthentication();
                    instance = tempManagerInstance;
                }
            }
        }
        return tempManagerInstance;
    }

    private CombinedGoogleAuthentication() {
        this.logger = Logger.getInstance(GoogleAuthenticationManager.class.getSimpleName());
    }

    /**
     * Register the default Handler for handling OAuth requests.
     * @param ctx - needed context for Google SDK initialization
     */
    public void useDefaultAuthenticationListener(Context ctx) {
        //Construct the default Google Authentication listener
        initDefaultAuthenticationListener(ctx);
        registerAuthenticationListener(ctx, this);
    }

    /**
     * Register an Authentication listener
     * @param ctx context for google api code
     * @param listener the listener to register
     */
    public void registerAuthenticationListener(Context ctx, GoogleAuthenticationListener listener) {
        googleAuthenticationListener = listener;

        //register as authListener
        BMSClient.getInstance().registerAuthenticationListener(GOOGLE_REALM, this);
    }

    /**
     * Construct the default Google Authentication Listener
     * @param context - need to pass the ApplicationContext for initializing the googleApiClient since it is not a singelton
     */
    private void initDefaultAuthenticationListener(Context context) {
        this.logger = Logger.getInstance(DefaultGoogleAuthenticationListener.class.getSimpleName());

        // Build GoogleApiClient with access to basic profile
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
//                .addScope(new Scope(Scopes.PROFILE))
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(new Scope(Scopes.PROFILE))
                .addScope(new Scope("email"))
                .addScope(new Scope("https://www.googleapis.com/auth/plus.profile.emails.read"))
                .addScope(new Scope(Scopes.PLUS_ME))
                .build();
    }

    /**
     * This is for the default listener
     * @param context context to pass for request resources
     * @param appId                 The Facebook app id.
     */
    @Override
    public void handleAuthentication(Context context, String appId) {
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

            GoogleAuthenticationManager.getInstance().onGoogleAuthenticationFailure(obj);
        }
    }

    @Override
    public void onActivityResultCalled(int requestCode, int resultCode, Intent data) {
        if (googleAuthenticationListener == this) {
            // If the error resolution was not successful we should not resolve further.
            if (resultCode != Activity.RESULT_OK) {
                mShouldResolve = false;
            }

            mIsResolving = false;
            mGoogleApiClient.connect();
        }
        else {
            googleAuthenticationListener.onActivityResultCalled(requestCode, resultCode, data);
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
                connectionResult.startResolutionForResult((Activity)ctx, DEFAULT_GOOGLE_AUTHENTICATOR_RSOLVER_ID);
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
            GoogleAuthenticationManager.getInstance().onGoogleAuthenticationFailure(obj);
            this.ctx = null;
        }
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
                GoogleAuthenticationManager.getInstance().onGoogleAuthenticationFailure(obj);
                CombinedGoogleAuthentication.this.ctx = null;
            }
            else {
                logger.debug("google token="+ token);
                GoogleAuthenticationManager.getInstance().onGoogleAccessTokenReceived(token);
                CombinedGoogleAuthentication.this.ctx = null;
            }
        }
    }

    private JSONObject createFailureResponse(String code, String msg) throws JSONException{
        JSONObject obj = new JSONObject();
        obj.put("errorCode", code);
        obj.put("msg", msg);
        return obj;
    }


    @Override
    public void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge, Context context) {

    }

    @Override
    public void onAuthenticationSuccess(Context context, JSONObject info) {

    }

    @Override
    public void onAuthenticationFailure(Context context, JSONObject info) {

    }
}
