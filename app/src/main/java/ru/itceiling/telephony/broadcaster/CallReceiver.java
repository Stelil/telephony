package ru.itceiling.telephony.broadcaster;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;

import static android.content.Context.MODE_PRIVATE;

public class CallReceiver extends BroadcastReceiver {
    static private String phoneNumber = "";
    static private String TAG = "callReceiv";

    static private DBHelper dbHelper;
    static private SQLiteDatabase db;
    static private Context ctx;

    private static String mLastState = "";

    static private long date1, date2;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static int callStatus = 2;

    static private MediaRecorder mediaRecorder;
    static private MediaPlayer mediaPlayer;
    static private String fileName;
    static File audiofile;

    static int notifyID = 0;

    static boolean bool = true, boolHis = false;

    @Override
    public void onReceive(Context context, Intent intent) {

        ctx = context;
        dbHelper = new DBHelper(ctx);
        db = dbHelper.getWritableDatabase();

        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            //получаем исходящий номер
            Log.d(TAG, "onReceive: NEW_OUTGOING_CALL");
            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            telephony.listen(new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, final String number) {
                    super.onCallStateChanged(state, number);
                    phoneNumber = number;
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);

            //phoneNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
            callStatus = 2;
        } else if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            Log.d(TAG, "onReceive: PHONE_STATE");
            String phone_state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (!phone_state.equals(mLastState)) {
                if (TelephonyManager.EXTRA_INCOMING_NUMBER != null) {
                    Log.d(TAG, "onReceive: " + TelephonyManager.EXTRA_INCOMING_NUMBER);
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                }

                if (!boolHis && TelephonyManager.EXTRA_INCOMING_NUMBER != null) {
                    boolHis = true;
                    historyClient();
                }

                mLastState = phone_state;

                if (phone_state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    Log.d(TAG, "onReceive: EXTRA_STATE_RINGING");
                    //телефон звонит, получаем входящий номер
                    callStatus = 3;
                } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    Log.d(TAG, "onReceive: EXTRA_STATE_OFFHOOK");
                    //телефон находится в режиме звонка (набор номера / разговор)
                    //recordCall();
                    date1 = System.currentTimeMillis();
                } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    // телефон находиться в ждущем режиме.
                    // Это событие наступает по окончанию разговора, когда мы уже знаем номер и факт звонка
                    date2 = System.currentTimeMillis();
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    boolHis = false;
                    newClient();
                    addHistoryClientCall();
                }
            }
        }

    }

    /*void timeDifference() {

        if (date2.equals("")) {
        } else {
            if (this.mediaRecorder != null) {
                this.mediaRecorder.stop();
            }
        }
    }*/

    void recordCall() {
        Log.d(TAG, "startRecorging");

        releaseRecorder();

        String formatDateTime = HelperClass.nowDate();

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

    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void newClient() {
        Log.d(TAG, "newClient: " + phoneNumber);

        phoneNumber = HelperClass.phoneEdit(phoneNumber);

        int id = 0;
        String sqlQuewy = "SELECT cc.client_id"
                + " FROM rgzbn_gm_ceiling_clients_contacts as cc" +
                " INNER JOIN rgzbn_gm_ceiling_clients AS c" +
                " ON c._id = cc.client_id " +
                " WHERE cc.phone = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{phoneNumber});
        if (c != null) {
            if (c.moveToFirst()) {
                id = c.getInt(c.getColumnIndex(c.getColumnName(0)));
            }
        }
        c.close();

        if (id == 0) {
            Intent intent = new Intent(ctx, BroadcastNewClient.class);
            intent.putExtra("phone", phoneNumber);
            ctx.sendBroadcast(intent);

            addClient();
        }
    }

    void addClient() {

        bool = false;

        Log.d(TAG, "addClient: ");

        SharedPreferences SP = ctx.getSharedPreferences("dealer_id", MODE_PRIVATE);
        String dealer_id = SP.getString("", "");

        SP = ctx.getSharedPreferences("user_id", MODE_PRIVATE);
        String user_id = SP.getString("", "");

        int maxIdClient = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients",
                ctx, user_id);
        String nowDate = HelperClass.nowDate();
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID, maxIdClient);
        values.put(DBHelper.KEY_CLIENT_NAME, "Неизвестный");
        values.put(DBHelper.KEY_TYPE_ID, "1");
        values.put(DBHelper.KEY_DEALER_ID, dealer_id);
        values.put(DBHelper.KEY_MANAGER_ID, user_id);
        values.put(DBHelper.KEY_CREATED, nowDate);
        values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
        values.put(DBHelper.KEY_API_PHONE_ID, "null");
        values.put(DBHelper.KEY_DELETED_BY_USER, "1");
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, null, values);

        int maxIdContacts = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_contacts",
                ctx, user_id);
        values = new ContentValues();
        values.put(DBHelper.KEY_ID, maxIdContacts);
        values.put(DBHelper.KEY_CLIENT_ID, maxIdClient);
        values.put(DBHelper.KEY_PHONE, phoneNumber);
        values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, null, values);

    }

    private void addHistoryClientCall() {

        SharedPreferences SP = ctx.getSharedPreferences("dealer_id", MODE_PRIVATE);
        String dealer_id = SP.getString("", "");

        int client_id = 0;
        String sqlQuewy = "SELECT client_id "
                + "FROM rgzbn_gm_ceiling_clients_contacts" +
                " WHERE phone = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{phoneNumber});
        if (c != null) {
            if (c.moveToLast()) {
                client_id = c.getInt(c.getColumnIndex(c.getColumnName(0)));
            }
        }
        c.close();

        String stringToParse = "";
        sqlQuewy = "SELECT settings "
                + "FROM rgzbn_users " +
                "WHERE _id = ? ";
        c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    stringToParse = c.getString(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        JSONObject json = null;
        try {
            json = new JSONObject(stringToParse);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*if (client_id != 0) {

            long sec = (date2 - date1) / 1000;
            Log.d(TAG, "addHistoryClientCall: " + sec);

            try {
                Log.d(TAG, "addHistoryClientCall: " + json.getInt("CheckTimeCall"));
                if (sec <= json.getInt("CheckTimeCall")) {
                    String text = "Hедозвон";
                    HelperClass.addHistory(text, ctx, String.valueOf(client_id), bool);
                    HelperClass.addCallsStatusHistory(ctx, client_id, 1, 0, bool);
                } else if (sec >= json.getInt("CheckTimeCall")) {
                    String text = "";
                    switch (callStatus) {
                        case 2:
                            text = "Исходящий дозвон. \nДлина разговора = " + HelperClass.editTimeCall(String.valueOf(sec));
                            break;
                        case 3:
                            text = "Входящий звонок. \nДлина разговора = " + HelperClass.editTimeCall(String.valueOf(sec));
                            break;
                    }
                    HelperClass.addHistory(text, ctx, String.valueOf(client_id), bool);
                    HelperClass.addCallsStatusHistory(ctx, client_id, callStatus, (int) sec, bool);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }*/

        // TODO : выводить окошко, в котором будет выбор типа звонка
        if (client_id != 0) {
            long sec = (date2 - date1) / 1000;
            Log.d(TAG, "addHistoryClientCall: " + sec);
            try {
                if (sec <= json.getInt("CheckTimeCall")) {
                    String text = "Пропущенный";
                    HelperClass.addHistory(text, ctx, String.valueOf(client_id), bool);
                    HelperClass.addCallsStatusHistory(ctx, client_id, 1, 0, bool);
                } else {
                    Intent intent = new Intent(ctx, CallTypeWindow.class);
                    intent.putExtra("id", String.valueOf(client_id));
                    intent.putExtra("bool", bool);
                    ctx.sendBroadcast(intent);
                }
            } catch (JSONException e) {
                Log.d(TAG, "addHistoryClientCall Exception: " + e);
            }
        }

        /*int[] call = getCallDetails();
        try {
            if (call[1] <= json.getInt("CheckTimeCall") || call[0] == 3 || call[0] == 5) {
                String text = "Hедозвон";
                HelperClass.addHistory(text, ctx, String.valueOf(client_id), bool);
                HelperClass.addCallsStatusHistory(ctx, client_id, 1, 0, bool);
            } else if (call[1] >= json.getInt("CheckTimeCall")) {
                String text = "";
                switch (call[0]) {
                    case 1:
                        text = "Исходящий дозвон. \nДлина разговора = " + HelperClass.editTimeCall(String.valueOf(call[1]));
                        break;
                    case 2:
                        text = "Входящий звонок. \nДлина разговора = " + HelperClass.editTimeCall(String.valueOf(call[1]));
                        break;
                }
                HelperClass.addHistory(text, ctx, String.valueOf(client_id), bool);
                HelperClass.addCallsStatusHistory(ctx, client_id, callStatus, call[1], bool);
            }
        } catch (Exception e) {
            Log.d(TAG, "addHistoryClientCall: error " + e);
        }*/

    }

    private void historyClient() {

        Log.d(TAG, "historyClient: " + phoneNumber);

        phoneNumber = HelperClass.phoneEdit(phoneNumber);
        dbHelper = new DBHelper(ctx);
        db = dbHelper.getWritableDatabase();
        int id = 0;
        String sqlQuewy = "SELECT cl._id "
                + "FROM rgzbn_gm_ceiling_clients_contacts as cc " +
                "inner join rgzbn_gm_ceiling_clients as cl " +
                "on cl._id = cc.client_id " +
                "WHERE cc.phone = ? and cl.deleted_by_user = 0";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{phoneNumber});
        if (c != null) {
            if (c.moveToFirst()) {
                id = c.getInt(c.getColumnIndex(c.getColumnName(0)));
            }
        }
        c.close();

        Log.d(TAG, "historyClient: " + id);
        if (id != 0) {
            Intent intent = new Intent(ctx, BroadcastHistoryClient.class);
            intent.putExtra("id", String.valueOf(id));
            ctx.sendBroadcast(intent);
        }
    }

    static private int[] getCallDetails() {
        int[] call = new int[2];
        call[0] = 0;
        call[1] = 0;
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
        } else {
            Cursor managedCursor = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    null,
                    null,
                    null,
                    null);
            managedCursor.moveToLast();
            int requiredNumber = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
            int durations = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
            String phNumber = managedCursor.getString(requiredNumber);
            String dur = managedCursor.getString(durations);
            String type = managedCursor.getString(managedCursor.getColumnIndex(CallLog.Calls.TYPE));
            String date = managedCursor.getString(managedCursor.getColumnIndex(CallLog.Calls.DATE));
            Log.e(TAG, "last position number " + phNumber);
            Log.e(TAG, "last call Duration " + dur);
            Log.e(TAG, "last call type " + type);
            Log.e(TAG, "last call date " + date);
            managedCursor.close();
            call[0] = Integer.valueOf(type);
            call[1] = Integer.valueOf(dur);
        }
        return call;
    }

}