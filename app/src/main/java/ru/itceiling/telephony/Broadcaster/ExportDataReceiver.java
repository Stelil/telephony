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

    static String sendClient = "[", sendClientContacts = "[", sendClientDopContacts = "[", sendClientsStatus = "[",
            sendUsers = "[", sendUsersMap = "[", sendApiPhones = "[", sendClientHistory = "[", sendCallback = "[",
            sendCallStatusHistory = "[", sendClientStatusMap = "[";

    static String checkApiPhones = "[", checkClientHistory = "[", checkCallback = "[", checkCallStatusHistory = "[",
            checkClientsStatus = "[", checkClientStatusMap = "[", checkClientsContacts = "[", checkClientsDopContacts = "[",
            checkUsers = "[", checkUsersMap = "[", checkClient = "[";

    static String jsonDelete = "", jsonDeleteTable = "", jsonNewUser = "";

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

        if (count_line > 0 && HelperClass.isOnline(context)) {
            try {
                requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
            } catch (Exception e) {
            }
            delete();

            SP = ctx.getSharedPreferences("user_id", MODE_PRIVATE);
            String gager_id = SP.getString("", "");
            user_id = Integer.parseInt(gager_id) * 100000;

            if (firstRequest() == 0) {
                if (secondRequest() == 0) {
                    if (thirdRequest() == 0) {
                        newUser();
                        deleteTable();
                        checkRequest();
                    }
                }
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
            parameters.put("r_data", jsonNewUser);
            new SendNewUser().execute();
        }
    }

    static Map<String, String> parametersFirstRequest;

    static Integer firstRequest() {

        parametersFirstRequest = new LinkedHashMap<>();
        dbHelper = new DBHelper(ctx);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

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
                                        if (status1 == null || status1.equals("") || status1.equals("null") || status.equals("change_time")) {
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
                // check
            }
        }
        cursor.close();
        sendClient = sendClient.substring(0, sendClient.length() - 1) + "]";
        if (sendClient.equals("]")) {
        } else {
            parametersFirstRequest.put("rgzbn_gm_ceiling_clients", sendClient);
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
                                            if (status1.equals("") || (status1 == null) || status.equals("change_time")) {
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
            }
        }
        cursor.close();

        sendUsers = sendUsers.substring(0, sendUsers.length() - 1) + "]";
        if (sendUsers.equals("]") || sendUsers.equals("[]") || sendUsers.equals("[")) {
        } else {
            parametersFirstRequest.put("rgzbn_users", sendUsers);
        }

        if (parametersFirstRequest.size() > 0) {
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
        sendClientsStatus = "[";
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
                                        if (status1 == null || status1.equals("null") || status.equals("change_time")) {
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
            }
        }
        cursor.close();

        sendClientsStatus = sendClientsStatus.substring(0, sendClientsStatus.length() - 1) + "]";
        if (sendClientsStatus.equals("]")) {
        } else {
            parametersSecondRequest.put("rgzbn_gm_ceiling_clients_statuses", sendClientsStatus);
        }

        if (parametersSecondRequest.size() > 0) {
            new SendSecond().execute();
        } else {
            thirdRequest();
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
        sendClientContacts = "[";
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
            }
        }
        cursor.close();

        sendClientContacts = sendClientContacts.substring(0, sendClientContacts.length() - 1) + "]";
        if (sendClientContacts.equals("]")) {
        } else {
            parametersThirdRequest.put("rgzbn_gm_ceiling_clients_contacts", sendClientContacts);
        }

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
            }
        }
        cursor.close();
        sendClientDopContacts = sendClientDopContacts.substring(0, sendClientDopContacts.length() - 1) + "]";
        if (sendClientDopContacts.equals("]")) {
        } else {
            parametersThirdRequest.put("rgzbn_gm_ceiling_clients_dop_contacts", sendClientDopContacts);
        }

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
                                        if (status1 == null || status1.equals("null") || status.equals("change_time")) {
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
                //check
            }
        }
        cursor.close();
        sendClientHistory = sendClientHistory.substring(0, sendClientHistory.length() - 1) + "]";

        if (sendClientHistory.equals("]")) {
        } else {
            parametersThirdRequest.put("rgzbn_gm_ceiling_client_history", sendClientHistory);
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
                                        if (status1 == null || status1.equals("null") || status.equals("change_time")) {
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
                //check
            }
        }
        cursor.close();
        sendCallback = sendCallback.substring(0, sendCallback.length() - 1) + "]";
        if (sendCallback.equals("]")) {
        } else {
            parametersThirdRequest.put("rgzbn_gm_ceiling_callback", sendCallback);
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
                                        if (status1 == null || status1.equals("null") || status.equals("change_time")) {
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
                //chgeck
            }
        }
        cursor.close();
        sendCallStatusHistory = sendCallStatusHistory.substring(0, sendCallStatusHistory.length() - 1) + "]";
        if (sendCallStatusHistory.equals("]")) {
        } else {
            parametersThirdRequest.put("rgzbn_gm_ceiling_calls_status_history", sendCallStatusHistory);
        }

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
                                        if (status1 == null || status1.equals("null") || status.equals("change_time")) {
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
            }
        }
        cursor.close();
        sendClientStatusMap = sendClientStatusMap.substring(0, sendClientStatusMap.length() - 1) + "]";
        if (sendClientStatusMap.equals("]")) {
        } else {
            parametersThirdRequest.put("rgzbn_gm_ceiling_clients_statuses_map", sendClientStatusMap);
        }

        if (parametersThirdRequest.size() > 0) {
            new SendThird().execute();
        } else {
            checkRequest();
            deleteTable();
        }

        return parametersThirdRequest.size();
    }

    static Map<String, String> parametersCheck;

    static void checkRequest() {
        parametersCheck = new LinkedHashMap<>();
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
        cursor.close();
        checkClient = checkClient.substring(0, checkClient.length() - 1) + "]";
        if (checkClient.equals("]")) {
        } else {
            parametersCheck.put("rgzbn_gm_ceiling_clients", checkClient);
        }

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
        cursor.close();

        checkUsers = checkUsers.substring(0, checkUsers.length() - 1) + "]";
        if (checkUsers.equals("]")) {
        } else {
            parametersCheck.put("rgzbn_users", checkUsers);
        }

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
        cursor.close();

        checkClientsStatus = checkClientsStatus.substring(0, checkClientsStatus.length() - 1) + "]";
        if (checkClientsStatus.equals("]")) {
        } else {
            parametersCheck.put("rgzbn_gm_ceiling_clients_statuses", checkClientsStatus);
        }

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
        cursor.close();

        checkClientsContacts = checkClientsContacts.substring(0, checkClientsContacts.length() - 1) + "]";
        if (checkClientsContacts.equals("]")) {
        } else {
            parametersCheck.put("rgzbn_gm_ceiling_clients_contacts", checkClientsContacts);
        }

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
                    jsonObjectClientContacts = new org.json.simple.JSONObject();
                    String id_new = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                    jsonObjectClientContacts.put("id", id_new);
                    checkClientsDopContacts += String.valueOf(jsonObjectClientContacts) + ",";
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        checkClientsDopContacts = checkClientsDopContacts.substring(0, checkClientsDopContacts.length() - 1) + "]";
        if (checkClientsDopContacts.equals("]")) {
        } else {
            parametersCheck.put("rgzbn_gm_ceiling_clients_dop_contacts", checkClientsDopContacts);
        }

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
        cursor.close();

        checkClientHistory = checkClientHistory.substring(0, checkClientHistory.length() - 1) + "]";
        if (checkClientHistory.equals("]")) {
        } else {
            parametersCheck.put("rgzbn_gm_ceiling_client_history", checkClientHistory);
        }

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
        cursor.close();

        checkCallback = checkCallback.substring(0, checkCallback.length() - 1) + "]";
        if (checkCallback.equals("]")) {
        } else {
            parametersCheck.put("rgzbn_gm_ceiling_callback", checkCallback);
        }

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
        cursor.close();

        checkClientStatusMap = checkClientStatusMap.substring(0, checkClientStatusMap.length() - 1) + "]";
        if (checkClientStatusMap.equals("]")) {
        } else {
            parametersCheck.put("rgzbn_gm_ceiling_clients_statuses_map", checkClientStatusMap);
        }

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
        cursor.close();

        checkCallStatusHistory = checkCallStatusHistory.substring(0, checkCallStatusHistory.length() - 1) + "]";
        if (checkCallStatusHistory.equals("]")) {
        } else {
            parametersCheck.put("rgzbn_gm_ceiling_calls_status_history", checkCallStatusHistory);
        }

        if (parametersCheck.size() > 0) {
            new SendCheck().execute();
        }
    }

    static Map<String, String> parametersDelete;

    static void deleteTable() {
        parametersDelete = new LinkedHashMap<>();
        dbHelper = new DBHelper(ctx);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        Log.d(TAG, "-------------------------- DELETE ------------------------");
        //send
        String sqlQuewy = "SELECT id_old, name_table "
                + "FROM history_send_to_server " +
                "where ((id_old>=? and id_old<=?) or (id_old<=?)) and type=? and sync=? and status=?";
        Cursor cursor = db.rawQuery(sqlQuewy, new String[]{String.valueOf(user_id),
                String.valueOf(user_id + 999999), String.valueOf(999999), "delete", "0", "1"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String id_old = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(0)));
                String name_table = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(1)));
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", id_old);

                    parametersDelete.put(name_table, "[" + String.valueOf(jsonObject) + "]");
                } catch (Exception e) {
                }
            }
        }
        cursor.close();

        if (parametersDelete.size() > 0) {
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

                    Log.d(TAG, "onResponse: SEND " + res);

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

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
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
                        }

                        secondRequest();
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
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

                    if (res.equals("") || res.equals("\"\u041e\u0448\u0438\u0431\u043a\u0430!\"")) {
                        Log.d("sync_app", "SendClientData пусто");
                    } else {
                        SQLiteDatabase db;
                        db = dbHelper.getWritableDatabase();
                        ContentValues values;
                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
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

                    Log.d(TAG, "onResponse: SEND " + res);

                    if (res.equals("") || res.equals("\"\u041e\u0448\u0438\u0431\u043a\u0430!\"")) {
                        Log.d("sync_app", "SendClientData пусто");
                    } else {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        ContentValues values;

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
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(old_id),
                                        "send", "0", "rgzbn_gm_ceiling_clients_contacts"});
                                if (c != null) {
                                    if (c.moveToFirst()) {
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

                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();
                            }
                        } catch (Exception e) {
                        }

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
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(old_id), "send", "0",
                                        "rgzbn_gm_ceiling_clients_dop_contacts"});
                                if (c != null) {
                                    if (c.moveToFirst()) {
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

                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();
                            }
                        } catch (Exception e) {
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
                            JSONArray id_array = dat.getJSONArray("rgzbn_gm_ceiling_client_history");
                            for (int i = 0; i < id_array.length(); i++) {
                                org.json.JSONObject client_contact = id_array.getJSONObject(i);
                                String old_id = client_contact.getString("old_id");
                                String new_id = client_contact.getString("new_id");

                                String sqlQuewy = "SELECT * "
                                        + "FROM history_send_to_server " +
                                        "where id_old = ? and type=? and sync = ? and name_table=?";
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(old_id),
                                        "send", "0", "rgzbn_gm_ceiling_client_history"});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        do {

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

                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();

                            }
                        } catch (Exception e) {
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
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
                        } catch (Exception e) {
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
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
                        } catch (Exception e) {
                        }

                        try {
                            org.json.JSONObject dat = new org.json.JSONObject(res);
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

                        } catch (Exception e) {
                        }

                        delete();
                        checkRequest();
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

                    Log.d(TAG, "onResponse: SEND " + res);

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
                    Log.d(TAG, "send r_data " + parameters);
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }
}