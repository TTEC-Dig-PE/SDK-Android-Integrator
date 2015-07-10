package com.humanify.expertconnect.sample.api.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Custom app action to show settings page.
 */
public class SettingsAction extends AppAction implements Parcelable {
    SettingsAction() {
        super(AppAction.TYPE_SETTINGS);
    }

    private SettingsAction(Parcel in) {
        super(in);
    }

    public static final Creator<SettingsAction> CREATOR = new Creator<SettingsAction>() {
        public SettingsAction createFromParcel(Parcel source) {
            return new SettingsAction(source);
        }

        public SettingsAction[] newArray(int size) {
            return new SettingsAction[size];
        }
    };
}
