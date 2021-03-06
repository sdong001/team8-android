/*
 * Copyright (c) 10/17/18 2:26 PM
 * Written by Sungdong Jo
 * Description:
 */

package com.helper.helper.controller;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.telephony.SmsManager;
import android.util.Log;

import com.helper.helper.R;
import com.helper.helper.model.ContactItem;
import com.helper.helper.view.ScrollingActivity;

import java.util.List;

public class SMSManager {

    private static final String SENT = "SMS_SENT_ACTION";
    private static final String DELIVERED = "SMS_DELIVERED_ACTION";

    private final static String TAG = ScrollingActivity.class.getSimpleName() + "/DEV";


    public static void sendEmergencyMessages(Context context, List<ContactItem> list, Location location, String address) {
        if( list == null ) { return; }
        if( list.size() == 0 ) { return; }

        String strMessage = context.getString(R.string.sms_content) + "\n\n" + address;
        String strGoogleMap = "https://google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude();

        for (ContactItem contact :
                list) {
            sendMessage(context, contact.getPhoneNumber(), strMessage);
            sendMessage(context, contact.getPhoneNumber(), strGoogleMap);
        }
    }

    public static void sendMessage(Context context, String receiveNumber, String message) {
        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0, new Intent(DELIVERED), 0);

        SmsManager sms = SmsManager.getDefault();
        if (sms == null) {
            Log.e(TAG, "SmsManager is null");
            return;
        }
        sms.sendTextMessage(receiveNumber, "ME", message, sentPI, deliveredPI);
        //sms.sendTextMessage(num, null, txt, null, null);
    }
}

/*
private final Handler m_handle = new Handler() {


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
//                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {

                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
//                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
//                    Toast.makeText(getApplicationContext(), "Connected to "
//                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
//                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
//                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

 */
