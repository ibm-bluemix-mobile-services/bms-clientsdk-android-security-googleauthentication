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
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

import java.io.IOException;

/**
 * Created by iklein on 8/10/15.
 */
public class MCADefaultGoogleAuthenticationHandler implements
        MCAGoogleAuthentication,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    private static final String TAG = "GoogleAuth";

    Activity ctx;

    /* Request code used to invoke sign in user interactions. */
    public static final int RC_SIGN_IN = 0;

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

    public MCADefaultGoogleAuthenticationHandler(Activity ctx) {
        this.ctx = ctx;

        // Build GoogleApiClient with access to basic profile
        mGoogleApiClient = new GoogleApiClient.Builder(ctx)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
//                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(new Scope(Scopes.PROFILE))
                .build();
    }

    @Override
    public void handleAuthentication(String appId) {
        mShouldResolve = true;
        mGoogleApiClient.connect();
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
                connectionResult.startResolutionForResult(ctx, RC_SIGN_IN);
                mIsResolving = true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Could not resolve ConnectionResult.", e);
                mIsResolving = false;
                mGoogleApiClient.connect();
            }
        }
        else {
            MCAGoogleAuthenticationManager.getInstance().onGoogleAuthenticationFailure(null);
        }
    }

    private class RetrieveTokenTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String accountName = params[0];
            String scopes = "oauth2:" + Scopes.PLUS_LOGIN;
//            String scopes = "oauth2:profile email";
            String token = null;
            try {
                token = GoogleAuthUtil.getToken(ctx, accountName, scopes);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (UserRecoverableAuthException e) {
//                startActivityForResult(e.getIntent(), RC_SIGN_IN);
            } catch (GoogleAuthException e) {
                Log.e(TAG, e.getMessage());
            }
            return token;
        }

        @Override
        protected void onPostExecute(String token) {
            super.onPostExecute(token);
            Log.i(TAG, "token = " + token);
            MCAGoogleAuthenticationManager.getInstance().onGoogleAccessTokenReceived(token);
        }
    }
}
