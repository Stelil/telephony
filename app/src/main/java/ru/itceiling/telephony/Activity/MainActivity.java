package ru.itceiling.telephony.Activity;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import ru.itceiling.telephony.Broadcaster.CallReceiver;
import ru.itceiling.telephony.Broadcaster.CallbackReceiver;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.R;

public class MainActivity extends AppCompatActivity {

    CallReceiver callRecv;
    CallbackReceiver callbackReceiver;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        registerReceiver();
        registerCallbackReceiver();
    }

    public void registerReceiver(){
        callRecv = new CallReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        registerReceiver(callRecv, filter);

    }

    private void registerCallbackReceiver(){

        callbackReceiver = new CallbackReceiver();

        if (callbackReceiver != null) {
            callbackReceiver.SetAlarm(this);
        }

    }

    public void onButtonRecall(View view){

        Intent intent = new Intent(this, CallbackListActivity.class);
        startActivity(intent);
    }

    public void onButtonClients(View view){

        Intent intent = new Intent(this, ClientsListActivity.class);
        startActivity(intent);
    }

    public void onButtonAnalytics(View view){

    }

    public void onButtonSettings(View view){

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.PROCESS_OUTGOING_CALLS,
                            Manifest.permission.READ_PHONE_STATE},
                    1);
        }

        try {
            SharedPreferences SP = this.getSharedPreferences("enter", MODE_PRIVATE);
            Log.d("logd", "onStart: " + SP.getString("", ""));
            if (SP.getString("", "").equals("1")) {
            } else {
                SP = getSharedPreferences("dealer_id", MODE_PRIVATE);
                SharedPreferences.Editor ed = SP.edit();
                ed.putString("", "138");
                ed.commit();

                SP = getSharedPreferences("enter", MODE_PRIVATE);
                ed = SP.edit();
                ed.putString("", "1");
                ed.commit();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("CheckTimeCallback", 10);
                jsonObject.put("CheckTimeCall", 15);

                SP = getSharedPreferences("JsonCheckTime", MODE_PRIVATE);
                ed = SP.edit();
                ed.putString("", String.valueOf(jsonObject));
                ed.commit();
            }
        }catch (Exception e){
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(callRecv);
    }

}