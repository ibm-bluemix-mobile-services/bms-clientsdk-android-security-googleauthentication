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
package com.ibm.mobileclientaccess.clientsdk.android.auth;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.ibm.bms.clientsdk.android.security.api.AuthenticationContext;
import com.ibm.mobileclientaccess.authenticator.R;
import com.ibm.mobileclientaccess.clientsdk.android.auth.google.MCAGoogleAuthenticationManager;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        AuthenticationContext
{
    static final String TAG = "googleauth";
    private TextView info;
    private static final String ACCESS_TOKEN_KEY = "accessToken";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnConnect).setOnClickListener(this);
        info = (TextView) findViewById(R.id.tvStatus);

        // Register with default delegate
        MCAGoogleAuthenticationManager.getInstance().registerWithDefaultAuthenticationHandler(this);
    }
    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        MCAGoogleAuthenticationManager.getInstance().onActivityResultCalled(requestCode, responseCode, intent);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnConnect) {
            MCAGoogleAuthenticationManager.getInstance().onAuthenticationChallengeReceived(this, null);
        }
    }

    @Override
    public void submitAuthenticationChallengeAnswer(JSONObject answer) {
        try {
            info.setText(answer.getString(ACCESS_TOKEN_KEY));
        } catch (JSONException e) {
            info.setText("Error");
            e.printStackTrace();
        }
    }

    @Override
    public void submitAuthenticationChallengeSuccess() {
        info.setText("submitAuthenticationChallengeSuccess called");
    }

    @Override
    public void submitAuthenticationChallengeFailure(JSONObject info) {
        this.info.setText("submitAuthenticationChallengeFailure called");
    }
}
