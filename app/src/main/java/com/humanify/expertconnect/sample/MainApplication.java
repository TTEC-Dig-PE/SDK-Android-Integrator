package com.humanify.expertconnect.sample;

import android.app.Application;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.ExpertConnectConfig;
import com.urbanairship.UAirship;
import com.urbanairship.push.notifications.DefaultNotificationFactory;

public class MainApplication extends Application {
    //public static final String ENDPOINT_DEV = "http://uldcd-cldap02.ttechenabled.net:8080/";
    public static final String ENDPOINT_DEV = "http://api.humanify.com:8080/";

    static final String CLIENT_ID = "mktwebextc";
    //static final String CLIENT_ID = "horizon";
    static final String CLIENT_SECRET = "secret123";

    private String endpoint;

    @Override
    public void onCreate() {
        super.onCreate();

        endpoint = ENDPOINT_DEV;
        setEndpoint(endpoint);

        final DefaultNotificationFactory notificationFactory = new DefaultNotificationFactory(this);
        notificationFactory.setSmallIconId(R.drawable.ic_notification);

        UAirship.takeOff(this, new UAirship.OnReadyCallback() {
            @Override
            public void onAirshipReady(UAirship uAirship) {
                uAirship.getPushManager().setPushEnabled(true);
                uAirship.getPushManager().setUserNotificationsEnabled(true);
                uAirship.getPushManager().setNotificationFactory(notificationFactory);
            }
        });
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        ExpertConnect.getInstance(this).setConfig(new ExpertConnectConfig()
                .setMainNavigationClass(MainActivity.class)
                .setEndpoint(endpoint)
                .setCredentials(MainApplication.CLIENT_ID, MainApplication.CLIENT_SECRET));
    }
}
