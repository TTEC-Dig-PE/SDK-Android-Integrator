package com.humanify.expertconnect.sample.api.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Custom app action to show the profile page.
 */
public class ProfileAction extends AppAction implements Parcelable {
    public ProfileAction() {
        super(TYPE_PROFILE);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o.getClass().equals(ProfileAction.class))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    private ProfileAction(Parcel in) {
        super(in);
    }

    public static final Creator<ProfileAction> CREATOR = new Creator<ProfileAction>() {
        public ProfileAction createFromParcel(Parcel source) {
            return new ProfileAction(source);
        }

        public ProfileAction[] newArray(int size) {
            return new ProfileAction[size];
        }
    };
}
