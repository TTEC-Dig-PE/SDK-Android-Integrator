package com.humanify.expertconnect.sample;

import android.app.Application;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.ExpertConnectConfig;

public class MainApplication extends Application {

    public static final String ENDPOINT_DEV = "http://api.ce03.humanify.com";
    static final String CLIENT_ID = "henry";
    static final String CLIENT_SECRET = "secret123";

//    public static final String ENDPOINT_DEV = "http://api.humanify.com:8080/";
//    static final String CLIENT_ID = "mktwebextc";
//    static final String CLIENT_SECRET = "secret123";

    private String endpoint;

    @Override
    public void onCreate() {
        super.onCreate();

        endpoint = ENDPOINT_DEV;
        setEndpoint(endpoint);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        ExpertConnect.getInstance(this).setConfig(new ExpertConnectConfig()
                .setMainNavigationClass(SampleActivity.class)
                .setEndpoint(endpoint)
                .setCredentials(MainApplication.CLIENT_ID, MainApplication.CLIENT_SECRET));
    }
}
