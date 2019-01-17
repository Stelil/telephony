package ru.itceiling.telephony.Activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;

import ru.itceiling.telephony.Broadcaster.CallReceiver;
import ru.itceiling.telephony.Broadcaster.CallbackReceiver;
import ru.itceiling.telephony.Broadcaster.ExportDataReceiver;
import ru.itceiling.telephony.Broadcaster.ImportDataReceiver;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.Fragments.AnalyticsFragment;
import ru.itceiling.telephony.Fragments.CallbackListFragment;
import ru.itceiling.telephony.Fragments.ClientsListFragment;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

public class MainActivity extends AppCompatActivity {

    CallReceiver callRecv;
    CallbackReceiver callbackReceiver;
    DBHelper dbHelper;
    SQLiteDatabase db;

    private String dealer_id;
    private String phoneNumber = "";
    private static String mLastState = "";
    private String date1, date2 = "";
    int callStatus = 0;
    private String TAG = "callReceiver";
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String fileName;
    File audiofile;

    private ImportDataReceiver importDataReceiver;
    private ExportDataReceiver exportDataReceiver;

    private static long back_pressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadFragment(CallbackListFragment.newInstance());

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        registerReceiver();
        registerCallbackReceiver();

        SharedPreferences SP = this.getSharedPreferences("group_id", MODE_PRIVATE);
        String group_id = SP.getString("", "");

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.recall:
                    loadFragment(CallbackListFragment.newInstance());
                    return true;

                case R.id.clients:
                    loadFragment(ClientsListFragment.newInstance());
                    return true;

                case R.id.analytics:
                    loadFragment(AnalyticsFragment.newInstance());
                    return true;
            }
            return false;
        }
    };

    private void loadFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_content, fragment);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SharedPreferences SP = this.getSharedPreferences("group_id", MODE_PRIVATE);
        if (SP.getString("", "").equals("13")) {
            MenuItem item = menu.getItem(1);
            item.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // получим идентификатор выбранного пункта меню
        int id = item.getItemId();
        // Операции для выбранного пункта меню
        switch (id) {
            case R.id.exit:
                if (back_pressed + 2000 > System.currentTimeMillis()) {

                    SharedPreferences SP = getSharedPreferences("dealer_id", MODE_PRIVATE);
                    SharedPreferences.Editor ed = SP.edit();
                    ed.putString("", "");
                    ed.commit();

                    SP = getSharedPreferences("user_id", MODE_PRIVATE);
                    ed = SP.edit();
                    ed.putString("", "");
                    ed.commit();

                    SP = getSharedPreferences("JsonCheckTime", MODE_PRIVATE);
                    ed = SP.edit();
                    ed.putString("", "");
                    ed.commit();

                    SP = getSharedPreferences("enter", MODE_PRIVATE);
                    ed = SP.edit();
                    ed.putString("", "0");
                    ed.commit();

                    SP = getSharedPreferences("group_id", MODE_PRIVATE);
                    ed = SP.edit();
                    ed.putString("", "");
                    ed.commit();

                    callbackReceiver.CancelAlarm(this);

                    ExportDataReceiver exportDataReceiver = new ExportDataReceiver();
                    exportDataReceiver.CancelAlarm(this);

                    ImportDataReceiver importDataReceiver = new ImportDataReceiver();
                    importDataReceiver.CancelAlarm(this);

                    finish();
                    Intent intent = new Intent(this, AuthorizationActivity.class);
                    intent.putExtra("exit", "true");
                    startActivity(intent);

                } else {
                    Toast.makeText(getBaseContext(), "Нажмите ещё раз, для того чтобы выйти из пользователя",
                            Toast.LENGTH_SHORT).show();
                }
                back_pressed = System.currentTimeMillis();

                break;

            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;

            case R.id.addFromPhoneBook:
                intent = new Intent(this, PhoneBookActivity.class);
                startActivity(intent);
                break;

            case R.id.manager:
                intent = new Intent(this, ManagerActivity.class);
                startActivity(intent);
                break;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), "Нажмите ещё раз, для того чтобы закрыть приложение",
                    Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();

        importDataReceiver = new ImportDataReceiver();
        if (importDataReceiver != null) {
            importDataReceiver.SetAlarm(this);
        }

        exportDataReceiver = new ExportDataReceiver();
        if (exportDataReceiver != null) {
            exportDataReceiver.SetAlarm(this);
        }
    }

    public void registerReceiver() {
        callRecv = new CallReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        filter.addAction(Intent.EXTRA_PHONE_NUMBER);
        registerReceiver(callRecv, filter);

    }

    private void registerCallbackReceiver() {
        callbackReceiver = new CallbackReceiver();
        if (callbackReceiver != null)
            callbackReceiver.SetAlarm(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.PROCESS_OUTGOING_CALLS,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAPTURE_AUDIO_OUTPUT,
                            Manifest.permission.READ_CALL_LOG,
                            Manifest.permission.WRITE_CALL_LOG,
                            Manifest.permission.INTERNET,
                            Manifest.permission.READ_CONTACTS},
                    1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}