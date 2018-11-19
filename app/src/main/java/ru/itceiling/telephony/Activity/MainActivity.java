package ru.itceiling.telephony.Activity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.itceiling.telephony.Broadcaster.CallReceiver;
import ru.itceiling.telephony.Broadcaster.CallbackReceiver;
import ru.itceiling.telephony.Broadcaster.ImportDataReceiver;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

public class MainActivity extends AppCompatActivity {

    CallReceiver callRecv;
    CallbackReceiver callbackReceiver;
    DBHelper dbHelper;
    SQLiteDatabase db;

    private String phoneNumber = "";
    private static String mLastState = "";
    private String date1, date2;
    int callStatus = 0;
    private String TAG = "callReceiver";
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String fileName;
    File audiofile;

    private ImportDataReceiver importDataReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        //registerReceiver();
        registerCallbackReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();

        importDataReceiver = new ImportDataReceiver();

        if (importDataReceiver != null) {
            importDataReceiver.SetAlarm(this);
        }

    }

    public void registerReceiver() {
        callRecv = new CallReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        registerReceiver(callRecv, filter);

    }

    private void registerCallbackReceiver() {

        callbackReceiver = new CallbackReceiver();

        if (callbackReceiver != null) {
            callbackReceiver.SetAlarm(this);
        }

    }

    public void onButtonRecall(View view) {
        Intent intent = new Intent(this, CallbackListActivity.class);
        startActivity(intent);
    }

    public void onButtonClients(View view) {
        Intent intent = new Intent(this, ClientsListActivity.class);
        startActivity(intent);
    }

    public void onButtonAnalytics(View view) {
        Intent intent = new Intent(this, AnalyticsActivity.class);
        startActivity(intent);
    }

    public void onButtonSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
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
                            Manifest.permission.INTERNET},
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
                jsonObject.put("CheckTimeCallback", 10); // для CallbackReceiver
                jsonObject.put("CheckTimeCall", 15);    // для CallReceiver

                SP = getSharedPreferences("JsonCheckTime", MODE_PRIVATE);
                ed = SP.edit();
                ed.putString("", String.valueOf(jsonObject));
                ed.commit();

                SP = getSharedPreferences("link", MODE_PRIVATE);
                ed = SP.edit();
                ed.putString("", "test1");
                ed.commit();

                String sqlQuewy = "SELECT change_time "
                        + "FROM history_import_to_server";
                Cursor c = db.rawQuery(sqlQuewy, new String[]{});
                if (c != null) {
                    if (c.moveToFirst()) {

                    } else {
                        ContentValues values = new ContentValues();
                        values.put(DBHelper.KEY_CHANGE_TIME, "0000-00-00 00:00:00");
                        values.put(DBHelper.KEY_USER_ID, "138");
                        db.insert(DBHelper.HISTORY_IMPORT_TO_SERVER, null, values);
                    }
                }

            }
        } catch (Exception e) {
        }

        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
            registerReceiver(mBatInfoReceiver, new IntentFilter(filter));

        } catch (Exception e) {
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(callRecv);
    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (i.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
                //получаем исходящий номер
                phoneNumber = i.getExtras().getString("android.intent.extra.PHONE_NUMBER");
                callStatus = 2;
            } else if (i.getAction().equals("android.intent.action.PHONE_STATE")) {
                String phone_state = i.getStringExtra(TelephonyManager.EXTRA_STATE);

                if (!phone_state.equals(mLastState)) {
                    mLastState = phone_state;
                    if (phone_state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        //телефон звонит, получаем входящий номер
                        callStatus = 3;
                        phoneNumber = i.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        historyClient();
                    } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                        //телефон находится в режиме звонка (набор номера / разговор)
                        phoneNumber = i.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        date1 = HelperClass.now_date();
                        newClient();
                        recordCall();
                    } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        //телефон находиться в ждущем режиме. Это событие наступает по окончанию разговора, когда мы уже знаем номер и факт звонка
                        timeDifference();
                        date2 = HelperClass.now_date();
                        addHistoryClientCall();
                    }
                }
            }
        }
    };

    void timeDifference() {

        if (this.mediaRecorder != null) {
            this.mediaRecorder.stop();
        }
    }

    void recordCall() {
        Log.d(TAG, "startRecorging");

        //try {
        releaseRecorder();

        String formatDateTime = HelperClass.now_date();

        Log.d(TAG, "recordCall: " + formatDateTime);
        if (audiofile == null) {
            File sampleDir = new File("/storage/emulated/0/" + formatDateTime + ".flac");

            audiofile = sampleDir;
        }

        mediaRecorder = new MediaRecorder();
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer.toLowerCase().contains("samsung")) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        } else {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        }
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audiofile.getAbsolutePath());
        Log.d(TAG, "recordCall: " + audiofile);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
        }
        mediaRecorder.start();

        //} catch (Exception e) {
        //    Log.d(TAG, "recordCall er: 3" + e);
        //}

    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void newClient() {

        phoneNumber = phoneNumber.substring(1, phoneNumber.length());
        int id = 0;
        String sqlQuewy = "SELECT client_id "
                + "FROM rgzbn_gm_ceiling_clients_contacts" +
                " WHERE phone = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{phoneNumber});
        if (c != null) {
            if (c.moveToFirst()) {
                id = c.getInt(c.getColumnIndex(c.getColumnName(0)));
            }
        }
        c.close();

        if (id == 0) {
            Intent resultIntent = new Intent(this, ClientsListActivity.class);
            resultIntent.putExtra("phone", phoneNumber);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            String message = "Данный клиент не найден. Хотите добавить его?";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int notifyID = 1;
                String CHANNEL_ID = "my_channel_01";
                CharSequence name = "1";
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                Notification notification = new Notification.Builder(this)
                        .setAutoCancel(true)
                        .setTicker("Звонок")
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setSmallIcon(R.raw.icon_notif)
                        .addAction(R.raw.plus, "Добавить", resultPendingIntent)
                        .setStyle(new Notification.BigTextStyle().bigText(message))
                        .setContentTitle("Планер звонков")
                        .setContentText(message)
                        .setChannelId(CHANNEL_ID)
                        .setAutoCancel(true)
                        .build();

                NotificationManager mNotificationManager =
                        (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.createNotificationChannel(mChannel);
                mNotificationManager.notify(notifyID, notification);

            } else {
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(this)
                                .setAutoCancel(true)
                                .setTicker("Звонок")
                                .setWhen(System.currentTimeMillis())
                                .setDefaults(Notification.DEFAULT_ALL)
                                .setSmallIcon(R.raw.icon_notif)
                                .addAction(R.raw.plus, "Добавить", resultPendingIntent)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                                .setContentTitle("Планер звонков")
                                .setAutoCancel(true)
                                .setContentText(message);
                Notification notification = builder.build();
                NotificationManager notificationManager = (NotificationManager) this
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(2, notification);
            }
        }
    }

    private void addHistoryClientCall() {

        SharedPreferences SP = this.getSharedPreferences("CheckTimeCallback", MODE_PRIVATE);
        int checkTime = SP.getInt("", 0);

        Date one = null, two = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            one = format.parse(date1);
            two = format.parse(date2);
        } catch (Exception e) {
        }

        long difference = two.getTime() - one.getTime();

        int min = (int) (difference / (60 * 1000)); // миллисекунды / (24ч * 60мин * 60сек * 1000мс)


        if (min == checkTime) {

            phoneNumber = phoneNumber.substring(1, phoneNumber.length());
            int id = 0;
            String sqlQuewy = "SELECT client_id "
                    + "FROM rgzbn_gm_ceiling_clients_contacts" +
                    " WHERE phone = ? ";
            Cursor c = db.rawQuery(sqlQuewy, new String[]{phoneNumber});
            if (c != null) {
                if (c.moveToFirst()) {
                    id = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                }
            }
            c.close();

            String text = "";
            switch (callStatus) {
                case 1:
                    text = "Исходящий недозвон";
                    break;
                case 2:
                    text = "Исходящий дозвон";
                    break;
                case 3:
                    text = "Входящий дозвон";
                    break;
            }

            HelperClass.addHistory(text, this, String.valueOf(id));
        }
    }

    private void historyClient() {

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();
        phoneNumber = phoneNumber.substring(1, phoneNumber.length());
        int id = 0;
        String sqlQuewy = "SELECT client_id "
                + "FROM rgzbn_gm_ceiling_clients_contacts" +
                " WHERE phone = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{phoneNumber});
        if (c != null) {
            if (c.moveToFirst()) {
                id = c.getInt(c.getColumnIndex(c.getColumnName(0)));
            }
        }
        c.close();

        if (id != 0) {
            String message = "";
            sqlQuewy = "SELECT date_time, text "
                    + "FROM rgzbn_gm_ceiling_client_history" +
                    " WHERE client_id = ? " +
                    "order by date_time desc";
            c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id)});
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        message += c.getString(c.getColumnIndex(c.getColumnName(0))) + " ";
                        message += c.getString(c.getColumnIndex(c.getColumnName(1))) + "\n";
                    } while (c.moveToNext());
                }
            }
            c.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int notifyID = 1;
                String CHANNEL_ID = "my_channel_01";
                CharSequence name = "1";
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                Notification notification = new Notification.Builder(this)
                        .setAutoCancel(true)
                        .setTicker("Звонок")
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setSmallIcon(R.raw.icon_notif)
                        .setStyle(new Notification.BigTextStyle().bigText(message))
                        .setContentTitle("Планер звонков")
                        .setContentText(message)
                        .setChannelId(CHANNEL_ID)
                        .setAutoCancel(true)
                        .build();

                NotificationManager mNotificationManager =
                        (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.createNotificationChannel(mChannel);
                mNotificationManager.notify(notifyID, notification);

            } else {
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(this)
                                .setAutoCancel(true)
                                .setTicker("Звонок")
                                .setWhen(System.currentTimeMillis())
                                .setDefaults(Notification.DEFAULT_ALL)
                                .setSmallIcon(R.raw.icon_notif)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                                .setContentTitle("Планер звонков")
                                .setAutoCancel(true)
                                .setContentText(message);
                Notification notification = builder.build();
                NotificationManager notificationManager = (NotificationManager) this
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(2, notification);
            }
        }
    }
}