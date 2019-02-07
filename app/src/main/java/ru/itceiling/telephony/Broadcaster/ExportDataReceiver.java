package ru.itceiling.telephony.Broadcaster;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;

import static android.content.Context.MODE_PRIVATE;

public class ExportDataReceiver extends BroadcastReceiver {

    final public static String ONE_TIME = "onetime";
    private static String TAG = "ExportLog";
    static private String domen;
    private static Integer user_id;
    private static Context ctx;
    private static DBHelper dbHelper;
    private static RequestQueue requestQueue;

    static String sendClient = "[", sendClientContacts = "[", sendClientDopContacts = "[", sendClientsStatus = "[",
            sendUsers = "[", sendUsersMap = "[", sendApiPhones = "[", sendClientHistory = "[", sendCallback = "[",
            sendCallStatusHistory = "[", sendClientStatusMap = "[";

    static String checkApiPhones = "[", checkClientHistory = "[", checkCallback = "[", checkCallStatusHistory = "[",
            checkClientsStatus = "[", checkClientStatusMap = "[", checkClientsContacts = "[", checkClientsDopContacts = "[",
            checkUsers = "[", checkUsersMap = "[", checkClient = "[";

    static String jsonDelete = "[", jsonDeleteTable = "", jsonNewUser = "";

    static org.json.simple.JSONObject jsonObjectClient = new org.json.simple.JSONObject();
    static org.json.simple.JSONObject jsonObjectClientContacts = new org.json.simple.JSONObject();
    static org.json.simple.JSONObject jsonObjectClientDopContacts = new org.json.simple.JSONObject();
    static JSONObject jsonObjectUsers = new JSONObject();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "ExportDataReceiver started!");

        ctx = context;
        SharedPreferences SP = ctx.getSharedPreferences("link", MODE_PRIVATE);
        domen = SP.getString("", "");

        int count_line = 0;
        dbHelper = new DBHelper(ctx);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            String sqlQuewy = "SELECT _id, id_old, name_table "
                    + "FROM history_send_to_server ";
            Cursor cursor = db.rawQuery(sqlQuewy,
                    new String[]{});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        count_line++;
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
        }

        if (count_line > 0) {
            try {
                requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
            } catch (Exception e) {
            }
            delete();

            SP = ctx.getSharedPreferences("user_id", MODE_PRIVATE);
            String gager_id = SP.getString("", "");
            user_id = Integer.parseInt(gager_id) * 100000;

            Log.d(TAG, "-------------------------- CLIENTS ------------------------");
            //клиент send
            sendClient = "[";
            String sqlQuewy = "SELECT id_old "
                    + "FROM history_send_to_server " +
                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
            Cursor cursor = db.rawQuery(sqlQuewy,
                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                            "send", "0", "rgzbn_gm_ceiling_clients", "1"});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        try {
                            sqlQuewy = "SELECT * "
                                    + "FROM rgzbn_gm_ceiling_clients " +
                                    "where _id = ?";
                            Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    do {
                                        JSONObject jsonObjectClient = new JSONObject();
                                        for (int j = 0; j < HelperClass.countColumns(ctx, "rgzbn_gm_ceiling_clients"); j++) {
                                            String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                            String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));

                                            if (j == 0) {
                                                status = "android_id";
                                            }
                                            if (status1 == null || status1.equals("") || (status1.equals("null"))) {
                                            } else {
                                                jsonObjectClient.put(status, status1);
                                            }
                                        }
                                        sendClient += String.valueOf(jsonObjectClient) + ",";
                                    } while (c.moveToNext());
                                } else {
                                    db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                            "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                            new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_clients"});
                                }
                            }
                            c.close();
                        } catch (Exception e) {
                        }
                    } while (cursor.moveToNext());
                } else {
                    forClient();
                }
            }
            sendClient = sendClient.substring(0, sendClient.length() - 1) + "]";
            if (sendClient.equals("]")) {
            } else {
                new SendClientData().execute();
            }
            cursor.close();

            Log.d(TAG, "-------------------------- STATUSES CLIENTS ------------------------");
            //клиент send
            sendClientsStatus = "[";
            sqlQuewy = "SELECT id_old "
                    + "FROM history_send_to_server " +
                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
            cursor = db.rawQuery(sqlQuewy,
                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                            "send", "0", "rgzbn_gm_ceiling_clients_statuses", "1"});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        try {
                            sqlQuewy = "SELECT * "
                                    + "FROM rgzbn_gm_ceiling_clients_statuses " +
                                    "where _id = ?";
                            Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    do {
                                        JSONObject jsonObjectClient = new JSONObject();
                                        for (int j = 0; j < HelperClass.countColumns(ctx,
                                                "rgzbn_gm_ceiling_clients_statuses"); j++) {
                                            String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                            String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));

                                            if (j == 0) {
                                                status = "android_id";
                                            }
                                            if (status1 == null || (status1.equals("null"))) {
                                            } else {
                                                jsonObjectClient.put(status, status1);
                                            }
                                        }
                                        sendClientsStatus += String.valueOf(jsonObjectClient) + ",";
                                    } while (c.moveToNext());
                                } else {
                                    db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                            "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                            new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_clients_statuses"});
                                }
                            }
                            c.close();
                        } catch (Exception e) {
                        }
                    } while (cursor.moveToNext());
                } else {
                    checkClientsStatus = "[";
                    sqlQuewy = "SELECT id_new "
                            + "FROM history_send_to_server " +
                            "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                    cursor = db.rawQuery(sqlQuewy,
                            new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                    "check", "0", "rgzbn_gm_ceiling_clients_statuses"});
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            do {
                                try {
                                    jsonObjectUsers = new JSONObject();
                                    String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                    jsonObjectUsers.put("id", id_new);
                                    checkClientsStatus += String.valueOf(jsonObjectUsers) + ",";
                                } catch (Exception e) {
                                }
                            } while (cursor.moveToNext());
                        }
                    }
                    checkClientsStatus = checkClientsStatus.substring(0, checkClientsStatus.length() - 1) + "]";
                    if (checkClientsStatus.equals("]")) {
                    } else {
                        new CheckClientStatus().execute();
                    }
                    cursor.close();

                    Log.d(TAG, "-------------------------- CLIENTS STATUS MAP ------------------------");
                    //клиент send
                    sendClientStatusMap = "[";
                    sqlQuewy = "SELECT id_old "
                            + "FROM history_send_to_server " +
                            "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
                    cursor = db.rawQuery(sqlQuewy,
                            new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                    "send", "0", "rgzbn_gm_ceiling_clients_statuses_map", "1"});
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            do {
                                String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                try {
                                    sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_clients_statuses_map " +
                                            "where _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            do {
                                                JSONObject jsonObject = new JSONObject();
                                                for (int j = 0; j < HelperClass.countColumns(ctx,
                                                        "rgzbn_gm_ceiling_clients_statuses_map"); j++) {
                                                    String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                                    String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));

                                                    if (j == 0) {
                                                        status = "android_id";
                                                    }
                                                    if (status1 == null || (status1.equals("null"))) {
                                                    } else {
                                                        jsonObject.put(status, status1);
                                                    }
                                                }
                                                sendClientStatusMap += String.valueOf(jsonObject) + ",";
                                            } while (c.moveToNext());
                                        } else {
                                            db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                                    "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                                    new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_clients_statuses_map"});
                                        }
                                    }
                                    c.close();
                                } catch (Exception e) {
                                }

                            } while (cursor.moveToNext());
                        } else {
                            checkClientStatusMap = "[";
                            sqlQuewy = "SELECT id_new "
                                    + "FROM history_send_to_server " +
                                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                            cursor = db.rawQuery(sqlQuewy,
                                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                            "check", "0", "rgzbn_gm_ceiling_clients_statuses_map"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {
                                        try {
                                            jsonObjectUsers = new JSONObject();
                                            String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                            jsonObjectUsers.put("id", id_new);
                                            checkClientStatusMap += String.valueOf(jsonObjectUsers) + ",";
                                        } catch (Exception e) {
                                        }
                                    } while (cursor.moveToNext());
                                }
                            }
                            checkClientStatusMap = checkClientStatusMap.substring(0, checkClientStatusMap.length() - 1) + "]";
                            if (checkClientStatusMap.equals("]")) {
                            } else {
                                new CheckClientStatusMap().execute();
                            }
                            cursor.close();
                        }
                    }
                    cursor.close();
                    sendClientStatusMap = sendClientStatusMap.substring(0, sendClientStatusMap.length() - 1) + "]";
                    if (sendClientStatusMap.equals("]")) {
                    } else {
                        new SendClientStatusMap().execute();
                    }
                }
            }
            cursor.close();
            sendClientsStatus = sendClientsStatus.substring(0, sendClientsStatus.length() - 1) + "]";
            if (sendClientsStatus.equals("]")) {
            } else {
                new SendClientStatus().execute();
            }

            Log.d(TAG, "-------------------------- API ------------------------");
            //клиент send
            sendApiPhones = "[";
            sqlQuewy = "SELECT id_old "
                    + "FROM history_send_to_server " +
                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
            cursor = db.rawQuery(sqlQuewy,
                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                            "send", "0", "rgzbn_gm_ceiling_api_phones", "1"});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        try {
                            sqlQuewy = "SELECT * "
                                    + "FROM rgzbn_gm_ceiling_api_phones " +
                                    "where _id = ?";
                            Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    do {
                                        JSONObject jsonObjectClient = new JSONObject();
                                        for (int j = 0; j < HelperClass.countColumns(ctx, "rgzbn_gm_ceiling_api_phones"); j++) {
                                            String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                            String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));

                                            if (j == 0) {
                                                status = "android_id";
                                            }
                                            if (status1 == null || (status1.equals("null"))) {
                                            } else {
                                                jsonObjectClient.put(status, status1);
                                            }
                                        }
                                        sendApiPhones += String.valueOf(jsonObjectClient) + ",";
                                    } while (c.moveToNext());
                                } else {
                                    db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                            "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                            new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_api_phones"});
                                }
                            }
                            c.close();
                        } catch (Exception e) {
                        }
                    } while (cursor.moveToNext());
                } else {
                    checkApiPhones = "[";
                    sqlQuewy = "SELECT id_new "
                            + "FROM history_send_to_server " +
                            "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                    cursor = db.rawQuery(sqlQuewy,
                            new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                    "check", "0", "rgzbn_gm_ceiling_api_phones"});
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            do {
                                try {
                                    jsonObjectUsers = new JSONObject();
                                    String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                    jsonObjectUsers.put("id", id_new);
                                    checkApiPhones += String.valueOf(jsonObjectUsers) + ",";
                                } catch (Exception e) {
                                }
                            } while (cursor.moveToNext());
                        }
                    }
                    checkApiPhones = checkApiPhones.substring(0, checkApiPhones.length() - 1) + "]";
                    if (checkApiPhones.equals("]")) {
                    } else {
                        new CheckApiPhones().execute();
                    }
                    cursor.close();
                }
            }
            cursor.close();

            sendApiPhones = sendApiPhones.substring(0, sendApiPhones.length() - 1) + "]";
            if (sendApiPhones.equals("]")) {
            } else {
                new SendApiPhones().execute();
            }

            Log.d(TAG, "-------------------------- USERS ------------------------");
            //клиент send
            sendUsers = "[";
            sqlQuewy = "SELECT id_old "
                    + "FROM history_send_to_server " +
                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
            cursor = db.rawQuery(sqlQuewy,
                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                            "send", "0", "rgzbn_users", "1"});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        try {
                            sqlQuewy = "SELECT * "
                                    + "FROM rgzbn_users " +
                                    "where _id = ?";
                            Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    do {
                                        jsonObjectUsers = new JSONObject();
                                        for (int j = 0; j < HelperClass.countColumns(ctx, "rgzbn_users"); j++) {
                                            String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                            String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));
                                            if (j == 0) {
                                                status = "android_id";
                                            }

                                            try {
                                                if (status1.equals("") || (status1 == null)) {
                                                } else {
                                                    jsonObjectUsers.put(status, status1);
                                                }
                                            } catch (Exception e) {
                                            }
                                        }
                                        sendUsers += String.valueOf(jsonObjectUsers) + ",";
                                    } while (c.moveToNext());
                                }
                            }
                            c.close();
                        } catch (Exception e) {
                            Log.d(TAG, String.valueOf(e));
                        }
                    } while (cursor.moveToNext());
                } else {
                    //клиент check
                    checkUsers = "[";
                    sqlQuewy = "SELECT id_new "
                            + "FROM history_send_to_server " +
                            "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                    cursor = db.rawQuery(sqlQuewy,
                            new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                    "check", "0", "rgzbn_users"});
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            do {
                                try {
                                    jsonObjectUsers = new JSONObject();
                                    String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                    jsonObjectUsers.put("id", id_new);
                                    checkUsers += String.valueOf(jsonObjectUsers) + ",";
                                } catch (Exception e) {
                                }
                            } while (cursor.moveToNext());
                        }
                    }
                    checkUsers = checkUsers.substring(0, checkUsers.length() - 1) + "]";
                    if (checkUsers.equals("]")) {
                    } else {
                        new CheckUsersData().execute();
                    }
                    cursor.close();

                }
            }
            cursor.close();
            sendUsers = sendUsers.substring(0, sendUsers.length() - 1) + "]";
            if (sendUsers.equals("]") || sendUsers.equals("[]") || sendUsers.equals("[")) {
            } else {
                new SendUsersData().execute();
            }

            Log.d(TAG, "-------------------------- NEW USERS ------------------------");
            //send
            sqlQuewy = "SELECT date "
                    + "FROM history_send_to_server " +
                    "where id_old = 0 and date is not null";
            cursor = db.rawQuery(sqlQuewy, new String[]{});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    jsonNewUser = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                }
            }
            cursor.close();

            if (jsonNewUser.equals("")) {
            } else {
                new SendNewUser().execute();
            }

            Log.d(TAG, "-------------------------- DELETE ------------------------");
            //send
            jsonDelete = "[";
            sqlQuewy = "SELECT id_old, name_table "
                    + "FROM history_send_to_server " +
                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and status=?";
            cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(user_id),
                    String.valueOf(user_id + 999999), String.valueOf(999999), "delete", "0", "1"});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                    String name_table = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(1)));
                    jsonDeleteTable = name_table;
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("id", id_old);
                        jsonDelete += String.valueOf(jsonObject);
                    } catch (Exception e) {
                    }
                }
            }
            cursor.close();

            jsonDelete = jsonDelete.substring(0, jsonDelete.length()) + "]";
            if (jsonDelete.equals("[]")) {
            } else {
                new SendDeleteTable().execute();
            }
        }
    }


    public void SetAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ExportDataReceiver.class);
        intent.putExtra(ONE_TIME, Boolean.FALSE);//Задаем параметр интента
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60, pi);
    }

    public void CancelAlarm(Context context) {
        Intent intent = new Intent(context, ExportDataReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

    static void delete() {

        try {
            SQLiteDatabase db;
            db = dbHelper.getWritableDatabase();

            int count_line = 0;
            String sqlQuewy = "SELECT _id "
                    + "FROM history_send_to_server ";
            Cursor cursor = db.rawQuery(sqlQuewy,
                    new String[]{});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        count_line++;
                    } while (cursor.moveToNext());
                }
            }

            int count_line_sync = 0;
            sqlQuewy = "SELECT _id "
                    + "FROM history_send_to_server " +
                    "where sync=?";
            cursor = db.rawQuery(sqlQuewy, new String[]{"1"});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        count_line_sync++;
                    } while (cursor.moveToNext());
                }
            }

            if (count_line == count_line_sync) {
                db.delete(DBHelper.HISTORY_SEND_TO_SERVER, null, null);
            }
        } catch (Exception e) {
        }
    }

    static void forClient() {

        dbHelper = new DBHelper(ctx);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        checkClient = "[";
        String sqlQuewy = "SELECT id_new "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
        Cursor cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "check", "0", "rgzbn_gm_ceiling_clients"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    jsonObjectClient = new org.json.simple.JSONObject();
                    String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                    jsonObjectClient.put("id", id_new);
                    checkClient += String.valueOf(jsonObjectClient) + ",";
                } while (cursor.moveToNext());
            }
        }
        checkClient = checkClient.substring(0, checkClient.length() - 1) + "]";
        if (checkClient.equals("]")) {
        } else {
            new CheckClientsData().execute();
        }
        cursor.close();

        Log.d(TAG, "-------------------------- CONTACTS ------------------------");
        //контакты send
        sendClientContacts = "[";
        sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_clients_contacts", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                    sqlQuewy = "SELECT * "
                            + "FROM rgzbn_gm_ceiling_clients_contacts " +
                            "where _id = ?";
                    Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                    if (c != null) {
                        if (c.moveToFirst()) {
                            do {
                                jsonObjectClientContacts = new org.json.simple.JSONObject();
                                String status = "android_id";
                                String status1 = c.getString(c.getColumnIndex(c.getColumnName(0)));
                                jsonObjectClientContacts.put(status, status1);
                                status = c.getColumnName(c.getColumnIndex(c.getColumnName(1)));
                                status1 = c.getString(c.getColumnIndex(c.getColumnName(1)));
                                jsonObjectClientContacts.put(status, status1);
                                status = c.getColumnName(c.getColumnIndex(c.getColumnName(2)));
                                status1 = c.getString(c.getColumnIndex(c.getColumnName(2)));
                                jsonObjectClientContacts.put(status, status1);

                                sendClientContacts += String.valueOf(jsonObjectClientContacts) + ",";

                            } while (c.moveToNext());
                        } else {
                            db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                    "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                    new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_clients_contacts"});
                            checkClientsContacts = "[";
                        }
                    }
                    c.close();
                } while (cursor.moveToNext());
            } else {
                // check
                checkClientsContacts = "[";
                sqlQuewy = "SELECT id_new "
                        + "FROM history_send_to_server " +
                        "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                cursor = db.rawQuery(sqlQuewy,
                        new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                "check", "0", "rgzbn_gm_ceiling_clients_contacts"});
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            jsonObjectClientContacts = new org.json.simple.JSONObject();
                            String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                            jsonObjectClientContacts.put("id", id_new);
                            checkClientsContacts += String.valueOf(jsonObjectClientContacts) + ",";
                        } while (cursor.moveToNext());
                    }
                }
                checkClientsContacts = checkClientsContacts.substring(0, checkClientsContacts.length() - 1) + "]";
                if (checkClientsContacts.equals("]")) {
                } else {
                    new CheckClientsContactsData().execute();
                }
                cursor.close();
            }
        }
        sendClientContacts = sendClientContacts.substring(0, sendClientContacts.length() - 1) + "]";
        if (sendClientContacts.equals("]")) {
        } else {
            new SendClientsContactsData().execute();
        }
        cursor.close();

        Log.d(TAG, "-------------------------- DOP CONTACTS ------------------------");
        //контакты send
        sendClientDopContacts = "[";
        sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_clients_dop_contacts", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                    sqlQuewy = "SELECT * "
                            + "FROM rgzbn_gm_ceiling_clients_dop_contacts " +
                            "where _id = ?";
                    Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                    if (c != null) {
                        if (c.moveToFirst()) {
                            do {
                                jsonObjectClientDopContacts = new org.json.simple.JSONObject();
                                String status = "android_id";
                                String status1 = c.getString(c.getColumnIndex(c.getColumnName(0)));
                                jsonObjectClientDopContacts.put(status, status1);
                                status = c.getColumnName(c.getColumnIndex(c.getColumnName(1)));
                                status1 = c.getString(c.getColumnIndex(c.getColumnName(1)));
                                jsonObjectClientDopContacts.put(status, status1);
                                status = c.getColumnName(c.getColumnIndex(c.getColumnName(2)));
                                status1 = c.getString(c.getColumnIndex(c.getColumnName(2)));
                                jsonObjectClientDopContacts.put(status, status1);
                                status = c.getColumnName(c.getColumnIndex(c.getColumnName(3)));
                                status1 = c.getString(c.getColumnIndex(c.getColumnName(3)));
                                jsonObjectClientDopContacts.put(status, status1);
                                status = c.getColumnName(c.getColumnIndex(c.getColumnName(4)));
                                status1 = c.getString(c.getColumnIndex(c.getColumnName(4)));
                                jsonObjectClientDopContacts.put(status, status1);
                                sendClientDopContacts += String.valueOf(jsonObjectClientDopContacts) + ",";

                            } while (c.moveToNext());
                        } else {
                            db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                    "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                    new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_clients_dop_contacts"});
                            checkClientsDopContacts = "[";
                        }
                    }
                    c.close();
                } while (cursor.moveToNext());
            } else {
                // check
                checkClientsDopContacts = "[";
                sqlQuewy = "SELECT id_new "
                        + "FROM history_send_to_server " +
                        "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                cursor = db.rawQuery(sqlQuewy,
                        new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                "check", "0", "rgzbn_gm_ceiling_clients_dop_contacts"});
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            jsonObjectClientDopContacts = new org.json.simple.JSONObject();
                            String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                            jsonObjectClientDopContacts.put("id", id_new);
                            checkClientsDopContacts += String.valueOf(jsonObjectClientDopContacts) + ",";
                        } while (cursor.moveToNext());
                    }
                }
                checkClientsDopContacts = checkClientsDopContacts.substring(0, checkClientsDopContacts.length() - 1) + "]";
                if (checkClientsDopContacts.equals("]")) {
                } else {
                    new CheckClientsDopContactsData().execute();
                }
                cursor.close();
            }
        }
        sendClientDopContacts = sendClientDopContacts.substring(0, sendClientDopContacts.length() - 1) + "]";
        if (sendClientDopContacts.equals("]")) {
        } else {
            new SendClientsDopContactsData().execute();
        }
        cursor.close();

        Log.d(TAG, "-------------------------- CLIENT HISTORY ------------------------");
        //клиент send
        sendClientHistory = "[";
        sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_client_history", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                    try {
                        sqlQuewy = "SELECT * "
                                + "FROM rgzbn_gm_ceiling_client_history " +
                                "where _id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    JSONObject jsonObjectClient = new JSONObject();
                                    for (int j = 0; j < HelperClass.countColumns(ctx,
                                            "rgzbn_gm_ceiling_client_history"); j++) {
                                        String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                        String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));

                                        if (j == 0) {
                                            status = "android_id";
                                        }
                                        if (status1 == null || (status1.equals("null"))) {
                                        } else {
                                            jsonObjectClient.put(status, status1);
                                        }
                                    }

                                    sendClientHistory += String.valueOf(jsonObjectClient) + ",";
                                } while (c.moveToNext());
                            } else {
                                db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                        "id_old = ? and name_table = ? and sync = 0 and type = 'send'",
                                        new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_client_history"});
                            }
                        }
                        c.close();
                    } catch (Exception e) {
                    }
                } while (cursor.moveToNext());
            } else {
                checkClientHistory = "[";
                sqlQuewy = "SELECT id_new "
                        + "FROM history_send_to_server " +
                        "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                cursor = db.rawQuery(sqlQuewy,
                        new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                "check", "0", "rgzbn_gm_ceiling_client_history"});
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            try {
                                jsonObjectUsers = new JSONObject();
                                String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                jsonObjectUsers.put("id", id_new);
                                checkClientHistory += String.valueOf(jsonObjectUsers) + ",";
                            } catch (Exception e) {
                            }
                        } while (cursor.moveToNext());
                    }
                }
                checkClientHistory = checkClientHistory.substring(0, checkClientHistory.length() - 1) + "]";
                if (checkClientHistory.equals("]")) {
                } else {
                    new CheckClientHistory().execute();
                }
                cursor.close();
            }
        }
        cursor.close();
        sendClientHistory = sendClientHistory.substring(0, sendClientHistory.length() - 1) + "]";

        if (sendClientHistory.equals("]")) {
        } else {
            new SendClientHistory().execute();
        }

        Log.d(TAG, "-------------------------- CALLBACK ------------------------");
        //клиент send
        sendCallback = "[";
        sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_callback", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));

                    try {
                        sqlQuewy = "SELECT * "
                                + "FROM rgzbn_gm_ceiling_callback " +
                                "where _id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    JSONObject jsonObjectClient = new JSONObject();
                                    for (int j = 0; j < HelperClass.countColumns(ctx, "rgzbn_gm_ceiling_callback"); j++) {
                                        String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                        String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));

                                        if (j == 0) {
                                            status = "android_id";
                                        }
                                        if (status1 == null || (status1.equals("null"))) {
                                        } else {
                                            jsonObjectClient.put(status, status1);
                                        }
                                    }
                                    sendCallback += String.valueOf(jsonObjectClient) + ",";
                                } while (c.moveToNext());
                            } else {
                                db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                        "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                        new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_callback"});
                            }
                        }
                        c.close();
                    } catch (Exception e) {
                    }

                } while (cursor.moveToNext());
            } else {
                checkCallback = "[";
                sqlQuewy = "SELECT id_new "
                        + "FROM history_send_to_server " +
                        "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                cursor = db.rawQuery(sqlQuewy,
                        new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                "check", "0", "rgzbn_gm_ceiling_callback"});
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            try {
                                jsonObjectUsers = new JSONObject();
                                String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                jsonObjectUsers.put("id", id_new);
                                checkCallback += String.valueOf(jsonObjectUsers) + ",";
                            } catch (Exception e) {
                            }
                        } while (cursor.moveToNext());
                    }
                }
                checkCallback = checkCallback.substring(0, checkCallback.length() - 1) + "]";
                if (checkCallback.equals("]")) {
                } else {
                    new CheckCallback().execute();
                }
                cursor.close();
            }
        }
        cursor.close();
        sendCallback = sendCallback.substring(0, sendCallback.length() - 1) + "]";
        if (sendCallback.equals("]")) {
        } else {
            new SendCallback().execute();
        }

        Log.d(TAG, "-------------------------- CALLS STATUS HISTORY ------------------------");
        //клиент send
        sendCallStatusHistory = "[";
        sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_calls_status_history", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));

                    try {
                        sqlQuewy = "SELECT * "
                                + "FROM rgzbn_gm_ceiling_calls_status_history " +
                                "where _id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    JSONObject jsonObjectClient = new JSONObject();
                                    for (int j = 0; j < HelperClass.countColumns(ctx, "rgzbn_gm_ceiling_calls_status_history"); j++) {
                                        String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                        String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));

                                        if (j == 0) {
                                            status = "android_id";
                                        }
                                        if (status1 == null || (status1.equals("null"))) {
                                        } else {
                                            jsonObjectClient.put(status, status1);
                                        }
                                    }
                                    sendCallStatusHistory += String.valueOf(jsonObjectClient) + ",";
                                } while (c.moveToNext());
                            } else {
                                db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                        "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                        new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_calls_status_history"});
                            }
                        }
                        c.close();
                    } catch (Exception e) {
                    }

                } while (cursor.moveToNext());
            } else {
                checkCallStatusHistory = "[";
                sqlQuewy = "SELECT id_new "
                        + "FROM history_send_to_server " +
                        "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                cursor = db.rawQuery(sqlQuewy,
                        new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                "check", "0", "rgzbn_gm_ceiling_calls_status_history"});
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            try {
                                jsonObjectUsers = new JSONObject();
                                String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                jsonObjectUsers.put("id", id_new);
                                checkCallStatusHistory += String.valueOf(jsonObjectUsers) + ",";
                            } catch (Exception e) {
                            }
                        } while (cursor.moveToNext());
                    }
                }
                checkCallStatusHistory = checkCallStatusHistory.substring(0, checkCallStatusHistory.length() - 1) + "]";
                if (checkCallStatusHistory.equals("]")) {
                } else {
                    new CheckCallStatusHistory().execute();
                }
                cursor.close();
            }
        }
        cursor.close();
        sendCallStatusHistory = sendCallStatusHistory.substring(0, sendCallStatusHistory.length() - 1) + "]";
        if (sendCallStatusHistory.equals("]")) {
        } else {
            new SendCallStatusHistory().execute();
        }
    }


    static class SendClientData extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    if (res.equals("") || res.equals("\"\u041e\u0448\u0438\u0431\u043a\u0430!\"")) {
                        Log.d("sync_app", "SendClientData пусто");
                    } else {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();
                        ContentValues values;
                        String new_id = "";
                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients");
                            for (int i = 0; i < dat.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                new_id = client_contact.getString("new_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, values, "_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_CLIENT_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, values, "client_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_CLIENT_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS, values, "client_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_CLIENT_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, values, "client_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_CLIENT_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, values, "client_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_CLIENT_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, values, "client_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_CLIENT_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, values, "client_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and name_table=? and sync = ?",
                                        new String[]{old_id, "rgzbn_gm_ceiling_clients", "0"});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_OLD, old_id);
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_clients");
                                values.put(DBHelper.KEY_SYNC, "0");
                                values.put(DBHelper.KEY_TYPE, "check");
                                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);

                            }

                        } catch (Exception e) {
                        }

                        forClient();

                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Log.d(TAG, " SEND rgzbn_gm_ceiling_clients: " + sendClient);
                    parameters.put("rgzbn_gm_ceiling_clients", sendClient);
                    return parameters;
                }
            };

            requestQueue.add(request);
            return null;
        }
    }

    static class CheckClientsData extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.CheckDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    if (res.equals("") || res.equals("\u041e\u0448\u0438\u0431\u043a\u0430!")) {
                        Log.d("sync_app", "CheckClientsData пусто");
                    }
                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    try {
                        org.json.JSONObject dat = new org.json.JSONObject(res);
                        JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients");
                        for (int i = 0; i < dat.length(); i++) {

                            org.json.JSONObject client_contact = id_array.getJSONObject(i);
                            String new_id = client_contact.getString("new_android_id");

                            String sqlQuewy = "SELECT * "
                                    + "FROM history_send_to_server " +
                                    "where id_new = ? and type=? and sync=?";
                            Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(new_id), "check", "0"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {

                                        values = new ContentValues();
                                        values.put(DBHelper.KEY_SYNC, "1");
                                        db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_new = ? and sync=?",
                                                new String[]{new_id, "0"});

                                    } while (cursor.moveToNext());
                                }
                            }
                            cursor.close();

                        }

                    } catch (Exception e) {
                    }

                    delete();
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Log.d(TAG, " CHECK rgzbn_gm_ceiling_clients: " + checkClient);
                    parameters.put("rgzbn_gm_ceiling_clients", checkClient);
                    return parameters;
                }
            };

            requestQueue.add(request);

            return null;
        }
    }


    static class SendClientsContactsData extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    if (res.equals("") || res.equals("\u041e\u0448\u0438\u0431\u043a\u0430!")) {
                        Log.d("sync_app", "SendClientsContactsData пусто ");
                    } else {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();
                        ContentValues values = new ContentValues();
                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_contacts");
                            for (int i = 0; i < id_array.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                String sqlQuewy = "SELECT * "
                                        + "FROM history_send_to_server " +
                                        "where id_old = ? and type=? and sync = ? and name_table=?";
                                Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(old_id), "send", "0", "rgzbn_gm_ceiling_clients_contacts"});
                                if (cursor != null) {
                                    if (cursor.moveToFirst()) {
                                        do {

                                            values = new ContentValues();
                                            values.put(DBHelper.KEY_ID, new_id);
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, values,
                                                    "_id = ?", new String[]{old_id});

                                            values = new ContentValues();
                                            values.put(DBHelper.KEY_ID_NEW, new_id);
                                            values.put(DBHelper.KEY_SYNC, "1");
                                            db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and name_table=? and sync = ?",
                                                    new String[]{old_id, "rgzbn_gm_ceiling_clients_contacts", "0"});

                                            values = new ContentValues();
                                            values.put(DBHelper.KEY_ID_OLD, old_id);
                                            values.put(DBHelper.KEY_ID_NEW, new_id);
                                            values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_clients_contacts");
                                            values.put(DBHelper.KEY_SYNC, "0");
                                            values.put(DBHelper.KEY_TYPE, "check");
                                            db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);

                                        } while (cursor.moveToNext());
                                    }
                                }
                                cursor.close();
                            }

                            // check
                            checkClientsContacts = "[";
                            String sqlQuewy = "SELECT id_new "
                                    + "FROM history_send_to_server " +
                                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                            Cursor cursor = db.rawQuery(sqlQuewy,
                                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                            "check", "0", "rgzbn_gm_ceiling_clients_contacts"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {
                                        jsonObjectClientContacts = new org.json.simple.JSONObject();
                                        String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                        jsonObjectClientContacts.put("id", id_new);
                                        checkClientsContacts += String.valueOf(jsonObjectClientContacts) + ",";
                                    } while (cursor.moveToNext());
                                }
                            }
                            checkClientsContacts = checkClientsContacts.substring(0, checkClientsContacts.length() - 1) + "]";
                            if (checkClientsContacts.equals("]")) {
                            } else {
                                new CheckClientsContactsData().execute();
                            }
                            cursor.close();

                        } catch (Exception e) {
                        }
                        //ctx.startService(new Intent(ctx, Service_Sync.class));
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_clients_contacts", sendClientContacts);
                    Log.d(TAG, "SEND rgzbn_gm_ceiling_clients_contacts " + sendClientContacts);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }

    }

    static class CheckClientsContactsData extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.CheckDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    if (res.equals("")) {
                        Log.d(TAG, "CheckClientsContactsData пусто ");
                    }
                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    try {
                        org.json.JSONObject dat = new org.json.JSONObject(res);
                        JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_contacts");
                        for (int i = 0; i < dat.length(); i++) {

                            org.json.JSONObject client_contact = id_array.getJSONObject(i);
                            String new_id = client_contact.getString("new_android_id");

                            String sqlQuewy = "SELECT * "
                                    + "FROM history_send_to_server " +
                                    "where id_new = ? and type=? and name_table=? and sync = ?";
                            Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(new_id), "check", "rgzbn_gm_ceiling_clients_contacts", "0"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {

                                        values = new ContentValues();
                                        values.put(DBHelper.KEY_SYNC, "1");
                                        db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_new = ? and name_table=? and sync=?",
                                                new String[]{new_id, "rgzbn_gm_ceiling_clients_contacts", "0"});

                                    } while (cursor.moveToNext());
                                }
                            }
                            cursor.close();
                        }

                    } catch (Exception e) {
                    }
                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_clients_contacts", checkClientsContacts);
                    Log.d(TAG, "CHECK rgzbn_gm_ceiling_clients_contacts " + checkClientsContacts);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }


    static class SendClientsDopContactsData extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "onResponse: " + res);
                    if (res.equals("") || res.equals("\u041e\u0448\u0438\u0431\u043a\u0430!")) {
                        Log.d("sync_app", "SendClientsContactsData пусто ");
                    } else {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();
                        ContentValues values = new ContentValues();
                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_dop_contacts");
                            for (int i = 0; i < id_array.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                String sqlQuewy = "SELECT * "
                                        + "FROM history_send_to_server " +
                                        "where id_old = ? and type=? and sync = ? and name_table=?";
                                Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(old_id), "send", "0",
                                        "rgzbn_gm_ceiling_clients_dop_contacts"});
                                if (cursor != null) {
                                    if (cursor.moveToFirst()) {
                                        do {

                                            values = new ContentValues();
                                            values.put(DBHelper.KEY_ID, new_id);
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS, values,
                                                    "_id = ?", new String[]{old_id});

                                            values = new ContentValues();
                                            values.put(DBHelper.KEY_ID_NEW, new_id);
                                            values.put(DBHelper.KEY_SYNC, "1");
                                            db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and name_table=? and sync = ?",
                                                    new String[]{old_id, "rgzbn_gm_ceiling_clients_dop_contacts", "0"});

                                            values = new ContentValues();
                                            values.put(DBHelper.KEY_ID_OLD, old_id);
                                            values.put(DBHelper.KEY_ID_NEW, new_id);
                                            values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_clients_dop_contacts");
                                            values.put(DBHelper.KEY_SYNC, "0");
                                            values.put(DBHelper.KEY_TYPE, "check");
                                            db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);

                                        } while (cursor.moveToNext());
                                    }
                                }
                                cursor.close();
                            }

                            // check
                            checkClientsDopContacts = "[";
                            String sqlQuewy = "SELECT id_new "
                                    + "FROM history_send_to_server " +
                                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                            Cursor cursor = db.rawQuery(sqlQuewy,
                                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                            "check", "0", "rgzbn_gm_ceiling_clients_dop_contacts"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {
                                        jsonObjectClientContacts = new org.json.simple.JSONObject();
                                        String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                        jsonObjectClientContacts.put("id", id_new);
                                        checkClientsDopContacts += String.valueOf(jsonObjectClientContacts) + ",";
                                    } while (cursor.moveToNext());
                                }
                            }
                            checkClientsDopContacts = checkClientsDopContacts.substring(0, checkClientsDopContacts.length() - 1) + "]";
                            if (checkClientsDopContacts.equals("]")) {
                            } else {
                                new CheckClientsDopContactsData().execute();
                            }
                            cursor.close();

                        } catch (Exception e) {
                        }
                        //ctx.startService(new Intent(ctx, Service_Sync.class));
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_clients_dop_contacts", sendClientDopContacts);
                    Log.d(TAG, "SEND rgzbn_gm_ceiling_clients_dop_contacts " + sendClientDopContacts);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }

    }

    static class CheckClientsDopContactsData extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.CheckDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    try {
                        org.json.JSONObject dat = new org.json.JSONObject(res);
                        JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_dop_contacts");
                        for (int i = 0; i < dat.length(); i++) {

                            org.json.JSONObject client_contact = id_array.getJSONObject(i);
                            String new_id = client_contact.getString("new_android_id");

                            String sqlQuewy = "SELECT * "
                                    + "FROM history_send_to_server " +
                                    "where id_new = ? and type=? and name_table=? and sync = ?";
                            Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(new_id),
                                    "check", "rgzbn_gm_ceiling_clients_dop_contacts", "0"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {

                                        values = new ContentValues();
                                        values.put(DBHelper.KEY_SYNC, "1");
                                        db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                                "id_new = ? and name_table=? and sync=?",
                                                new String[]{new_id, "rgzbn_gm_ceiling_clients_dop_contacts", "0"});

                                    } while (cursor.moveToNext());
                                }
                            }
                            cursor.close();
                        }

                    } catch (Exception e) {
                    }
                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_clients_dop_contacts", checkClientsDopContacts);
                    Log.d(TAG, "CHECK rgzbn_gm_ceiling_clients_dop_contacts " + checkClientsDopContacts);

                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }


    static class SendClientHistory extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "SendClientHistory " + res);

                    if (res.equals("")) {
                    } else {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_client_history");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                ContentValues values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, values, "_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and type=? and sync=? and name_table=?",
                                        new String[]{String.valueOf(old_id), "send", "0", "rgzbn_gm_ceiling_client_history"});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_OLD, old_id);
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_client_history");
                                values.put(DBHelper.KEY_SYNC, "0");
                                values.put(DBHelper.KEY_TYPE, "check");
                                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);
                            }


                            checkClientHistory = "[";
                            String sqlQuewy = "SELECT id_new "
                                    + "FROM history_send_to_server " +
                                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                            Cursor cursor = db.rawQuery(sqlQuewy,
                                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                            "check", "0", "rgzbn_gm_ceiling_client_history"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {
                                        try {
                                            jsonObjectUsers = new JSONObject();
                                            String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                            jsonObjectUsers.put("id", id_new);
                                            checkClientHistory += String.valueOf(jsonObjectUsers) + ",";
                                        } catch (Exception e) {
                                        }
                                    } while (cursor.moveToNext());
                                }
                            }
                            checkClientHistory = checkClientHistory.substring(0, checkClientHistory.length() - 1) + "]";
                            if (checkClientHistory.equals("]")) {
                            } else {
                                new CheckClientHistory().execute();
                            }
                            cursor.close();


                        } catch (Exception e) {
                        }
                    }
                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_client_history", sendClientHistory);
                    Log.d(TAG, "SEND rgzbn_gm_ceiling_client_history " + sendClientHistory);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

    static class CheckClientHistory extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.CheckDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    try {
                        org.json.JSONObject dat = new org.json.JSONObject(res);
                        JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_client_history");
                        for (int i = 0; i < dat.length(); i++) {

                            org.json.JSONObject client_contact = id_array.getJSONObject(i);
                            String new_id = client_contact.getString("new_android_id");

                            String sqlQuewy = "SELECT * "
                                    + "FROM history_send_to_server " +
                                    "where id_new = ? and type=? and name_table=? and sync=?";
                            Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(new_id),
                                    "check", "rgzbn_gm_ceiling_client_history", "0"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {

                                        values = new ContentValues();
                                        values.put(DBHelper.KEY_SYNC, "1");
                                        db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                                "id_new = ? and name_table=? and sync=?",
                                                new String[]{new_id, "rgzbn_gm_ceiling_client_history", "0"});

                                    } while (cursor.moveToNext());
                                }
                            }
                            cursor.close();
                        }
                    } catch (Exception e) {
                    }

                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override

                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_client_history", checkClientHistory);
                    Log.d(TAG, "CHECK rgzbn_gm_ceiling_client_history " + checkClientHistory);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }


    static class SendCallback extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    if (res.equals("")) {

                    } else {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_callback");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                ContentValues values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, values, "_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and type=? and sync=? and name_table=? and id_new=?",
                                        new String[]{String.valueOf(old_id), "send", "0", "rgzbn_gm_ceiling_callback", "0"});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, values, "_id = ?",
                                        new String[]{String.valueOf(old_id)});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_OLD, old_id);
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_callback");
                                values.put(DBHelper.KEY_SYNC, "0");
                                values.put(DBHelper.KEY_TYPE, "check");
                                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);
                            }

                            checkCallback = "[";
                            String sqlQuewy = "SELECT id_new "
                                    + "FROM history_send_to_server " +
                                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                            Cursor cursor = db.rawQuery(sqlQuewy,
                                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                            "check", "0", "rgzbn_gm_ceiling_callback"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {
                                        try {
                                            jsonObjectUsers = new JSONObject();
                                            String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                            jsonObjectUsers.put("id", id_new);
                                            checkCallback += String.valueOf(jsonObjectUsers) + ",";
                                        } catch (Exception e) {
                                        }
                                    } while (cursor.moveToNext());
                                }
                            }
                            checkCallback = checkCallback.substring(0, checkCallback.length() - 1) + "]";
                            if (checkCallback.equals("]")) {
                            } else {
                                new CheckCallback().execute();
                            }
                            cursor.close();

                        } catch (Exception e) {
                        }
                    }
                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_callback", sendCallback);
                    Log.d(TAG, "SEND rgzbn_gm_ceiling_callback " + sendCallback);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

    static class CheckCallback extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.CheckDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "onResponse: " + res);

                    if (res.equals("") || res.equals("\u041e\u0448\u0438\u0431\u043a\u0430!")) {
                    }
                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    try {
                        org.json.JSONObject dat = new org.json.JSONObject(res);
                        JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_callback");
                        for (int i = 0; i < dat.length(); i++) {

                            org.json.JSONObject client_contact = id_array.getJSONObject(i);
                            String new_id = client_contact.getString("new_android_id");

                            String sqlQuewy = "SELECT * "
                                    + "FROM history_send_to_server " +
                                    "where id_new = ? and type=? and name_table=? and sync=?";
                            Cursor cursor = db.rawQuery(sqlQuewy,
                                    new String[]{String.valueOf(new_id), "check", "rgzbn_gm_ceiling_callback", "0"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {

                                        values = new ContentValues();
                                        values.put(DBHelper.KEY_SYNC, "1");
                                        db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                                "id_new = ? and name_table=? and sync=?",
                                                new String[]{new_id, "rgzbn_gm_ceiling_callback", "0"});

                                    } while (cursor.moveToNext());
                                }
                            }
                            cursor.close();
                        }
                    } catch (Exception e) {
                    }

                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_callback", checkCallback);
                    Log.d(TAG, "CHECK rgzbn_gm_ceiling_callback " + checkCallback);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }


    static class SendClientStatus extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    if (res.equals("")) {

                    } else {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_statuses");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                ContentValues values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, values, "_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and type=? and sync=? and name_table=? and id_new=?",
                                        new String[]{String.valueOf(old_id), "send", "0", "rgzbn_gm_ceiling_clients_statuses", "0"});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, values, "_id = ?",
                                        new String[]{String.valueOf(old_id)});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_OLD, old_id);
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_clients_statuses");
                                values.put(DBHelper.KEY_SYNC, "0");
                                values.put(DBHelper.KEY_TYPE, "check");
                                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);

                                values = new ContentValues();
                                values.put(DBHelper.KEY_STATUS_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, values, "status_id = ?",
                                        new String[]{String.valueOf(old_id)});
                            }

                            checkClientsStatus = "[";
                            String sqlQuewy = "SELECT id_new "
                                    + "FROM history_send_to_server " +
                                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                            Cursor cursor = db.rawQuery(sqlQuewy,
                                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                            "check", "0", "rgzbn_gm_ceiling_clients_statuses"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {
                                        try {
                                            jsonObjectUsers = new JSONObject();
                                            String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                            jsonObjectUsers.put("id", id_new);
                                            checkClientsStatus += String.valueOf(jsonObjectUsers) + ",";
                                        } catch (Exception e) {
                                        }
                                    } while (cursor.moveToNext());
                                }
                            }
                            checkClientsStatus = checkClientsStatus.substring(0, checkClientsStatus.length() - 1) + "]";
                            if (checkClientsStatus.equals("]")) {
                            } else {
                                new CheckClientStatus().execute();
                            }
                            cursor.close();

                        } catch (Exception e) {
                        }
                    }
                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_clients_statuses", sendClientsStatus);
                    Log.d(TAG, "SEND rgzbn_gm_ceiling_clients_statuses " + sendClientsStatus);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

    static class CheckClientStatus extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.CheckDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    if (res.equals("") || res.equals("\u041e\u0448\u0438\u0431\u043a\u0430!")) {
                    }
                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    try {
                        org.json.JSONObject dat = new org.json.JSONObject(res);
                        JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_statuses");
                        for (int i = 0; i < dat.length(); i++) {

                            org.json.JSONObject client_contact = id_array.getJSONObject(i);
                            String new_id = client_contact.getString("new_android_id");

                            String sqlQuewy = "SELECT * "
                                    + "FROM history_send_to_server " +
                                    "where id_new = ? and type=? and name_table=? and sync=?";
                            Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(new_id),
                                    "check", "rgzbn_gm_ceiling_clients_statuses", "0"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {

                                        values = new ContentValues();
                                        values.put(DBHelper.KEY_SYNC, "1");
                                        db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                                "id_new = ? and name_table=? and sync=?",
                                                new String[]{new_id, "rgzbn_gm_ceiling_clients_statuses", "0"});

                                    } while (cursor.moveToNext());
                                }
                            }
                            cursor.close();

                        }
                    } catch (Exception e) {
                    }

                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_clients_statuses", checkClientsStatus);
                    Log.d(TAG, "CHECK rgzbn_gm_ceiling_clients_statuses " + checkClientsStatus);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }


    static class SendCallStatusHistory extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "onResponse: SendCallStatusHistory " + res);
                    if (res.equals("")) {

                    } else {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_calls_status_history");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                ContentValues values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, values, "_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and type=? and sync=? and name_table=? and id_new=?",
                                        new String[]{String.valueOf(old_id), "send", "0", "rgzbn_gm_ceiling_calls_status_history", "0"});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, values, "_id = ?",
                                        new String[]{String.valueOf(old_id)});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_OLD, old_id);
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_calls_status_history");
                                values.put(DBHelper.KEY_SYNC, "0");
                                values.put(DBHelper.KEY_TYPE, "check");
                                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);
                            }

                            checkCallStatusHistory = "[";
                            String sqlQuewy = "SELECT id_new "
                                    + "FROM history_send_to_server " +
                                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                            Cursor cursor = db.rawQuery(sqlQuewy,
                                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                            "check", "0", "rgzbn_gm_ceiling_calls_status_history"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {
                                        try {
                                            jsonObjectUsers = new JSONObject();
                                            String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                            jsonObjectUsers.put("id", id_new);
                                            checkCallStatusHistory += String.valueOf(jsonObjectUsers) + ",";
                                        } catch (Exception e) {
                                        }
                                    } while (cursor.moveToNext());
                                }
                            }
                            checkCallStatusHistory = checkCallStatusHistory.substring(0, checkCallStatusHistory.length() - 1) + "]";
                            if (checkCallStatusHistory.equals("]")) {
                            } else {
                                new CheckCallStatusHistory().execute();
                            }
                            cursor.close();

                        } catch (Exception e) {
                        }
                    }
                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_calls_status_history", sendCallStatusHistory);
                    Log.d(TAG, "SEND rgzbn_gm_ceiling_calls_status_history " + domen + " " + sendCallStatusHistory);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

    static class CheckCallStatusHistory extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.CheckDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    if (res.equals("") || res.equals("\u041e\u0448\u0438\u0431\u043a\u0430!")) {
                    }
                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    try {
                        org.json.JSONObject dat = new org.json.JSONObject(res);
                        JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_calls_status_history");
                        for (int i = 0; i < dat.length(); i++) {

                            org.json.JSONObject client_contact = id_array.getJSONObject(i);
                            String new_id = client_contact.getString("new_android_id");

                            String sqlQuewy = "SELECT * "
                                    + "FROM history_send_to_server " +
                                    "where id_new = ? and type=? and name_table=? and sync=?";
                            Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(new_id),
                                    "check", "rgzbn_gm_ceiling_calls_status_history", "0"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {

                                        values = new ContentValues();
                                        values.put(DBHelper.KEY_SYNC, "1");
                                        db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                                "id_new = ? and name_table=? and sync=?",
                                                new String[]{new_id, "rgzbn_gm_ceiling_calls_status_history", "0"});

                                    } while (cursor.moveToNext());
                                }
                            }
                            cursor.close();
                        }
                    } catch (Exception e) {
                    }

                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_calls_status_history", checkCallStatusHistory);
                    Log.d(TAG, "CHECK rgzbn_gm_ceiling_calls_status_history " + checkCallStatusHistory);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }


    static class SendClientStatusMap extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "SendClientStatusMap: " + res);
                    if (res.equals("")) {

                    } else {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_statuses_map");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                ContentValues values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, values, "_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and type=? and sync=? and name_table=? and id_new=?",
                                        new String[]{String.valueOf(old_id), "send", "0", "rgzbn_gm_ceiling_clients_statuses_map", "0"});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, values, "_id = ?",
                                        new String[]{String.valueOf(old_id)});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_OLD, old_id);
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_clients_statuses_map");
                                values.put(DBHelper.KEY_SYNC, "0");
                                values.put(DBHelper.KEY_TYPE, "check");
                                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);
                            }

                            checkClientStatusMap = "[";
                            String sqlQuewy = "SELECT id_new "
                                    + "FROM history_send_to_server " +
                                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                            Cursor cursor = db.rawQuery(sqlQuewy,
                                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                            "check", "0", "rgzbn_gm_ceiling_clients_statuses_map"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {
                                        try {
                                            jsonObjectUsers = new JSONObject();
                                            String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                            jsonObjectUsers.put("id", id_new);
                                            checkClientStatusMap += String.valueOf(jsonObjectUsers) + ",";
                                        } catch (Exception e) {
                                        }
                                    } while (cursor.moveToNext());
                                }
                            }
                            checkClientStatusMap = checkClientStatusMap.substring(0, checkClientStatusMap.length() - 1) + "]";
                            if (checkClientStatusMap.equals("]")) {
                            } else {
                                new CheckClientStatusMap().execute();
                            }
                            cursor.close();

                        } catch (Exception e) {
                        }
                    }
                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_clients_statuses_map", sendClientStatusMap);
                    Log.d(TAG, "SEND rgzbn_gm_ceiling_clients_statuses_map: " + sendClientStatusMap);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

    static class CheckClientStatusMap extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.CheckDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    try {
                        org.json.JSONObject dat = new org.json.JSONObject(res);
                        JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_statuses_map");
                        for (int i = 0; i < dat.length(); i++) {

                            org.json.JSONObject client_contact = id_array.getJSONObject(i);
                            String new_id = client_contact.getString("new_android_id");

                            String sqlQuewy = "SELECT * "
                                    + "FROM history_send_to_server " +
                                    "where id_new = ? and type=? and name_table=? and sync=?";
                            Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(new_id),
                                    "check", "rgzbn_gm_ceiling_clients_statuses_map", "0"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {
                                        values = new ContentValues();
                                        values.put(DBHelper.KEY_SYNC, "1");
                                        db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                                "id_new = ? and name_table=? and sync=?",
                                                new String[]{new_id, "rgzbn_gm_ceiling_clients_statuses_map", "0"});
                                    } while (cursor.moveToNext());
                                }
                            }
                            cursor.close();
                        }
                    } catch (Exception e) {
                    }
                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_clients_statuses_map", checkClientStatusMap);
                    Log.d(TAG, "CHECK rgzbn_gm_ceiling_clients_statuses_map " + checkClientStatusMap);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

    static class SendApiPhones extends AsyncTask<Void, Void, Void> {
        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {
                    if (res.equals("")) {
                    } else {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_api_phones");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                ContentValues values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_API_PHONES, values, "_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and type=? and sync=? and name_table=? and id_new=?",
                                        new String[]{String.valueOf(old_id), "send", "0", "rgzbn_gm_ceiling_api_phones", "0"});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_OLD, old_id);
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_api_phones");
                                values.put(DBHelper.KEY_SYNC, "0");
                                values.put(DBHelper.KEY_TYPE, "check");
                                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);
                            }

                            checkApiPhones = "[";
                            String sqlQuewy = "SELECT id_new "
                                    + "FROM history_send_to_server " +
                                    "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                            Cursor cursor = db.rawQuery(sqlQuewy,
                                    new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                            "check", "0", "rgzbn_gm_ceiling_api_phones"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {
                                        try {
                                            jsonObjectUsers = new JSONObject();
                                            String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                            jsonObjectUsers.put("id", id_new);
                                            checkApiPhones += String.valueOf(jsonObjectUsers) + ",";
                                        } catch (Exception e) {
                                        }
                                    } while (cursor.moveToNext());
                                }
                            }
                            checkApiPhones = checkApiPhones.substring(0, checkApiPhones.length() - 1) + "]";
                            if (checkApiPhones.equals("]")) {
                            } else {
                                new CheckApiPhones().execute();
                            }
                            cursor.close();

                        } catch (Exception e) {
                        }
                    }
                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_api_phones", sendApiPhones);
                    Log.d(TAG, "SEND rgzbn_gm_ceiling_api_phones " + sendApiPhones);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

    static class CheckApiPhones extends AsyncTask<Void, Void, Void> {
        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.CheckDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "rgzbn_gm_ceiling_api_phones = " + res);

                    if (res.equals("") || res.equals("\u041e\u0448\u0438\u0431\u043a\u0430!")) {
                    }
                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    try {
                        org.json.JSONObject dat = new org.json.JSONObject(res);
                        JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_api_phones");
                        for (int i = 0; i < dat.length(); i++) {

                            org.json.JSONObject client_contact = id_array.getJSONObject(i);
                            String new_id = client_contact.getString("new_android_id");

                            String sqlQuewy = "SELECT * "
                                    + "FROM history_send_to_server " +
                                    "where id_new = ? and type=? and name_table=? and sync=?";
                            Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(new_id), "check", "rgzbn_gm_ceiling_api_phones", "0"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {

                                        values = new ContentValues();
                                        values.put(DBHelper.KEY_SYNC, "1");
                                        db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                                "id_new = ? and name_table=? and sync=?",
                                                new String[]{new_id, "rgzbn_gm_ceiling_api_phones", "0"});

                                    } while (cursor.moveToNext());
                                }
                            }
                            cursor.close();
                        }
                    } catch (Exception e) {
                    }

                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override

                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_gm_ceiling_api_phones", checkApiPhones);
                    Log.d(TAG, "CHECK rgzbn_gm_ceiling_api_phones " + checkApiPhones);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }


    static class SendUsersData extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "SendUsersData " + res);

                    if (res.equals("")) {
                    } else {

                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();
                        ContentValues values = new ContentValues();
                        String new_id = "";
                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
                            JSONArray id_array = dat.getJSONArray("rgzbn_users");
                            for (int i = 0; i < dat.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                new_id = client_contact.getString("new_id");

                                Log.d(TAG, new_id + " " + old_id);

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and name_table=? and sync = ? ",
                                        new String[]{old_id, "rgzbn_users", "0"});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_OLD, old_id);
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_users");
                                values.put(DBHelper.KEY_SYNC, "0");
                                values.put(DBHelper.KEY_TYPE, "check");
                                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update("rgzbn_users", values, "_id = ?",
                                        new String[]{old_id});

                                //клиент check
                                checkUsers = "[";
                                String sqlQuewy = "SELECT id_new "
                                        + "FROM history_send_to_server " +
                                        "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
                                Cursor cursor = db.rawQuery(sqlQuewy,
                                        new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                                                "check", "0", "rgzbn_users"});
                                if (cursor != null) {
                                    if (cursor.moveToFirst()) {
                                        do {
                                            try {
                                                jsonObjectUsers = new JSONObject();
                                                String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                                                jsonObjectUsers.put("id", id_new);
                                                checkUsers += String.valueOf(jsonObjectUsers) + ",";
                                            } catch (Exception e) {
                                            }
                                        } while (cursor.moveToNext());
                                    }
                                }
                                checkUsers = checkUsers.substring(0, checkUsers.length() - 1) + "]";
                                if (checkUsers.equals("]")) {
                                } else {
                                    new CheckUsersData().execute();
                                }
                                cursor.close();

                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_users", sendUsers);
                    Log.d(TAG, "SEND rgzbn_users " + sendUsers);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

    static class CheckUsersData extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.CheckDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "check " + res);
                    if (res.equals("") || res.equals("\u041e\u0448\u0438\u0431\u043a\u0430!")) {
                        Log.d("sync_app", "CheckClientsData пусто");
                    }
                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    try {
                        org.json.JSONObject dat = new org.json.JSONObject(res);
                        JSONArray id_array = dat.getJSONArray("rgzbn_users");
                        for (int i = 0; i < dat.length(); i++) {

                            org.json.JSONObject client_contact = id_array.getJSONObject(i);
                            String new_id = client_contact.getString("new_android_id");

                            String sqlQuewy = "SELECT * "
                                    + "FROM history_send_to_server " +
                                    "where id_new = ? and type=? and sync=?";
                            Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(new_id), "check", "0"});
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    do {

                                        values = new ContentValues();
                                        values.put(DBHelper.KEY_SYNC, "1");
                                        db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                                "id_new = ? and sync=? and name_table = ?",
                                                new String[]{new_id, "0", "rgzbn_users"});

                                    } while (cursor.moveToNext());
                                }
                            }
                            cursor.close();

                        }

                    } catch (Exception e) {
                    }
                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("rgzbn_users", checkUsers);
                    Log.d(TAG, "CHECK rgzbn_users " + checkUsers);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }


    static class SendNewUser extends AsyncTask<Void, Void, Void> {
        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.registerUser";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "registerUser = " + res);

                    if (res.equals("") || res.equals("\u041e\u0448\u0438\u0431\u043a\u0430!")) {
                    }
                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    try {
                        org.json.JSONObject dat = new org.json.JSONObject(res);

                        String old_id = "";
                        String new_id = dat.getString("id");
                        String username = dat.getString("username");

                        String sqlQuewy = "SELECT _id "
                                + "FROM rgzbn_users " +
                                "where username = ? ";
                        Cursor cursor = db.rawQuery(sqlQuewy, new String[]{username});
                        if (cursor != null) {
                            if (cursor.moveToFirst()) {
                                do {
                                    old_id = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));

                                } while (cursor.moveToNext());
                            }
                        }
                        cursor.close();

                        values = new ContentValues();
                        values.put(DBHelper.KEY_ID, new_id);
                        db.update(DBHelper.TABLE_USERS, values,
                                "_id = ?",
                                new String[]{old_id});

                        values = new ContentValues();
                        values.put(DBHelper.KEY_SYNC, "1");
                        db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                "name_table=? and sync = ? ",
                                new String[]{"rgzbn_users_manager", "0"});

                        delete();
                    } catch (Exception e) {
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override

                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("r_data", jsonNewUser);
                    Log.d(TAG, "send r_data " + parameters);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

    static class SendDeleteTable extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.deleteDataFromAndroid";
        Map<String, String> parameters = new HashMap<String, String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    if (res.equals("")) {

                    } else {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();
                        res = res.substring(1, res.length() - 1);
                        try {
                            JSONObject jsonObject = new JSONObject(res);
                            String delete_id = jsonObject.getString("ids");
                            String table = jsonObject.getString("table");

                            ContentValues values = new ContentValues();
                            values.put(DBHelper.KEY_SYNC, "1");
                            db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and type=? and sync=? and name_table=? and id_new=?",
                                    new String[]{String.valueOf(delete_id), "delete", "0", table, "0"});

                        } catch (Exception e) {
                            Log.d(TAG, String.valueOf(e));
                        }
                    }
                    delete();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put(jsonDeleteTable, jsonDelete);
                    Log.d(TAG, "delete" + parameters);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

}