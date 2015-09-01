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

import com.ibm.bms.clientsdk.android.core.api.BMSClient;
import com.ibm.bms.clientsdk.android.security.api.AuthenticationContext;
import com.ibm.bms.clientsdk.android.security.api.AuthenticationListener;

import org.json.JSONException;
import org.json.JSONObject;

public class MCAGoogleAuthenticationManager implements
        AuthenticationListener
{
    private static String TAG = "MCAGoogleAuth";
    private static String GOOGLE_REALM = "wl_googleRealm";
    private static final String ACCESS_TOKEN_KEY = "accessToken";

    private MCAGoogleAuthentication googleAuthenticationHandler;
    private AuthenticationContext authContext;

    //singelton
    private static final Object lock = new Object();
    private static volatile MCAGoogleAuthenticationManager instance;
    public static MCAGoogleAuthenticationManager getInstance() {
        MCAGoogleAuthenticationManager r = instance;
        if (r == null) {
            synchronized (lock) {    // While we were waiting for the lock, another
                r = instance;        // thread may have instantiated the object.
                if (r == null) {
                    r = new MCAGoogleAuthenticationManager();
                    instance = r;
                }
            }
        }
        return r;
    }

    private MCAGoogleAuthenticationManager() {

    }

    public void registerWithDefaultAuthenticationHandler(Activity ctx) {
        registerWithAuthenticationHandler(ctx, new MCADefaultGoogleAuthenticationHandler(ctx));
    }

    public void registerWithAuthenticationHandler(Activity ctx, MCAGoogleAuthentication handler) {
        googleAuthenticationHandler = handler;

        //register as authListener
        BMSClient.getInstance().registerAuthenticationListener(GOOGLE_REALM, this);
    }

    public void onActivityResultCalled(int requestCode, int resultCode, Intent data) {
        googleAuthenticationHandler.onActivityResultCalled(requestCode, resultCode, data);
    }

    public void onGoogleAccessTokenReceived(String facebookAccessToken) {
        JSONObject object = new JSONObject();
        try {
            object.put(ACCESS_TOKEN_KEY, facebookAccessToken);
            authContext.submitAuthenticationChallengeAnswer(object);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onGoogleAuthenticationFailure(JSONObject userInfo) {
        authContext.submitAuthenticationChallengeFailure(userInfo);
        authContext = null;
    }

    void setAuthenticationContext(AuthenticationContext authContext) {
        this.authContext = authContext;
    }

    @Override
    public void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge) {
            setAuthenticationContext(authContext);
            googleAuthenticationHandler.handleAuthentication(null);
    }

    @Override
    public void onAuthenticationSuccess(JSONObject info) {
        authContext = null;
    }

    @Override
    public void onAuthenticationFailure(JSONObject info) {
        authContext = null;
    }
}