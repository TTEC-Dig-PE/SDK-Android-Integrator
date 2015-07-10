package com.humanify.expertconnect.sample.api.model;

import android.os.Parcel;

import com.humanify.expertconnect.api.model.action.Action;

/**
 * Custom, app-only actions. These are to show Profile and Settings in the nav drawer.
 */
public abstract class AppAction extends Action {
    public static final String TYPE_PROFILE = "profile";
    public static final String TYPE_SETTINGS = "settings";

    public AppAction(String type) {
        super(type);
    }

    protected AppAction(Parcel in) {
        super(in);
    }
}
