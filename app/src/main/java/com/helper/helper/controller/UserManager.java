/*
 * Copyright (c) 10/15/18 1:50 PM
 * Written by Sungdong Jo
 * Description:
 */

package com.helper.helper.controller;

import android.graphics.Bitmap;

import com.helper.helper.model.User;

import org.json.JSONException;
import org.json.JSONObject;

public class UserManager {
    private final static String TAG = UserManager.class.getSimpleName() + "/DEV";
    private static Bitmap m_userProfileBitmap;
    private static User m_user;

    public static User getUser() { return m_user; }

    public static void setUser(User user) {
        m_user = user;
    }

    public static void setUser(final JSONObject jsonObject) throws JSONException {
        m_user = new User.Builder()
                .email(jsonObject.getString("email"))
                .pw(jsonObject.getString("passwd"))
                .name(jsonObject.getString("name"))
                .phone(jsonObject.getString("phone"))
                .build();
    }

    public static void setUserName(String name) { m_user.setUserName(name); }

    public static void setUserEmail(String email) { m_user.setUserEmail(email); }

    public static void setRidingType(String ridingType) { m_user.setUserRidingType(ridingType); }

    public static void setUserProfileBitmap(Bitmap bitmap) { m_userProfileBitmap = bitmap; }

    public static String getUserEmail() {
        return m_user.getUserEmail();
    }

    public static String getUserName() {
        return m_user.getUserName();
    }

    public static String getUserPhone() {
        return m_user.getUserPhone();
    }

    public static String getUserRidingType() {
        return m_user.getUserRidingType();
    }

    public static Bitmap getUserProfileBitmap() {
        return m_userProfileBitmap;
    }
}
