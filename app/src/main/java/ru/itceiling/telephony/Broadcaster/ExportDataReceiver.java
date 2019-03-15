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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
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

    static String jsonNewUser = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "ExportDataReceiver started!");

        ctx = context;
        SharedPreferences SP = ctx.getSharedPreferences("link", MODE_PRIVATE);
        domen = SP.getString("", "");

        int count_line = 0;
        if (HelperClass.isOnline(context)) {
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
        }

        if (count_line > 0) {
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());

            SP = ctx.getSharedPreferences("user_id", MODE_PRIVATE);
            String gager_id = SP.getString("", "");
            user_id = Integer.parseInt(gager_id) * 100000;

            try {
                if (firstRequest() == 0) {
                    if (secondRequest() == 0) {
                        if (thirdRequest() == 0) {
                            newUser();
                            deleteTable();
                            checkRequest();
                        }
                    }
                }
                delete();
            } catch (Exception e) {
            }
        }
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

    static Map<String, String> parameters;

    static void newUser() {
        parameters = new LinkedHashMap<>();
        dbHelper = new DBHelper(ctx);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        Log.d(TAG, "-------------------------- NEW USERS ------------------------");
        //send
        String sqlQuewy = "SELECT date "
                + "FROM history_send_to_server " +
                "where id_old = 0 and date is not null";
        Cursor cursor = db.rawQuery(sqlQuewy, new String[]{});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                jsonNewUser = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
            }
        }
        cursor.close();

        if (jsonNewUser.equals("")) {
        } else {
            parameters.put("data", HelperClass.encrypt(jsonNewUser, ctx));
            new SendNewUser().execute();
        }
    }

    static Map<String, String> parametersFirstRequest;

    static Integer firstRequest() {

        parametersFirstRequest = new LinkedHashMap<>();
        dbHelper = new DBHelper(ctx);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        JSONObject jsonObjectFirstRequest = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONArray jsonArrayFirstRequest = new JSONArray();
        Log.d(TAG, "-------------------------- CLIENTS ------------------------");
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
                        jsonObjectFirstRequest.put("table_name", "rgzbn_gm_ceiling_clients");
                        sqlQuewy = "SELECT * "
                                + "FROM rgzbn_gm_ceiling_clients " +
                                "where _id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    JSONObject jsonObject = new JSONObject();
                                    for (int j = 0; j < HelperClass.countColumns(ctx, "rgzbn_gm_ceiling_clients"); j++) {
                                        String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                        String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));

                                        if (j == 0) {
                                            status = "android_id";
                                        }
                                        if (status1 == null || status1.equals("") || status1.equals("null") || status.equals("change_time")) {
                                        } else {
                                            jsonObject.put(status, status1);
                                        }
                                    }
                                    jsonArray.put(jsonObject);
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
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectFirstRequest.put("rows", jsonArray);
                jsonArrayFirstRequest.put(jsonObjectFirstRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        jsonObjectFirstRequest = new JSONObject();
        jsonArray = new JSONArray();
        Log.d(TAG, "-------------------------- USERS ------------------------");
        //клиент send
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
                        jsonObjectFirstRequest.put("table_name", "rgzbn_users");
                        sqlQuewy = "SELECT * "
                                + "FROM rgzbn_users " +
                                "where _id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    JSONObject jsonObject = new JSONObject();
                                    for (int j = 0; j < HelperClass.countColumns(ctx, "rgzbn_users"); j++) {
                                        String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                        String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));
                                        if (j == 0) {
                                            status = "android_id";
                                        }

                                        try {
                                            if (status1.equals("") || (status1 == null) || status.equals("change_time")) {
                                            } else {
                                                jsonObject.put(status, status1);
                                            }
                                        } catch (Exception e) {
                                        }
                                    }
                                    //sendUsers += String.valueOf(jsonObjectUsers) + ",";
                                    jsonArray.put(jsonObject);
                                } while (c.moveToNext());
                            }
                        }
                        c.close();
                    } catch (Exception e) {
                        Log.d(TAG, String.valueOf(e));
                    }
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectFirstRequest.put("rows", jsonArray);
                jsonArrayFirstRequest.put(jsonObjectFirstRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        Log.d(TAG, "firstRequest: " + jsonArrayFirstRequest.toString());

        if (jsonArrayFirstRequest.length() > 0) {
            parametersFirstRequest.put("data", HelperClass.encrypt(jsonArrayFirstRequest.toString(), ctx));
            new SendFirst().execute();
        }

        return parametersFirstRequest.size();
    }

    static Map<String, String> parametersSecondRequest;

    static Integer secondRequest() {
        parametersSecondRequest = new LinkedHashMap<>();
        dbHelper = new DBHelper(ctx);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        Log.d(TAG, "-------------------------- STATUSES CLIENTS ------------------------");
        //клиент send
        JSONObject jsonObjectSecondRequest = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONArray jsonArraySecondRequest = new JSONArray();
        String sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        Cursor cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_clients_statuses", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                    try {
                        jsonObjectSecondRequest.put("table_name", "rgzbn_gm_ceiling_clients_statuses");
                        sqlQuewy = "SELECT * "
                                + "FROM rgzbn_gm_ceiling_clients_statuses " +
                                "where _id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    JSONObject jsonObject = new JSONObject();
                                    for (int j = 0; j < HelperClass.countColumns(ctx,
                                            "rgzbn_gm_ceiling_clients_statuses"); j++) {
                                        String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                        String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));

                                        if (j == 0) {
                                            status = "android_id";
                                        }
                                        if (status1 == null || status1.equals("null") || status.equals("change_time")) {
                                        } else {
                                            jsonObject.put(status, status1);
                                        }
                                    }
                                    jsonArray.put(jsonObject);
                                } while (c.moveToNext());
                            } else {
                                db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                        "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                        new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_clients_statuses"});
                            }
                        }
                        c.close();
                    } catch (Exception e) {
                        Log.d(TAG, "secondRequest: " + e);
                    }
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectSecondRequest.put("rows", jsonArray);
                jsonArraySecondRequest.put(jsonObjectSecondRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        if (jsonArraySecondRequest.length() > 0) {
            parametersSecondRequest.put("data", HelperClass.encrypt(jsonArraySecondRequest.toString(), ctx));
            new SendSecond().execute();
        }

        return parametersSecondRequest.size();
    }

    static Map<String, String> parametersThirdRequest;

    static Integer thirdRequest() {
        parametersThirdRequest = new LinkedHashMap<>();
        dbHelper = new DBHelper(ctx);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        Log.d(TAG, "-------------------------- CONTACTS ------------------------");
        //контакты send
        JSONObject jsonObjectThirdRequest = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONArray jsonArrayThirdRequest = new JSONArray();
        String sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        Cursor cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_clients_contacts", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                    try {
                        jsonObjectThirdRequest.put("table_name", "rgzbn_gm_ceiling_clients_contacts");
                        sqlQuewy = "SELECT * "
                                + "FROM rgzbn_gm_ceiling_clients_contacts " +
                                "where _id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    JSONObject jsonObject = new JSONObject();
                                    String status = "android_id";
                                    String status1 = c.getString(c.getColumnIndex(c.getColumnName(0)));
                                    jsonObject.put(status, status1);
                                    status = c.getColumnName(c.getColumnIndex(c.getColumnName(1)));
                                    status1 = c.getString(c.getColumnIndex(c.getColumnName(1)));
                                    jsonObject.put(status, status1);
                                    status = c.getColumnName(c.getColumnIndex(c.getColumnName(2)));
                                    status1 = c.getString(c.getColumnIndex(c.getColumnName(2)));
                                    jsonObject.put(status, status1);
                                    jsonArray.put(jsonObject);
                                } while (c.moveToNext());
                            } else {
                                db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                        "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                        new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_clients_contacts"});
                            }
                        }
                        c.close();
                    } catch (Exception e) {
                        Log.d(TAG, "thirdRequest: " + e);
                    }
                } while (cursor.moveToNext());
            } else {
                // check
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectThirdRequest.put("rows", jsonArray);
                jsonArrayThirdRequest.put(jsonObjectThirdRequest);
            } catch (JSONException e) {
                Log.d(TAG, "ThirdRequest: " + e);
            }
        }

        jsonObjectThirdRequest = new JSONObject();
        jsonArray = new JSONArray();
        Log.d(TAG, "-------------------------- DOP CONTACTS ------------------------");
        //контакты send
        sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_clients_dop_contacts", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObjectThirdRequest.put("table_name", "rgzbn_gm_ceiling_clients_dop_contacts");
                        sqlQuewy = "SELECT * "
                                + "FROM rgzbn_gm_ceiling_clients_dop_contacts " +
                                "where _id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    JSONObject jsonObject = new JSONObject();
                                    String status = "android_id";
                                    String status1 = c.getString(c.getColumnIndex(c.getColumnName(0)));
                                    jsonObject.put(status, status1);
                                    status = c.getColumnName(c.getColumnIndex(c.getColumnName(1)));
                                    status1 = c.getString(c.getColumnIndex(c.getColumnName(1)));
                                    jsonObject.put(status, status1);
                                    status = c.getColumnName(c.getColumnIndex(c.getColumnName(2)));
                                    status1 = c.getString(c.getColumnIndex(c.getColumnName(2)));
                                    jsonObject.put(status, status1);
                                    status = c.getColumnName(c.getColumnIndex(c.getColumnName(3)));
                                    status1 = c.getString(c.getColumnIndex(c.getColumnName(3)));
                                    jsonObject.put(status, status1);
                                    jsonArray.put(jsonObject);
                                } while (c.moveToNext());
                            } else {
                                db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                        "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                        new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_clients_dop_contacts"});
                            }
                        }
                        c.close();
                    } catch (Exception e) {
                        Log.d(TAG, "thirdRequest: " + e);
                    }
                } while (cursor.moveToNext());
            } else {
                // check
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectThirdRequest.put("rows", jsonArray);
                jsonArrayThirdRequest.put(jsonObjectThirdRequest);
            } catch (JSONException e) {
                Log.d(TAG, "ThirdRequest: " + e);
            }
        }

        jsonObjectThirdRequest = new JSONObject();
        jsonArray = new JSONArray();
        Log.d(TAG, "-------------------------- CLIENT HISTORY ------------------------");
        //клиент send
        sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_client_history", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObjectThirdRequest.put("table_name", "rgzbn_gm_ceiling_client_history");
                        sqlQuewy = "SELECT * "
                                + "FROM rgzbn_gm_ceiling_client_history " +
                                "where _id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    JSONObject jsonObject = new JSONObject();
                                    for (int j = 0; j < HelperClass.countColumns(ctx,
                                            "rgzbn_gm_ceiling_client_history"); j++) {
                                        String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                        String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));
                                        if (j == 0) {
                                            status = "android_id";
                                        }
                                        if (status1 == null || status1.equals("null") || status.equals("change_time")) {
                                        } else {
                                            jsonObject.put(status, status1);
                                        }
                                    }
                                    jsonArray.put(jsonObject);
                                } while (c.moveToNext());
                            } else {
                                db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                        "id_old = ? and name_table = ? and sync = 0 and type = 'send'",
                                        new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_client_history"});
                            }
                        }
                        c.close();
                    } catch (Exception e) {
                        Log.d(TAG, "thirdRequest: " + e);
                    }
                } while (cursor.moveToNext());
            } else {
                //check
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectThirdRequest.put("rows", jsonArray);
                jsonArrayThirdRequest.put(jsonObjectThirdRequest);
            } catch (JSONException e) {
                Log.d(TAG, "ThirdRequest: " + e);
            }
        }
        jsonObjectThirdRequest = new JSONObject();
        jsonArray = new JSONArray();

        Log.d(TAG, "-------------------------- CALLBACK ------------------------");
        //клиент send
        sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_callback", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {

                    try {
                        String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObjectThirdRequest.put("table_name", "rgzbn_gm_ceiling_callback");
                        sqlQuewy = "SELECT * "
                                + "FROM rgzbn_gm_ceiling_callback " +
                                "where _id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    JSONObject jsonObject = new JSONObject();
                                    for (int j = 0; j < HelperClass.countColumns(ctx, "rgzbn_gm_ceiling_callback"); j++) {
                                        String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                        String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));
                                        if (j == 0) {
                                            status = "android_id";
                                        }
                                        if (status1 == null || status1.equals("null") || status.equals("change_time")) {
                                        } else {
                                            jsonObject.put(status, status1);
                                        }
                                    }
                                    jsonArray.put(jsonObject);
                                } while (c.moveToNext());
                            } else {
                                db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                        "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                        new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_callback"});
                            }
                        }
                        c.close();
                    } catch (Exception e) {
                        Log.d(TAG, "thirdRequest: " + e);
                    }

                } while (cursor.moveToNext());
            } else {
                //check
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectThirdRequest.put("rows", jsonArray);
                jsonArrayThirdRequest.put(jsonObjectThirdRequest);
            } catch (JSONException e) {
                Log.d(TAG, "ThirdRequest: " + e);
            }
        }
        jsonObjectThirdRequest = new JSONObject();
        jsonArray = new JSONArray();

        Log.d(TAG, "-------------------------- CALLS STATUS HISTORY ------------------------");
        //клиент send
        sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_calls_status_history", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObjectThirdRequest.put("table_name", "rgzbn_gm_ceiling_calls_status_history");
                        sqlQuewy = "SELECT * "
                                + "FROM rgzbn_gm_ceiling_calls_status_history " +
                                "where _id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_old)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    JSONObject jsonObject = new JSONObject();
                                    for (int j = 0; j < HelperClass.countColumns(ctx, "rgzbn_gm_ceiling_calls_status_history"); j++) {
                                        String status = c.getColumnName(c.getColumnIndex(c.getColumnName(j)));
                                        String status1 = c.getString(c.getColumnIndex(c.getColumnName(j)));

                                        if (j == 0) {
                                            status = "android_id";
                                        }
                                        if (status1 == null || status1.equals("null") || status.equals("change_time")) {
                                        } else {
                                            jsonObject.put(status, status1);
                                        }
                                    }
                                    jsonArray.put(jsonObject);
                                } while (c.moveToNext());
                            } else {
                                db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                        "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                        new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_calls_status_history"});
                            }
                        }
                        c.close();
                    } catch (Exception e) {
                        Log.d(TAG, "thirdRequest: " + e);
                    }

                } while (cursor.moveToNext());
            } else {
                //chgeck
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectThirdRequest.put("rows", jsonArray);
                jsonArrayThirdRequest.put(jsonObjectThirdRequest);
            } catch (JSONException e) {
                Log.d(TAG, "ThirdRequest: " + e);
            }
        }
        jsonObjectThirdRequest = new JSONObject();
        jsonArray = new JSONArray();

        Log.d(TAG, "-------------------------- CLIENTS STATUS MAP ------------------------");
        //клиент send
        sqlQuewy = "SELECT id_old "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=? and status=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "send", "0", "rgzbn_gm_ceiling_clients_statuses_map", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObjectThirdRequest.put("table_name", "rgzbn_gm_ceiling_clients_statuses_map");
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
                                        if (status1 == null || status1.equals("null") || status.equals("change_time")) {
                                        } else {
                                            jsonObject.put(status, status1);
                                        }
                                    }
                                    jsonArray.put(jsonObject);
                                } while (c.moveToNext());
                            } else {
                                db.delete(DBHelper.HISTORY_SEND_TO_SERVER,
                                        "id_old = ? and name_table = ? and sync = 0 and type = 'send' ",
                                        new String[]{String.valueOf(id_old), "rgzbn_gm_ceiling_clients_statuses_map"});
                            }
                        }
                        c.close();
                    } catch (Exception e) {
                        Log.d(TAG, "thirdRequest: " + e);
                    }

                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectThirdRequest.put("rows", jsonArray);
                jsonArrayThirdRequest.put(jsonObjectThirdRequest);
            } catch (JSONException e) {
                Log.d(TAG, "ThirdRequest: " + e);
            }
        }

        Log.d(TAG, "thirdRequest: " + jsonArrayThirdRequest.toString());

        if (jsonArrayThirdRequest.length() > 0) {
            parametersThirdRequest.put("data", HelperClass.encrypt(jsonArrayThirdRequest.toString(), ctx));
            Log.d(TAG, "thirdRequest:parameters " + parametersThirdRequest);
            new SendThird().execute();
        }

        return parametersThirdRequest.size();
    }

    static Map<String, String> parametersCheck;

    static void checkRequest() {
        parametersCheck = new LinkedHashMap<>();
        dbHelper = new DBHelper(ctx);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        JSONObject jsonObjectCheckRequest = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONArray jsonArrayCheckRequest = new JSONArray();
        String sqlQuewy = "SELECT id_new "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
        Cursor cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "check", "0", "rgzbn_gm_ceiling_clients"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    jsonObjectCheckRequest.put("table_name", "rgzbn_gm_ceiling_clients");
                    do {
                        JSONObject jsonObject = new JSONObject();
                        String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObject.put("id", id_new);
                        jsonArray.put(jsonObject);
                    } while (cursor.moveToNext());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectCheckRequest.put("rows", jsonArray);
                jsonArrayCheckRequest.put(jsonObjectCheckRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        jsonObjectCheckRequest = new JSONObject();
        jsonArray = new JSONArray();
        sqlQuewy = "SELECT id_new "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "check", "0", "rgzbn_users"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    jsonObjectCheckRequest.put("table_name", "rgzbn_users");
                    do {
                        JSONObject jsonObject = new JSONObject();
                        String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObject.put("id", id_new);
                        jsonArray.put(jsonObject);
                    } while (cursor.moveToNext());
                } catch (Exception e) {
                    Log.d(TAG, "checkRequest: " + e);
                }
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectCheckRequest.put("rows", jsonArray);
                jsonArrayCheckRequest.put(jsonObjectCheckRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        jsonObjectCheckRequest = new JSONObject();
        jsonArray = new JSONArray();
        sqlQuewy = "SELECT id_new "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "check", "0", "rgzbn_gm_ceiling_clients_statuses"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    jsonObjectCheckRequest.put("table_name", "rgzbn_gm_ceiling_clients_statuses");
                    do {
                        JSONObject jsonObject = new JSONObject();
                        String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObject.put("id", id_new);
                        jsonArray.put(jsonObject);
                    } while (cursor.moveToNext());
                } catch (Exception e) {
                    Log.d(TAG, "checkRequest: " + e);
                }
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectCheckRequest.put("rows", jsonArray);
                jsonArrayCheckRequest.put(jsonObjectCheckRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        jsonObjectCheckRequest = new JSONObject();
        jsonArray = new JSONArray();
        sqlQuewy = "SELECT id_new "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "check", "0", "rgzbn_gm_ceiling_clients_contacts"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    jsonObjectCheckRequest.put("table_name", "rgzbn_gm_ceiling_clients_contacts");
                    do {
                        JSONObject jsonObject = new JSONObject();
                        String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObject.put("id", id_new);
                        jsonArray.put(jsonObject);
                    } while (cursor.moveToNext());
                } catch (Exception e) {
                    Log.d(TAG, "checkRequest: " + e);
                }
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectCheckRequest.put("rows", jsonArray);
                jsonArrayCheckRequest.put(jsonObjectCheckRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        jsonObjectCheckRequest = new JSONObject();
        jsonArray = new JSONArray();
        sqlQuewy = "SELECT id_new "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "check", "0", "rgzbn_gm_ceiling_clients_dop_contacts"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    jsonObjectCheckRequest.put("table_name", "rgzbn_gm_ceiling_clients_dop_contacts");
                    do {
                        JSONObject jsonObject = new JSONObject();
                        String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObject.put("id", id_new);
                        jsonArray.put(jsonObject);
                    } while (cursor.moveToNext());
                } catch (Exception e) {
                    Log.d(TAG, "checkRequest: " + e);
                }
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectCheckRequest.put("rows", jsonArray);
                jsonArrayCheckRequest.put(jsonObjectCheckRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        jsonObjectCheckRequest = new JSONObject();
        jsonArray = new JSONArray();
        sqlQuewy = "SELECT id_new "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "check", "0", "rgzbn_gm_ceiling_client_history"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    jsonObjectCheckRequest.put("table_name", "rgzbn_gm_ceiling_client_history");
                    do {
                        JSONObject jsonObject = new JSONObject();
                        String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObject.put("id", id_new);
                        jsonArray.put(jsonObject);
                    } while (cursor.moveToNext());
                } catch (Exception e) {
                    Log.d(TAG, "checkRequest: " + e);
                }
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectCheckRequest.put("rows", jsonArray);
                jsonArrayCheckRequest.put(jsonObjectCheckRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        jsonObjectCheckRequest = new JSONObject();
        jsonArray = new JSONArray();
        sqlQuewy = "SELECT id_new "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "check", "0", "rgzbn_gm_ceiling_callback"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    jsonObjectCheckRequest.put("table_name", "rgzbn_gm_ceiling_callback");
                    do {
                        JSONObject jsonObject = new JSONObject();
                        String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObject.put("id", id_new);
                        jsonArray.put(jsonObject);
                    } while (cursor.moveToNext());
                } catch (Exception e) {
                    Log.d(TAG, "checkRequest: " + e);
                }
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectCheckRequest.put("rows", jsonArray);
                jsonArrayCheckRequest.put(jsonObjectCheckRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        jsonObjectCheckRequest = new JSONObject();
        jsonArray = new JSONArray();
        sqlQuewy = "SELECT id_new "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "check", "0", "rgzbn_gm_ceiling_clients_statuses_map"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    jsonObjectCheckRequest.put("table_name", "rgzbn_gm_ceiling_clients_statuses_map");
                    do {
                        JSONObject jsonObject = new JSONObject();
                        String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObject.put("id", id_new);
                        jsonArray.put(jsonObject);
                    } while (cursor.moveToNext());
                } catch (Exception e) {
                    Log.d(TAG, "checkRequest: " + e);
                }
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectCheckRequest.put("rows", jsonArray);
                jsonArrayCheckRequest.put(jsonObjectCheckRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        jsonObjectCheckRequest = new JSONObject();
        jsonArray = new JSONArray();
        sqlQuewy = "SELECT id_new "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and name_table=?";
        cursor = db.rawQuery(sqlQuewy,
                new String[]{String.valueOf(user_id), String.valueOf(user_id + 999999), String.valueOf(999999),
                        "check", "0", "rgzbn_gm_ceiling_calls_status_history"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    jsonObjectCheckRequest.put("table_name", "rgzbn_gm_ceiling_calls_status_history");
                    do {
                        JSONObject jsonObject = new JSONObject();
                        String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        jsonObject.put("id", id_new);
                        jsonArray.put(jsonObject);
                    } while (cursor.moveToNext());
                } catch (Exception e) {
                    Log.d(TAG, "checkRequest: " + e);
                }
            }
        }
        cursor.close();

        if (jsonArray.length() > 0) {
            try {
                jsonObjectCheckRequest.put("rows", jsonArray);
                jsonArrayCheckRequest.put(jsonObjectCheckRequest);
            } catch (JSONException e) {
                Log.d(TAG, "firstRequest: " + e);
            }
        }

        Log.d(TAG, "checkRequest: " + jsonArrayCheckRequest.toString());
        if (jsonArrayCheckRequest.length() > 0) {
            parametersCheck.put("data", HelperClass.encrypt(jsonArrayCheckRequest.toString(), ctx));
            new SendCheck().execute();
        }
    }

    static Map<String, String> parametersDelete;

    static void deleteTable() {
        parametersDelete = new LinkedHashMap<>();
        dbHelper = new DBHelper(ctx);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        Log.d(TAG, "-------------------------- DELETE ------------------------");
        JSONObject jsonObjectDeleteTable = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONArray jsonArrayDeleteTable = new JSONArray();
        String sqlQuewy = "SELECT id_old, name_table "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and status=?";
        Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(user_id),
                String.valueOf(user_id + 999999), String.valueOf(999999), "delete", "0", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    do {
                        String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                        String name_table = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(1)));
                        jsonObjectDeleteTable.put("table_name", name_table);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("id", id_old);
                        jsonArray.put(jsonObject);
                        jsonObjectDeleteTable.put("rows", jsonArray);
                        jsonArrayDeleteTable.put(jsonObjectDeleteTable);
                    } while (cursor.moveToNext());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        cursor.close();
        Log.d(TAG, "deleteRequest: " + jsonArrayDeleteTable.toString());
        if (jsonArrayDeleteTable.length() > 0) {
            parametersDelete.put("data", HelperClass.encrypt(jsonArrayDeleteTable.toString(), ctx));
            new SendDeleteTable().execute();
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

    static class SendFirst extends AsyncTask<Void, Void, Void> {
        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {
                    Log.d(TAG, "onResponse:SendFirst 1 " + res);
                    String newRes = "";
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(res);
                        String data = jsonObject.getString("data");
                        Log.d(TAG, "onResponse: " + data);
                        String hash = jsonObject.getString("hash");
                        Log.d(TAG, "onResponse: " + hash);
                        newRes = HelperClass.decrypt(hash, data, ctx);
                    } catch (JSONException e) {
                        Log.d(TAG, "onResponse: " + e);
                        newRes = "null";
                    }

                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values;
                    String new_id = "";
                    Log.d(TAG, "onResponse:SendFirst 2 " + newRes);
                    if (newRes != null && !newRes.equals("null")) {
                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
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
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_users");
                            for (int i = 0; i < dat.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                new_id = client_contact.getString("new_id");

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

                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        secondRequest();
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "onErrorResponse: " + error);
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Log.d(TAG, "getParams: " + parametersFirstRequest);
                    return parametersFirstRequest;
                }
            };

            requestQueue.add(request);
            return null;
        }
    }

    static class SendSecond extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "onResponse: SEND " + res);

                    String newRes = "";
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(res);
                        String data = jsonObject.getString("data");
                        String hash = jsonObject.getString("hash");
                        newRes = HelperClass.decrypt(hash, data, ctx);
                    } catch (JSONException e) {
                        newRes = "null";
                    }

                    if (newRes != null && !newRes.equals("null")) {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();
                        ContentValues values;
                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_statuses");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                values = new ContentValues();
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
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        thirdRequest();
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Log.d(TAG, "getParams: " + parametersSecondRequest);
                    return parametersSecondRequest;
                }
            };

            requestQueue.add(request);
            return null;
        }
    }

    static class SendThird extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.addDataFromAndroid";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "onResponse: SendThird 1 " + res);
                    String newRes = "";
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(res);
                        String data = jsonObject.getString("data");
                        String hash = jsonObject.getString("hash");
                        newRes = HelperClass.decrypt(hash, data, ctx);
                    } catch (JSONException e) {
                        newRes = "null";
                    }

                    if (newRes != null && !newRes.equals("null")) {
                        Log.d(TAG, "onResponse: SendThird 2 " + newRes);
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        ContentValues values;

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_contacts");
                            for (int i = 0; i < id_array.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

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
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_dop_contacts");
                            for (int i = 0; i < id_array.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

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
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_client_history");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, values,
                                        "_id = ?", new String[]{old_id});

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
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_callback");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, values, "_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and type=? and sync=? and name_table=? and id_new=?",
                                        new String[]{String.valueOf(old_id), "send", "0", "rgzbn_gm_ceiling_callback", "0"});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_OLD, old_id);
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_callback");
                                values.put(DBHelper.KEY_SYNC, "0");
                                values.put(DBHelper.KEY_TYPE, "check");
                                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_statuses_map");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, values, "_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and type=? and sync=? and name_table=? and id_new=?",
                                        new String[]{String.valueOf(old_id), "send", "0", "rgzbn_gm_ceiling_clients_statuses_map", "0"});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_OLD, old_id);
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_clients_statuses_map");
                                values.put(DBHelper.KEY_SYNC, "0");
                                values.put(DBHelper.KEY_TYPE, "check");
                                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_calls_status_history");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, new_id);
                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, values, "_id = ?", new String[]{old_id});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_old = ? and type=? and sync=? and name_table=? and id_new=?",
                                        new String[]{String.valueOf(old_id), "send", "0", "rgzbn_gm_ceiling_calls_status_history", "0"});

                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID_OLD, old_id);
                                values.put(DBHelper.KEY_ID_NEW, new_id);
                                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_calls_status_history");
                                values.put(DBHelper.KEY_SYNC, "0");
                                values.put(DBHelper.KEY_TYPE, "check");
                                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);
                            }

                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        checkRequest();
                        delete();
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Log.d(TAG, "getParams: " + parametersThirdRequest);
                    return parametersThirdRequest;
                }
            };

            requestQueue.add(request);
            return null;
        }
    }

    static class SendCheck extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.CheckDataFromAndroid";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    Log.d(TAG, "onResponse: SendCheck " + res);

                    String newRes = "";
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(res);
                        String data = jsonObject.getString("data");
                        String hash = jsonObject.getString("hash");
                        newRes = HelperClass.decrypt(hash, data, ctx);
                    } catch (JSONException e) {
                        newRes = "null";
                    }

                    Log.d(TAG, "onResponse: " + newRes);

                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    if (newRes != null && !newRes.equals("null")) {
                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients");
                            for (int i = 0; i < dat.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String new_id = client_contact.getString("new_android_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values, "id_new = ? and sync=?",
                                        new String[]{new_id, "0"});
                            }

                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_users");
                            for (int i = 0; i < dat.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String new_id = client_contact.getString("new_android_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                        "id_new = ? and sync=? and name_table = ?",
                                        new String[]{new_id, "0", "rgzbn_users"});

                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_statuses");
                            for (int i = 0; i < dat.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String new_id = client_contact.getString("new_android_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                        "id_new = ? and name_table=? and sync=?",
                                        new String[]{new_id, "rgzbn_gm_ceiling_clients_statuses", "0"});
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_contacts");
                            for (int i = 0; i < dat.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String new_id = client_contact.getString("new_android_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                        "id_new = ? and name_table=? and sync=?",
                                        new String[]{new_id, "rgzbn_gm_ceiling_clients_contacts", "0"});
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_dop_contacts");
                            for (int i = 0; i < dat.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String new_id = client_contact.getString("new_android_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                        "id_new = ? and name_table=? and sync=?",
                                        new String[]{new_id, "rgzbn_gm_ceiling_clients_dop_contacts", "0"});
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_client_history");
                            for (int i = 0; i < dat.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String new_id = client_contact.getString("new_android_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                        "id_new = ? and name_table=? and sync=?",
                                        new String[]{new_id, "rgzbn_gm_ceiling_client_history", "0"});
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_callback");
                            for (int i = 0; i < dat.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String new_id = client_contact.getString("new_android_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                        "id_new = ? and name_table=? and sync=?",
                                        new String[]{new_id, "rgzbn_gm_ceiling_callback", "0"});
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_calls_status_history");
                            for (int i = 0; i < dat.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String new_id = client_contact.getString("new_android_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                        "id_new = ? and name_table=? and sync=?",
                                        new String[]{new_id, "rgzbn_gm_ceiling_calls_status_history", "0"});
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_clients_statuses_map");
                            for (int i = 0; i < dat.length(); i++) {

                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String new_id = client_contact.getString("new_android_id");

                                values = new ContentValues();
                                values.put(DBHelper.KEY_SYNC, "1");
                                db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                        "id_new = ? and name_table=? and sync=?",
                                        new String[]{new_id, "rgzbn_gm_ceiling_clients_statuses_map", "0"});
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }
                    }
                    delete();
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "onErrorResponse: " + error);
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Log.d(TAG, "getParams: " + parametersCheck);
                    return parametersCheck;
                }
            };

            requestQueue.add(request);
            return null;
        }
    }

    static class SendDeleteTable extends AsyncTask<Void, Void, Void> {
        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.deleteDataFromAndroid";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            StringRequest request = new StringRequest(Request.Method.POST, insertUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String res) {

                            Log.d(TAG, "onResponse: " + res);

                            String newRes = "";
                            JSONObject jsonObject = null;
                            try {
                                jsonObject = new JSONObject(res);
                                String data = jsonObject.getString("data");
                                String hash = jsonObject.getString("hash");
                                newRes = HelperClass.decrypt(hash, data, ctx);
                            } catch (JSONException e) {
                                newRes = "null";
                            }

                            Log.d(TAG, "onResponse: delet " + newRes);

                            if (newRes != null && newRes.equals("")) {
                                SQLiteDatabase db;
                                db = dbHelper.getWritableDatabase();
                                newRes = newRes.substring(1, newRes.length() - 1);
                                try {
                                    jsonObject = new JSONObject(newRes);
                                    String delete_id = jsonObject.getString("ids");
                                    String table = jsonObject.getString("table");

                                    ContentValues values = new ContentValues();
                                    values.put(DBHelper.KEY_SYNC, "1");
                                    db.update(DBHelper.HISTORY_SEND_TO_SERVER, values,
                                            "id_old = ? and type=? and sync=? and name_table=? and id_new=?",
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
                    Log.d(TAG, "delete" + parametersDelete);
                    return parametersDelete;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

    static class SendNewUser extends AsyncTask<Void, Void, Void> {
        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.registerUser";

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

                    String newRes = "";
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(res);
                        String data = jsonObject.getString("data");
                        String hash = jsonObject.getString("hash");
                        newRes = HelperClass.decrypt(hash, data, ctx);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    SQLiteDatabase db;
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    if (newRes != null && !newRes.equals("null")) {
                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(newRes);

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
                            Log.d(TAG, "onResponse: " + e);
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
                    Log.d(TAG, "send r_data " + parameters);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }
}