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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ru.itceiling.telephony.DBHelper;

import static android.content.Context.MODE_PRIVATE;

public class ImportDataReceiver extends BroadcastReceiver {

    static DBHelper dbHelper;
    static SQLiteDatabase db;
    static String domen = "",
            TAG = "ImportLog",
            user_id = "",
            change_time_global = "",
            sync_import = "";
    static RequestQueue requestQueue;
    static org.json.simple.JSONObject jsonSync_Import = new org.json.simple.JSONObject();

    final public static String ONE_TIME = "onetime";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences SP = context.getSharedPreferences("link", MODE_PRIVATE);
        domen = SP.getString("", "");

        Log.v(TAG, "ImportDataReceiver started!");
        int count = 0;

        dbHelper = new DBHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sqlQuewy = "SELECT * "
                + "FROM history_send_to_server";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    count++;
                } while (c.moveToNext());
            }
        }
        c.close();

        if (count == 0) {

            SharedPreferences SP_end = context.getSharedPreferences("dealer_id", MODE_PRIVATE);
            user_id = SP_end.getString("", "");

            requestQueue = Volley.newRequestQueue(context.getApplicationContext());

            String change_time = "";
            sqlQuewy = "SELECT change_time "
                    + "FROM history_import_to_server" +
                    " WHERE user_id = ?";
            c = db.rawQuery(sqlQuewy, new String[]{user_id});
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        change_time_global = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    } while (c.moveToNext());
                }
            }
            c.close();

            jsonSync_Import.put("change_time", change_time_global);
            jsonSync_Import.put("dealer_id", user_id);
            sync_import = String.valueOf(jsonSync_Import);
            new ImportDate().execute();

        }
    }


    public void SetAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ImportDataReceiver.class);
        intent.putExtra(ONE_TIME, Boolean.FALSE);//Задаем параметр интента
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60, pi);
    }

    public void CancelAlarm(Context context) {
        Intent intent = new Intent(context, ImportDataReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }


    static class ImportDate extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&task=api.sendInfoToAndroidCallGlider";
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

                    Log.d(TAG, res);

                    SQLiteDatabase db;
                    db = dbHelper.getReadableDatabase();

                    if (res.equals("null")) {
                    } else {
                        int count = 0;
                        try {
                            ContentValues values;

                            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date change_max = ft.parse(change_time_global);

                            JSONObject jsonObject = new JSONObject(res);
                            JSONArray rgzbn_gm_ceiling_clients = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients");
                            Log.d(TAG, "rgzbn_gm_ceiling_clients: " + rgzbn_gm_ceiling_clients);
                            try {
                                for (int i = 0; i < rgzbn_gm_ceiling_clients.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject cleint = rgzbn_gm_ceiling_clients.getJSONObject(i);

                                    Log.d(TAG, "cleint: " + cleint);

                                    String id = cleint.getString("id");
                                    count = 0;

                                    String client_name = cleint.getString("client_name");
                                    String client_data_id = cleint.getString("client_data_id");
                                    String type_id = cleint.getString("type_id");
                                    String manager_id = cleint.getString("manager_id");
                                    String dealer_id = cleint.getString("dealer_id");
                                    String created = cleint.getString("created");
                                    String sex = cleint.getString("sex");
                                    String deleted_by_user = cleint.getString("deleted_by_user");
                                    String change_time = cleint.getString("change_time");

                                    values.put(DBHelper.KEY_CLIENT_NAME, client_name);
                                    values.put(DBHelper.KEY_CLIENT_DATA_ID, client_data_id);
                                    values.put(DBHelper.KEY_TYPE_ID, type_id);
                                    values.put(DBHelper.KEY_MANAGER_ID, manager_id);
                                    values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                                    values.put(DBHelper.KEY_CREATED, created);
                                    values.put(DBHelper.KEY_SEX, sex);
                                    values.put(DBHelper.KEY_DELETED_BY_USER, deleted_by_user);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_clients" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            do {
                                                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, values, "_id = ?", new String[]{id});
                                                count++;
                                                Date change = ft.parse(change_time);
                                                if (change_max.getTime() < change.getTime()) {
                                                    change_max = change;
                                                }
                                            } while (c.moveToNext());
                                        }
                                    }
                                    c.close();

                                    if (count == 0) {
                                        try {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, null, values);

                                            Date change = ft.parse(change_time);
                                            if (change_max.getTime() < change.getTime()) {
                                                change_max = change;
                                            }
                                        } catch (Exception e) {
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: " + e);
                            }

                            JSONArray rgzbn_gm_ceiling_clients_contacts = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients_contacts");
                            Log.d(TAG, "rgzbn_gm_ceiling_clients_contacts: " + rgzbn_gm_ceiling_clients_contacts);
                            for (int i = 0; i < rgzbn_gm_ceiling_clients_contacts.length(); i++) {

                                values = new ContentValues();
                                org.json.JSONObject client_contact = rgzbn_gm_ceiling_clients_contacts.getJSONObject(i);

                                count = 0;
                                String id = client_contact.getString("id");
                                String client_id = client_contact.getString("client_id");
                                String phone = client_contact.getString("phone");
                                String change_time = client_contact.getString("change_time");

                                values.put(DBHelper.KEY_ID, id);
                                values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                values.put(DBHelper.KEY_PHONE, phone);

                                String sqlQuewy = "SELECT * "
                                        + "FROM rgzbn_gm_ceiling_clients_contacts" +
                                        " WHERE _id = ?";
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        do {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, values, "_id = ?", new String[]{id});
                                            count++;
                                            Date change = ft.parse(change_time);
                                            if (change_max.getTime() < change.getTime()) {
                                                change_max = change;
                                            }
                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();

                                if (count == 0) {
                                    try {
                                        values.put(DBHelper.KEY_ID, id);
                                        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, null, values);
                                        Date change = ft.parse(change_time);
                                        if (change_max.getTime() < change.getTime()) {
                                            change_max = change;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            JSONArray rgzbn_gm_ceiling_clients_dop_contacts = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients_dop_contacts");
                            Log.d(TAG, "rgzbn_gm_ceiling_clients_dop_contacts: " + rgzbn_gm_ceiling_clients_dop_contacts);
                            for (int i = 0; i < rgzbn_gm_ceiling_clients_dop_contacts.length(); i++) {

                                values = new ContentValues();
                                org.json.JSONObject client_dop_contact = rgzbn_gm_ceiling_clients_dop_contacts.getJSONObject(i);

                                count = 0;
                                String id = client_dop_contact.getString("id");
                                String client_id = client_dop_contact.getString("client_id");
                                String type_id = client_dop_contact.getString("type_id");
                                String contact = client_dop_contact.getString("contact");

                                values.put(DBHelper.KEY_ID, id);
                                values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                values.put(DBHelper.KEY_TYPE_ID, type_id);
                                values.put(DBHelper.KEY_CONTACT, contact);

                                String sqlQuewy = "SELECT * "
                                        + "FROM rgzbn_gm_ceiling_clients_contacts" +
                                        " WHERE _id = ?";
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        do {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS, values, "_id = ?", new String[]{id});
                                            count++;
                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();

                                if (count == 0) {
                                    try {
                                        values.put(DBHelper.KEY_ID, id);
                                        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS, null, values);
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            JSONArray rgzbn_gm_ceiling_callback = jsonObject.getJSONArray("rgzbn_gm_ceiling_callback");
                            Log.d(TAG, "rgzbn_gm_ceiling_callback: " + rgzbn_gm_ceiling_callback);
                            for (int i = 0; i < rgzbn_gm_ceiling_callback.length(); i++) {

                                values = new ContentValues();
                                org.json.JSONObject callback = rgzbn_gm_ceiling_callback.getJSONObject(i);

                                Log.d(TAG, "callback " + String.valueOf(callback));

                                count = 0;
                                String id = callback.getString("id");
                                String client_id = callback.getString("client_id");
                                String date_time = callback.getString("date_time");
                                String comment = callback.getString("comment");
                                String manager_id = callback.getString("manager_id");
                                String notify = callback.getString("notify");
                                String change_time = callback.getString("change_time");

                                values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                values.put(DBHelper.KEY_DATE_TIME, date_time);
                                values.put(DBHelper.KEY_COMMENT, comment);
                                values.put(DBHelper.KEY_MANAGER_ID, manager_id);
                                values.put(DBHelper.KEY_NOTIFY, notify);

                                String sqlQuewy = "SELECT * "
                                        + "FROM rgzbn_gm_ceiling_callback" +
                                        " WHERE _id = ?";
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        do {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, values,
                                                    "_id = ?", new String[]{id});
                                            count++;
                                            Date change = ft.parse(change_time);
                                            if (change_max.getTime() < change.getTime()) {
                                                change_max = change;
                                            }
                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();

                                if (count == 0) {
                                    try {
                                        values.put(DBHelper.KEY_ID, id);
                                        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, null, values);
                                        Date change = ft.parse(change_time);
                                        if (change_max.getTime() < change.getTime()) {
                                            change_max = change;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            JSONArray rgzbn_gm_ceiling_client_history = jsonObject.getJSONArray("rgzbn_gm_ceiling_client_history");
                            Log.d(TAG, "rgzbn_gm_ceiling_client_history: " + rgzbn_gm_ceiling_client_history);
                            for (int i = 0; i < rgzbn_gm_ceiling_client_history.length(); i++) {

                                values = new ContentValues();
                                org.json.JSONObject client_history = rgzbn_gm_ceiling_client_history.getJSONObject(i);

                                Log.d(TAG, "client_history " + String.valueOf(client_history));

                                count = 0;
                                String id = client_history.getString("id");
                                String client_id = client_history.getString("client_id");
                                String date_time = client_history.getString("date_time");
                                String text = client_history.getString("text");
                                String change_time = client_history.getString("change_time");

                                values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                values.put(DBHelper.KEY_DATE_TIME, date_time);
                                values.put(DBHelper.KEY_TEXT, text);
                                //values.put(DBHelper.KEY_CHANGE_TIME, change_time);

                                String sqlQuewy = "SELECT * "
                                        + "FROM rgzbn_gm_ceiling_client_history" +
                                        " WHERE _id = ?";
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        do {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, values,
                                                    "_id = ?", new String[]{id});
                                            count++;
                                            Date change = ft.parse(change_time);
                                            if (change_max.getTime() < change.getTime()) {
                                                change_max = change;
                                            }
                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();


                                if (count == 0) {
                                    try {
                                        values.put(DBHelper.KEY_ID, id);
                                        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, null, values);
                                        Date change = ft.parse(change_time);
                                        if (change_max.getTime() < change.getTime()) {
                                            change_max = change;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            JSONArray rgzbn_gm_ceiling_calls_status_history = jsonObject.getJSONArray("rgzbn_gm_ceiling_calls_status_history");
                            Log.d(TAG, "rgzbn_gm_ceiling_calls_status_history: " + rgzbn_gm_ceiling_calls_status_history);
                            for (int i = 0; i < rgzbn_gm_ceiling_calls_status_history.length(); i++) {

                                values = new ContentValues();
                                org.json.JSONObject status_history = rgzbn_gm_ceiling_calls_status_history.getJSONObject(i);

                                count = 0;
                                String id = status_history.getString("id");
                                String manager_id = status_history.getString("manager_id");
                                String client_id = status_history.getString("client_id");
                                String status = status_history.getString("status");
                                String date_time = status_history.getString("date_time");
                                String call_length = status_history.getString("call_length");

                                values.put(DBHelper.KEY_MANAGER_ID, manager_id);
                                values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                values.put(DBHelper.KEY_STATUS, status);
                                values.put(DBHelper.KEY_CALL_LENGTH, call_length);
                                values.put(DBHelper.KEY_DATE_TIME, date_time);

                                String sqlQuewy = "SELECT * "
                                        + "FROM rgzbn_gm_ceiling_calls_status_history" +
                                        " WHERE _id = ?";
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        do {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, values,
                                                    "_id = ?", new String[]{id});
                                            count++;
                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();
                                if (count == 0) {
                                    try {
                                        values.put(DBHelper.KEY_ID, id);
                                        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, null, values);
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            JSONArray rgzbn_gm_ceiling_calls_status = jsonObject.getJSONArray("rgzbn_gm_ceiling_calls_status");
                            Log.d(TAG, "rgzbn_gm_ceiling_calls_status: " + rgzbn_gm_ceiling_calls_status);
                            for (int i = 0; i < rgzbn_gm_ceiling_calls_status.length(); i++) {

                                values = new ContentValues();
                                org.json.JSONObject status_history = rgzbn_gm_ceiling_calls_status.getJSONObject(i);

                                count = 0;
                                String id = status_history.getString("id");
                                String title = status_history.getString("title");
                                String change_time = status_history.getString("change_time");

                                values.put(DBHelper.KEY_TITLE, title);

                                String sqlQuewy = "SELECT * "
                                        + "FROM rgzbn_gm_ceiling_calls_status" +
                                        " WHERE _id = ?";
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        do {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS, values,
                                                    "_id = ?", new String[]{id});
                                            count++;
                                            Date change = ft.parse(change_time);
                                            if (change_max.getTime() < change.getTime()) {
                                                change_max = change;
                                            }
                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();
                                if (count == 0) {
                                    try {
                                        values.put(DBHelper.KEY_ID, id);
                                        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS, null, values);
                                        Date change = ft.parse(change_time);
                                        if (change_max.getTime() < change.getTime()) {
                                            change_max = change;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            JSONArray rgzbn_gm_ceiling_clients_statuses = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients_statuses");
                            Log.d(TAG, "rgzbn_gm_ceiling_clients_statuses: " + rgzbn_gm_ceiling_clients_statuses);

                            for (int i = 0; i < rgzbn_gm_ceiling_clients_statuses.length(); i++) {

                                values = new ContentValues();
                                org.json.JSONObject status = rgzbn_gm_ceiling_clients_statuses.getJSONObject(i);

                                count = 0;
                                String id = status.getString("id");
                                String title = status.getString("title");
                                String dealer_id = status.getString("dealer_id");
                                String change_time = status.getString("change_time");

                                values.put(DBHelper.KEY_TITLE, title);
                                values.put(DBHelper.KEY_DEALER_ID, dealer_id);

                                String sqlQuewy = "SELECT * "
                                        + "FROM rgzbn_gm_ceiling_clients_statuses" +
                                        " WHERE _id = ?";
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        do {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, values,
                                                    "_id = ?", new String[]{id});
                                            count++;
                                            Date change = ft.parse(change_time);
                                            if (change_max.getTime() < change.getTime()) {
                                                change_max = change;
                                            }
                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();
                                if (count == 0) {
                                    try {
                                        values.put(DBHelper.KEY_ID, id);
                                        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, null, values);
                                        Date change = ft.parse(change_time);
                                        if (change_max.getTime() < change.getTime()) {
                                            change_max = change;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            JSONArray rgzbn_gm_ceiling_clients_statuses_map = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients_statuses_map");
                            Log.d(TAG, "rgzbn_gm_ceiling_clients_statuses_map: " + rgzbn_gm_ceiling_clients_statuses_map);
                            for (int i = 0; i < rgzbn_gm_ceiling_clients_statuses_map.length(); i++) {

                                values = new ContentValues();
                                org.json.JSONObject status = rgzbn_gm_ceiling_clients_statuses_map.getJSONObject(i);

                                count = 0;
                                String id = status.getString("id");
                                String client_id = status.getString("client_id");
                                String status_id = status.getString("status_id");
                                String change_time = status.getString("change_time");

                                values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                values.put(DBHelper.KEY_STATUS_ID, status_id);

                                String sqlQuewy = "SELECT * "
                                        + "FROM rgzbn_gm_ceiling_clients_statuses_map" +
                                        " WHERE _id = ?";
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        do {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, values,
                                                    "_id = ?", new String[]{id});
                                            count++;
                                            Date change = ft.parse(change_time);
                                            if (change_max.getTime() < change.getTime()) {
                                                change_max = change;
                                            }
                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();
                                if (count == 0) {
                                    try {
                                        values.put(DBHelper.KEY_ID, id);
                                        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, null, values);
                                        Date change = ft.parse(change_time);
                                        if (change_max.getTime() < change.getTime()) {
                                            change_max = change;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            JSONArray rgzbn_gm_ceiling_api_phones = jsonObject.getJSONArray("rgzbn_gm_ceiling_api_phones");
                            Log.d(TAG, "rgzbn_gm_ceiling_api_phones: " + rgzbn_gm_ceiling_api_phones);
                            for (int i = 0; i < rgzbn_gm_ceiling_api_phones.length(); i++) {

                                values = new ContentValues();
                                org.json.JSONObject api_p = rgzbn_gm_ceiling_api_phones.getJSONObject(i);

                                count = 0;
                                String id = api_p.getString("id");
                                String number = api_p.getString("number");
                                String name = api_p.getString("name");
                                String description = api_p.getString("description");
                                String site = api_p.getString("site");
                                String dealer_id = api_p.getString("dealer_id");
                                String change_time = api_p.getString("change_time");

                                values.put(DBHelper.KEY_NUMBER, number);
                                values.put(DBHelper.KEY_NAME, name);
                                values.put(DBHelper.KEY_DESCRIPTION, description);
                                values.put(DBHelper.KEY_SITE, site);
                                values.put(DBHelper.KEY_DEALER_ID, dealer_id);

                                String sqlQuewy = "SELECT * "
                                        + "FROM rgzbn_gm_ceiling_api_phones" +
                                        " WHERE _id = ?";
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        do {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_API_PHONES, values,
                                                    "_id = ?", new String[]{id});
                                            count++;
                                            Date change = ft.parse(change_time);
                                            if (change_max.getTime() < change.getTime()) {
                                                change_max = change;
                                            }
                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();
                                if (count == 0) {
                                    try {
                                        values.put(DBHelper.KEY_ID, id);
                                        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_API_PHONES, null, values);
                                        Date change = ft.parse(change_time);
                                        if (change_max.getTime() < change.getTime()) {
                                            change_max = change;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            JSONArray rgzbn_users = jsonObject.getJSONArray("rgzbn_users");
                            Log.d(TAG, "rgzbn_users: " + rgzbn_users);
                            for (int i = 0; i < rgzbn_users.length(); i++) {

                                values = new ContentValues();
                                org.json.JSONObject user_v = rgzbn_users.getJSONObject(i);

                                count = 0;
                                String id = user_v.getString("id");
                                String name = user_v.getString("name");
                                String username = user_v.getString("username");
                                String email = user_v.getString("email");
                                String dealer_id = user_v.getString("dealer_id");
                                String change_time = user_v.getString("change_time");

                                values.put(DBHelper.KEY_NAME, name);
                                values.put(DBHelper.KEY_USERNAME, username);
                                values.put(DBHelper.KEY_EMAIL, email);
                                values.put(DBHelper.KEY_DEALER_ID, dealer_id);

                                String sqlQuewy = "SELECT * "
                                        + "FROM rgzbn_users" +
                                        " WHERE _id = ?";
                                Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        do {
                                            db.update(DBHelper.TABLE_USERS, values,
                                                    "_id = ?", new String[]{id});
                                            count++;
                                            Date change = ft.parse(change_time);
                                            if (change_max.getTime() < change.getTime()) {
                                                change_max = change;
                                            }
                                        } while (c.moveToNext());
                                    }
                                }
                                c.close();
                                if (count == 0) {
                                    try {
                                        values.put(DBHelper.KEY_ID, id);
                                        db.insert(DBHelper.TABLE_USERS, null, values);
                                        Date change = ft.parse(change_time);
                                        if (change_max.getTime() < change.getTime()) {
                                            change_max = change;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            SimpleDateFormat out_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                            values = new ContentValues();
                            values.put(DBHelper.KEY_CHANGE_TIME, String.valueOf(out_format.format(change_max)));
                            db.update(DBHelper.HISTORY_IMPORT_TO_SERVER, values, "user_id = ?", new String[]{user_id});

                            Log.d(TAG, "NEW change_time: " + String.valueOf(out_format.format(change_max)));

                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: " + e);
                        }
                        int i = 0;
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "onResponse: " + error);

                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("synchronization", sync_import);
                    Log.d(TAG, String.valueOf(parameters));
                    return parameters;
                }
            };

            requestQueue.add(request);

            return null;
        }

    }

}