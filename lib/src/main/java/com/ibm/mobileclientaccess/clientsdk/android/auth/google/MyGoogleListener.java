package com.ibm.mobileclientaccess.clientsdk.android.auth.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

/**
 * Created by iklein on 19/10/15.
 */

/**
 * Just checking if this loooks okZ
 */
public class MyGoogleListener implements
        GoogleAuthenticationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{

    Logger logger;
    Activity ctx;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    public MyGoogleListener(Activity ctx) {

        // Build GoogleApiClient with access to basic profile
        mGoogleApiClient = new GoogleApiClient.Builder(ctx)
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
        //start authentication with google
    }

    @Override
    public void onActivityResultCalled(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
