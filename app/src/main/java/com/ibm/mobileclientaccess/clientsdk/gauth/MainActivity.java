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
package com.ibm.mobileclientaccess.clientsdk.gauth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.googleauthentication.GoogleAuthenticationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.api.MCAAuthorizationManager;

import org.json.JSONObject;

import java.net.MalformedURLException;

public class MainActivity extends Activity implements
        ResponseListener
{
    private final String backendRoute = "https://GoogleAndroidTest.mybluemix.net";
    private final String mcaTenantId = "49f8ac9d-5729-4158-aeb8-5ab35ba3e51c";

    private TextView infoTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoTextView = (TextView)findViewById(R.id.info);
        BMSClient.getInstance().initialize(getApplicationContext(), BMSClient.REGION_US_SOUTH);
        BMSClient.getInstance().setAuthorizationManager(MCAAuthorizationManager.createInstance(this, mcaTenantId));
        GoogleAuthenticationManager.getInstance().register(this);
        MCAAuthorizationManager.getInstance().obtainAuthorization(this,this);
        Logger.setSDKDebugLoggingEnabled(true);
    }
    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        GoogleAuthenticationManager.getInstance().onActivityResultCalled(requestCode, responseCode, intent);
    }
    //ResponseListener
    @Override
    public void onSuccess(Response response) {
        final TextView tmpInfo = this.infoTextView;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tmpInfo.setText("Connected to Google - OK");
            }
        });
    }

    @Override
    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
        final TextView tmpInfo = this.infoTextView;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tmpInfo.setText("Connection to Google - Failed");
            }
        });
    }
}

