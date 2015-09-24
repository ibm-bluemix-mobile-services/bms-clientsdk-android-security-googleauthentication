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

import android.content.Context;
import android.content.Intent;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationContext;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class implementation for managing the Google OAuth listener.
 * This class registers to AuthenticationListener in order to handle authentication requests
 */
public class GoogleAuthenticationManager implements
        AuthenticationListener
{
    private Logger logger;

    private GoogleAuthenticationListener googleAuthenticationListener;

    private static String GOOGLE_REALM = "wl_googleRealm";
    private static final String ACCESS_TOKEN_KEY = "accessToken";

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

    /**
     * private constructor for singleton
     */
    private GoogleAuthenticationManager() {
        this.logger = Logger.getInstance(GoogleAuthenticationManager.class.getSimpleName());
    }

    /**
     * Register the default Handler for handling OAuth requests.
     * @param ctx - needed context for Google SDK initialization
     */
    public void registerDefaultAuthenticationListener(Context ctx) {
        registerAuthenticationListener(ctx, new DefaultGoogleAuthenticationListener(ctx));
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
     * When the Google activity ends, it sends a result code, and that result needs to be transferred to the facebook code,
     * @param requestCode the intent request code
     * @param resultCode the result
     * @param data the data (if any)
     */
    public void onActivityResultCalled(int requestCode, int resultCode, Intent data) {
        googleAuthenticationListener.onActivityResultCalled(requestCode, resultCode, data);
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
        googleAuthenticationListener.handleAuthentication(context, null);
    }

    @Override
    public void onAuthenticationSuccess(Context ctx, JSONObject info) {
        authContext = null;
    }

    @Override
    public void onAuthenticationFailure(Context ctx, JSONObject info) {
        authContext = null;
    }
}