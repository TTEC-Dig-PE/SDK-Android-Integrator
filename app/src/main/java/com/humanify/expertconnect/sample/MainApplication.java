package com.humanify.expertconnect.sample;

import android.app.Application;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.ExpertConnectConfig;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class MainApplication extends Application {

    public static final String TOKEN_ENDPOINT = "http://api.ce03.humanify.com/identityDelegate/v1/tokens";

    public static final String API_ENDPOINT = "http://api.ce03.humanify.com";
    public static final String TOKEN = "1e189b81-499d-4714-a4cb-0fe22e2d118c"; // YOUR TOKEN GOES HERE

    // breadcrumb configuration
    public static final int CACHE_COUNT = 3;
    public static final int CACHE_TIME = 30; // seconds

    @Override
    public void onCreate() {
        super.onCreate();

        // hard coded token
        configureWithUserToken(TOKEN);

        // identity delegate server
        // new RetrieveUserIdentityTokenTask().execute();
    }

    private void configureWithUserToken(String userToken) {
        if (!TextUtils.isEmpty(userToken)) {
            ExpertConnect.getInstance(this).setConfig(new ExpertConnectConfig()
                    .setMainNavigationClass(SampleActivity.class)
                    .setEndpoint(API_ENDPOINT)
                    .setCacheCount(CACHE_COUNT)
                    .setCacheTime(CACHE_TIME)
                    .setUserIdentityToken(userToken));
        }
    }

    private class RetrieveUserIdentityTokenTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String newAuthUrl = Uri.parse(TOKEN_ENDPOINT).buildUpon()
                    .appendQueryParameter("username", "f")
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

        @Override
        protected void onPostExecute(String userToken) {
            configureWithUserToken(userToken);
        }
    }
}
