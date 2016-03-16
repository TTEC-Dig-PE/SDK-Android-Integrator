package com.humanify.expertconnect.sample;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.ExpertConnectConfig;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class MainApplication extends Application {

    public static final String API_ENDPOINT = "http://api.ce03.humanify.com";
    public static final String TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodW1hbmlmeS5jb20iLCJpYXQiOjE0NTYxNzQzMTMsImV4cCI6MTQ4NzcxMDMxNywiYXVkIjoic2VydmljZXMuY3guZm9yZC5jb20uaWRlbnRpdHlEZWxlZ2F0ZSIsInN1YiI6ImtlbkBmb3JkLmNvbSIsImFwaUtleSI6IjgwMjA1N2ZmOWI1YjRlYjdmYmI4ODU2YjZlYjJjYzViIiwiY2xpZW50X2lkIjoiaGVucnkifQ.H2OhXg9WQPg20pvxe9t5mIpkeWsDp6xyjIKp79V7YOU";      // YOUR TOKEN GOES HERE
    public static final String CLIENT_ID = "henry";  // YOUR CLIENT_ID GOES HERE
    public static final String USER_ID = "Guest";

    // breadcrumb configuration
    public static final int CACHE_COUNT = 3;
    public static final int CACHE_TIME = 30; // seconds

    @Override
    public void onCreate() {
        super.onCreate();

        configureWithUserToken(TOKEN);
    }

    private void configureWithUserToken(String userToken) {
        final ExpertConnect expertConnect = ExpertConnect.getInstance(this);
        expertConnect.setClientId(MainApplication.CLIENT_ID);

        if (!TextUtils.isEmpty(userToken)) {
            expertConnect.setConfig(new ExpertConnectConfig()
                    .setMainNavigationClass(SampleActivity.class)
                    .setEndpoint(MainApplication.API_ENDPOINT)
                    .setCacheCount(CACHE_COUNT)
                    .setCacheTime(CACHE_TIME)
                    .setUserIdentityToken(userToken));
        } else {
            // *********************************** Token Provider ***********************************;
            expertConnect.setConfig(new ExpertConnectConfig()
                .setMainNavigationClass(SampleActivity.class)
                .setEndpoint(expertConnect.getEndPoint())
                .setCacheCount(CACHE_COUNT)
                .setCacheTime(CACHE_TIME)
                .setTokenProvider(new ExpertConnectConfig.TokenProvider() {
                    @Override
                    public String token() {
                    String token = getUserSessionToken();
                    if (!TextUtils.isEmpty(token)) {
                        return token;
                    }
                    return null;
                    }
                }));
            // *********************************** Token Provider ***********************************;
        }
    }

    private String getUserSessionToken() {
        final ExpertConnect expertConnect = ExpertConnect.getInstance(this);
        String userId = expertConnect.getIdentityManager().getUserId();
        Uri.Builder builder = Uri.parse(expertConnect.getEndPoint()).buildUpon();
        builder.appendEncodedPath("authServerProxy/v1/tokens/ust");

        String newAuthUrl = Uri.parse(expertConnect.getEndPoint()).buildUpon()
                .appendEncodedPath("authServerProxy/v1/tokens/ust")
                .appendQueryParameter("client_id", expertConnect.getIdentityManager().getClientId())
                .appendQueryParameter("username", (userId == null ? USER_ID : userId))
                .toString();

        Request newAuthRequest = new Request.Builder()
                .url(newAuthUrl)
                .get()
                .build();

        try {
            OkHttpClient client = new OkHttpClient();

            Response newAuthResponse = client.newCall(newAuthRequest).execute();
            if (newAuthResponse.isSuccessful()) {
                String userToken = newAuthResponse.body().string();
                return userToken;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
