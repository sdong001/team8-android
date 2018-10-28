/*
 * Copyright (c) 10/17/18 2:45 PM
 * Written by Sungdong Jo
 * Description:
 */

package com.helper.helper.controller;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import com.helper.helper.R;
import com.helper.helper.interfaces.HttpCallback;
import com.helper.helper.interfaces.ValidateCallback;
import com.helper.helper.model.ContactItem;
import com.helper.helper.model.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmergencyManager {
    private final static String TAG = EmergencyManager.class.getSimpleName() + "/DEV";
    private static final int EMERGENCY_LOCATION_WATING_TIME = 240000; // 4min
    private static final int EMERGENCY_LOCATION_DISTANCE_RANGE = 50; // 50m

    public static final int EMERGENCY_VALIDATE_LOCATION_WATING_FINISH = 901;

    private static Location m_accLocation;
    private static boolean m_bIsAccidentProcessing;

    public static void setAccLocation(Location accLocation) {
        m_accLocation = accLocation;
    }

    public static Location getAccLocation() {
        return m_accLocation;
    }

    public static void startValidationAccident(final ValidateCallback callback) {
        if( m_bIsAccidentProcessing ) { return; }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onDone(EMERGENCY_VALIDATE_LOCATION_WATING_FINISH);
                    m_bIsAccidentProcessing = false;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, EMERGENCY_LOCATION_WATING_TIME);

        m_bIsAccidentProcessing = true;
    }

    public static boolean validateLocation(Location curLocation) {
        if( curLocation.distanceTo(m_accLocation) < EMERGENCY_LOCATION_DISTANCE_RANGE ) {
            return true;
        }

        return false;
    }

    public static void startEmergencyProcess(Context context) throws JSONException {
        /** Send SMS to EmergencyContacts **/
        List<ContactItem> contactItems;

        Location accLocation = UserManager.getUser().getUserPosition();

        final String strSMS1 = context.getString(R.string.sms_content) + "\n\n" + AddressManager.getConvertLocationToAddress();
        final String strSMS2 = "https://google.com/maps?q=" + accLocation.getLatitude() + "," + accLocation.getLongitude();

        try {
            contactItems = FileManager.readXmlEmergencyContacts(context);
            for(ContactItem item:
                    contactItems) {
                SMSManager.sendMessage(context, item.getPhoneNumber(), strSMS1);
                SMSManager.sendMessage(context, item.getPhoneNumber(), strSMS2);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        /** Insert data in server **/
        insertAccidentinServer(UserManager.getUser(), accLocation);
    }

    public static void insertAccidentinServer(User user, Location accLocation) throws JSONException {
        JSONObject locationObject = new JSONObject();
        locationObject.put("latitude", accLocation.getLatitude());
        locationObject.put("longitude", accLocation.getLongitude());

        if (HttpManager.useCollection("accident")) {
            JSONObject reqObject = new JSONObject();
            reqObject.put("user_id", user.getUserEmail());
            reqObject.put("riding_type", user.getUserRidingType());

            Date occDate = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.KOREA);

            String str = sdf.format(occDate);

            reqObject.put("occured_date", sdf.format(occDate));
            reqObject.put("position", locationObject);


            HttpManager.requestHttp(reqObject, "POST", new HttpCallback() {
                @Override
                public void onSuccess(JSONArray jsonArray) throws JSONException {
                    Log.d(TAG, "insertAccidentinServer: onSuccess!");
                }

                @Override
                public void onError(String err) throws JSONException {

                }
            });
        }
    }
}
