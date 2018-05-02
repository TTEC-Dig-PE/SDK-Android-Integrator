package com.humanify.expertconnect.sample;

import android.os.Parcel;

import com.humanify.expertconnect.api.model.conversationengine.ChannelOptions;

public class ChatChannelOptions extends ChannelOptions {

    private String department;
    private String userType;

    public ChatChannelOptions(String department, String userType) {
        this.department = department;
        this.userType = userType;
    }

    public String getDepartment() {
        return department;
    }

    public String getUserType() {
        return userType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getDepartment());
        dest.writeString(getUserType());
    }

    public ChatChannelOptions(Parcel in) {
        super(in);
        this.department = in.readString();
        this.userType = in.readString();
    }

    public static final Creator<ChatChannelOptions> CREATOR = new Creator<ChatChannelOptions>() {
        public ChatChannelOptions createFromParcel(Parcel source) {
            return new ChatChannelOptions(source);
        }

        public ChatChannelOptions[] newArray(int size) {
            return new ChatChannelOptions[size];
        }
    };

}
