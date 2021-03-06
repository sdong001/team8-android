/*
 * Copyright (c) 10/15/18 1:54 PM
 * Written by Sungdong Jo
 * Description:
 */

package com.helper.helper.view;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.helper.helper.R;
import com.helper.helper.controller.AddressManager;
import com.helper.helper.controller.BTManager;
import com.helper.helper.controller.DownloadImageTask;
import com.helper.helper.controller.EmergencyManager;
import com.helper.helper.controller.FileManager;
import com.helper.helper.controller.GoogleMapManager;
import com.helper.helper.controller.SharedPreferencer;
import com.helper.helper.controller.SocketManager;
import com.helper.helper.controller.UserManager;
import com.helper.helper.controller.ViewStateManager;
import com.helper.helper.interfaces.BluetoothReadCallback;
import com.helper.helper.interfaces.HttpCallback;
import com.helper.helper.interfaces.ValidateCallback;
import com.helper.helper.model.LEDCategory;
import com.helper.helper.model.MemberList;
import com.helper.helper.model.User;
import com.helper.helper.view.accident.ThresholdActivity;
import com.helper.helper.view.group.GroupActivity;
import com.helper.helper.view.main.myeight.EightFragment;
import com.helper.helper.view.main.myeight.InfoFragment;
import com.helper.helper.view.contact.ContactActivity;
import com.helper.helper.controller.HttpManager;
import com.helper.helper.controller.PermissionManager;
import com.helper.helper.view.popup.PopupActivity;
import com.helper.helper.view.widget.DialogAccident;
import com.snatik.storage.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.socket.client.IO;
import io.socket.client.Socket;

import static android.support.design.widget.TabLayout.*;

public class ScrollingActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        NavigationView.OnNavigationItemSelectedListener,
        EightFragment.OnFragmentInteractionListener,
        InfoFragment.OnFragmentInteractionListener {

    private final static String TAG = ScrollingActivity.class.getSimpleName() + "/DEV";

    private static final int PERMISSION_REQUEST = 267;

    /***************** Define widgtes in view *******************/
    private NavigationView m_navigationView;

    private TabLayout m_tabLayout;
    private ViewPager m_viewPager;
    /************************************************************/

    private Socket m_socket;
    private SweetAlertDialog m_loadingDialog;
    private boolean m_bIsDestroyed;

    public ViewPager getViewPager() {
        return m_viewPager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /** Http Server **/
        HttpManager.setServerURI(getString(R.string.server_uri));

        final Activity activity = this;

        Log.d(TAG, "onCreate: ");

        /******************* Connect widgtes with layout *******************/
        setContentView(R.layout.activity_scrolling);

        RelativeLayout bottomNavLayout = findViewById(R.id.bottomNavigationViewLayout);
        bottomNavLayout.setVisibility(View.GONE);

        ImageView toolbar_option_btn = findViewById(R.id.toolbar_option_btn);
        toolbar_option_btn.setVisibility(View.GONE);

        /** Set Emergency Contacts **/
        if ( EmergencyManager.getEmergencyContacts() == null ) {
            try {
                EmergencyManager.setEmergencycontacts(FileManager.readXmlEmergencyContacts(this));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /** Socket **/
        SocketManager.startSocket(this);

        /** Set User Info **/
        try {
            UserManager.setUser(FileManager.readXmlUserInfo(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (HttpManager.useCollection(activity.getString(R.string.collection_user))) {
            JSONObject reqObject = new JSONObject();

            try {
                reqObject.put(User.KEY_EMAIL, UserManager.getUserEmail());

                HttpManager.requestHttp(reqObject, "", "GET", "", new HttpCallback() {
                    @Override
                    public void onSuccess(JSONArray jsonArray) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                JSONArray groups = obj.getJSONArray(User.KEY_GROUPS);
                                for (int j = 0; j < groups.length(); j++) {
                                    String groupIdx = String.valueOf(groups.get(0));
                                    findGroup(groupIdx);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onError(String err) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity, "Check your network connection", Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        /** Set SharedPreferencer **/
        SharedPreferencer.getSharedPreferencer(this, UserManager.getUserEmail(), MODE_PRIVATE);

        /** Set Shop Data **/
        startInitializeShopData();

        /** ToolBar **/
        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView mTitle = toolbar.findViewById(R.id.toolbar_title);

        setSupportActionBar(toolbar);
        mTitle.setText(toolbar.getTitle());

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        /** Navigation **/
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        m_navigationView = findViewById(R.id.nav_view);
        m_navigationView.setNavigationItemSelectedListener(this);

        View headerView = m_navigationView.getHeaderView(0);

        TextView navUserName = headerView.findViewById(R.id.navUserName);
        navUserName.setText(UserManager.getUserName());
        ImageView navUserProfile = headerView.findViewById(R.id.navUserProfile);

        // TODO: 03/11/2018 set User Profile Bitmap.
//        navUserProfile.setImageBitmap(UserManager.getUserProfileBitmap());
        

        /** Tab **/
        m_tabLayout = findViewById(R.id.tabLayout);
        m_tabLayout.addTab(m_tabLayout.newTab().setText("MY EIGHT"));
        m_tabLayout.addTab(m_tabLayout.newTab().setText("LED"));
        m_tabLayout.addTab(m_tabLayout.newTab().setText("TRACKING"));

        TabPagerAdapter pagerAdapter = new TabPagerAdapter(getSupportFragmentManager(), m_tabLayout.getTabCount());

        m_viewPager = findViewById(R.id.pager);
        m_viewPager.setAdapter(pagerAdapter);
        m_viewPager.addOnPageChangeListener(new TabLayoutOnPageChangeListener(m_tabLayout));
        m_tabLayout.addOnTabSelectedListener(new OnTabSelectedListener() {
            @Override
            public void onTabSelected(Tab tab) {
                Log.d("DEV", "onTabSelected called! posistion : " + tab.getPosition());
                m_viewPager.setCurrentItem(tab.getPosition());
//                m_viewPager.reMeasureCurrentPage(tab.getPosition());
                /*
                if (tab.getPosition() == TAB_STATUS) {

                } else if (tab.getPosition() == TAB_TRACKING) {

                }
                */
            }

            @Override
            public void onTabUnselected(Tab tab) {

            }

            @Override
            public void onTabReselected(Tab tab) {

            }
        });

        /*******************************************************************/

        /******************* Make Listener in View *******************/

        /*************************************************************/


        /** Request permissions **/
        if (!PermissionManager.checkPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                !PermissionManager.checkPermissions(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

            /* Result about user selection -> onActivityResult in ScrollActivity */
            PermissionManager.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.SEND_SMS}, PERMISSION_REQUEST);
        }

        /** Internal FileStorage **/
        Storage storage = new Storage(getApplicationContext());
        String path = storage.getInternalFilesDirectory();
        String newDir = path + File.separator + "user_data";

        boolean dirExists = storage.isDirectoryExists(path);

        if (!dirExists) {
            storage.createDirectory(newDir);
        }

        /** GoogleMap **/
        GoogleMapManager.initGoogleMap(this);

        /** Bluetooth  **/
        /**************************************************************/
        BluetoothReadCallback emergencyCallback = new BluetoothReadCallback() {
            @Override
            public void onResult(String result) {
                if (result.equals(BTManager.BT_SIGNAL_EMERGENCY)) {
                    if (EmergencyManager.getEmergencyAlertState() ||
                            !PermissionManager.checkPermissions(activity, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                            !PermissionManager.checkPermissions(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        return;
                    }

                    EmergencyManager.setEmergencyAlertState(true);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Location accLocation = GoogleMapManager.getCurLocation();
                            AddressManager.startAddressIntentService(activity, accLocation);

                            if (m_bIsDestroyed) {
                                Intent intent = new Intent(activity, PopupActivity.class);
                                startActivity(intent);
                            } else {
                                DialogAccident dialogAccident = new DialogAccident(activity, false);
                                dialogAccident.showDialog();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String result) {

            }
        };

        BTManager.setActivityReadCb(emergencyCallback);
    }

    private void findGroup(final String groupIdx) {
        final Activity activity = this;

        if (HttpManager.useCollection(activity.getString(R.string.collection_group))) {
            JSONObject reqObject = new JSONObject();

            try {
                reqObject.put("index", groupIdx);

                HttpManager.requestHttp(reqObject, "", "GET", "", new HttpCallback() {
                    @Override
                    public void onSuccess(JSONArray jsonArray) throws JSONException {
                        JSONObject object = (JSONObject)jsonArray.get(0);

                        String idx = object.getString(MemberList.KEY_INDEX);

                        JSONArray members = object.getJSONArray(MemberList.KEY_MEMBERS);
                        String membersStr = members.toString();
                        membersStr = membersStr.replace("[","").replace("]","");
                        List<String> listNames = Arrays.asList(membersStr.split(","));

                        String masterStr = object.getString(MemberList.KEY_MASTER);
                        final MemberList memberList = new MemberList(listNames, masterStr, idx);

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                /** Socket **/
                                SocketManager.makePatternSyncListenter(memberList);
                            }
                        });
                    }

                    @Override
                    public void onError(String err) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity, "Check your network connection", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void startInitializeShopData() {
        if( HttpManager.useCollection(getString(R.string.collection_category)) ) {

            JSONObject reqObject = new JSONObject();

            try {
                HttpManager.requestHttp(reqObject, "", "GET", "", new HttpCallback() {
                    @Override
                    public void onSuccess(JSONArray existIdjsonArray) throws JSONException {
                        int arrLen = existIdjsonArray.length();
                        // write category xml file
                        List<LEDCategory> ledCategoryList = new ArrayList<>();

                        for (int i = 0; i < existIdjsonArray.length(); i++) {
                            JSONObject categoryObj = existIdjsonArray.getJSONObject(i);

                            LEDCategory category = new LEDCategory(
                                    categoryObj.getString("name"),
                                    categoryObj.getString("backgroundColor"),
                                    categoryObj.getString("notice"),
                                    categoryObj.getString("character")
                            );
                            ledCategoryList.add(category);
                        }
                        try {
                            FileManager.writeXmlCategory(getApplicationContext(), ledCategoryList);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(String err) {
                        Log.d(TAG, "startInitializeShopData onError: " + err);
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /** Dialog **/
    private SweetAlertDialog makeLoadingDialog() {
        SweetAlertDialog dlg = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dlg.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dlg.setTitleText(getString(R.string.loading_dialog_user_data));
        dlg.setCancelable(false);
        return dlg;
    }

    /** Result handler **/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case BTManager.SUCCESS_BLUETOOTH_CONNECT:
                Toast.makeText(this, "디바이스 블루투스 연결 성공", Toast.LENGTH_SHORT).show();
                break;

            case BTManager.FAIL_BLUETOOTH_CONNECT:
                break;

            case BTManager.REQUEST_ENABLE_BT:
                BTManager.initBluetooth(this);
                break;

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST:
                if (!PermissionManager.checkPermissions(
                        this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                        !PermissionManager.checkPermissions(
                                this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(this, getString(R.string.not_grant_location_permission), Toast.LENGTH_SHORT).show();
                }

                if (!PermissionManager.checkPermissions(
                        this, Manifest.permission.SEND_SMS)) {
                    Toast.makeText(this, getString(R.string.not_grant_contacts_permission), Toast.LENGTH_SHORT).show();
                }

                Toast.makeText(this, "권한요청 프로세스 완료", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /** GoogleMap callback **/

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /** Life cycle **/
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: ");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        m_bIsDestroyed = false;
        Log.d(TAG, "onResume: ");

        TabLayout.Tab tab = m_tabLayout.getTabAt(ViewStateManager.getSavedTabPosition());
        if (tab != null) {
            tab.select();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        ViewStateManager.saveTabPosition(m_tabLayout.getSelectedTabPosition());
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_bIsDestroyed = true;
        Log.d(TAG, "onDestroy: ");
    }

    /** Navigation **/
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /** Top-Right control **/
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        m_navigationView.getMenu().findItem(id).setCheckable(false);
        m_navigationView.getMenu().findItem(id).setChecked(false);

        Intent intent;
        switch (id) {
            case R.id.nav_emergencyContacts:
                intent = new Intent(this, ContactActivity.class);
                startActivity(intent);

                break;
            case R.id.nav_accidentThreshold:
                if( !BTManager.getConnected() ) {
                    new SweetAlertDialog(this)
                            .setTitleText("Sorry!")
                            .setContentText("Please pair with your EIGHT.")
                            .setConfirmButton("Close", new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    sweetAlertDialog.dismissWithAnimation();
                                }
                            })
                            .show();

                } else {
                    intent = new Intent(this, ThresholdActivity.class);
                    startActivity(intent);
                }
                break;

            case R.id.nav_group:
                intent = new Intent(this, GroupActivity.class);
                startActivity(intent);
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void messageFromParentFragment(Uri uri) {
        Log.i("TAG", "received communication from parent fragment");
    }

    @Override
    public void messageFromChildFragment(Uri uri) {
        Log.i("TAG", "received communication from child fragment");
    }
}

