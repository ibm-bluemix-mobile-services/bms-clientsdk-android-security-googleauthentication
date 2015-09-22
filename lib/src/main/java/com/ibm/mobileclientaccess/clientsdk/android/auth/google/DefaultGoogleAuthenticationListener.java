/*
   Copyright 2015 IBM Corp.
    Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
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
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by iklein on 8/10/15.
 */
public class DefaultGoogleAuthenticationListener implements
        GoogleAuthenticationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    private Logger logger;
    private Activity ctx;

    /* Request code used to invoke sign in user interactions. */
    public static final int DEFAULT_GOOGLE_AUTHENTICATOR_RSOLVER_ID = 1234567890;

    public static final String AUTH_CANCEL_CODE = "100";
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

    /**
     *
     * @param context - need to pass the ApplicationContext for initializing the googleApiClient since it is not a singelton
     */
    public DefaultGoogleAuthenticationListener(Context context) {
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
        // If the error resolution was not successful we should not resolve further.
        if (resultCode != Activity.RESULT_OK) {
            mShouldResolve = false;
        }

        mIsResolving = false;
        mGoogleApiClient.connect();
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
                DefaultGoogleAuthenticationListener.this.ctx = null;
            }
            else {
                logger.debug("google token="+ token);
                GoogleAuthenticationManager.getInstance().onGoogleAccessTokenReceived(token);
                DefaultGoogleAuthenticationListener.this.ctx = null;
            }
        }
    }

    private JSONObject createFailureResponse(String code, String msg) throws JSONException{
        JSONObject obj = new JSONObject();
        obj.put("errorCode", code);
        obj.put("msg", msg);
        return obj;
    }
}
