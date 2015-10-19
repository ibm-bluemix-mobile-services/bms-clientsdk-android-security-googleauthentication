package com.ibm.mobileclientaccess.clientsdk.gauth;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.ibm.mobileclientaccess.clientsdk.android.auth.google.GoogleAuthenticationManager;
import com.ibm.mobileclientaccess.clientsdk.android.auth.google.MyGoogleListener;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;

import org.json.JSONObject;

import java.net.MalformedURLException;

public class Activity2 extends Activity implements
        ResponseListener
{
    private final String backendRoute = "https://ilan1.stage1.mybluemix.net/?subzone=dev";
    private final String backendGUID = "e8998749-4ad7-414a-b2c9-36983d000f62";

    private TextView infoTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity2);

        try {
            //Register to the server with backendroute and GUID
            BMSClient.getInstance().initialize(this, backendRoute,backendGUID);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        MyGoogleListener listener = new MyGoogleListener(this);
        // Register with default delegate
        GoogleAuthenticationManager.getInstance().registerAuthenticationListener(getApplicationContext(), listener);
    }

    @Override
    public void onSuccess(Response response) {

    }

    @Override
    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {

    }
}
