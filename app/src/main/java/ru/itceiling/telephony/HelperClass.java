package ru.itceiling.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.MODE_PRIVATE;

public class HelperClass {

    public static int lastIdTable(String table, Context context, String dealer_id) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db;
        db = dbHelper.getWritableDatabase();

        int max_id = 0;
        try {
            String sqlQuewy = "select MAX(_id) "
                    + "FROM " + table + " " +
                    "where _id>? and _id<?";
            Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(Integer.parseInt(dealer_id) * 100000),
                    String.valueOf(Integer.parseInt(dealer_id) * 100000 + 999999)});
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        max_id = Integer.parseInt(c.getString(c.getColumnIndex(c.getColumnName(0))));
                        max_id++;
                    } while (c.moveToNext());
                }
            }
        } catch (Exception e) {
            max_id = Integer.parseInt(dealer_id) * 100000 + 1;
        }

        return max_id;
    }

    public static String now_date() {
        Calendar date_cr = new GregorianCalendar();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = df.format(date_cr.getTime());
        return date;
    }

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static boolean validateMail(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return matcher.find();
    }

    public static String phone_edit(String phone) {

        String str1 = phone.substring(0, 2);
        String str2 = phone;
        if (str1.equals("7")) {

        } else if (str1.equals("+8") || str1.equals("+7")) {
            str2 = phone.substring(2, phone.length());
            str2 = "7" + str2;
        } else {
            str2 = phone.substring(1, phone.length());
            str2 = "7" + str2;
        }

        return str2;
    }

    public static void addHistory(String text, Context context, String id_client) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        SharedPreferences SPI = context.getSharedPreferences("dealer_id", MODE_PRIVATE);
        String dealer_id = SPI.getString("", "");

        int max_id = lastIdTable("rgzbn_gm_ceiling_client_history", context, dealer_id);

        String date = HelperClass.now_date();
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID, max_id);
        values.put(DBHelper.KEY_CLIENT_ID, id_client);
        values.put(DBHelper.KEY_DATE_TIME, date);
        values.put(DBHelper.KEY_TEXT, text);
        values.put(DBHelper.KEY_CHANGE_TIME, now_date());
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, null, values);

        HelperClass.addExportData(
                context,
                max_id,
                "rgzbn_gm_ceiling_client_history",
                "send");

    }

    public static void addCallback(String comment, Context context, String id_client, String callDate) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        SharedPreferences SPI = context.getSharedPreferences("dealer_id", MODE_PRIVATE);
        String dealer_id = SPI.getString("", "");

        int max_id = lastIdTable("rgzbn_gm_ceiling_callback", context, dealer_id);

        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID, max_id);
        values.put(DBHelper.KEY_CLIENT_ID, id_client);
        values.put(DBHelper.KEY_DATE_TIME, callDate + ":00");
        values.put(DBHelper.KEY_COMMENT, comment);
        values.put(DBHelper.KEY_MANAGER_ID, "");
        values.put(DBHelper.KEY_NOTIFY, "");
        values.put(DBHelper.KEY_CHANGE_TIME, now_date());
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, null, values);

        HelperClass.addExportData(
                context,
                max_id,
                "rgzbn_gm_ceiling_callback",
                "send");

    }

    public static int countColumns(Context context, String table_name) {

        int count = 0;
        String sql = "";

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db;
        db = dbHelper.getWritableDatabase();

        String sqlQuewy = "SELECT sql " +
                "FROM sqlite_master " +
                "WHERE tbl_name = '" + table_name + "' ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    sql = c.getString(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        count = sql.length() - sql.replace(",", "").length() + 1;

        return count;
    }

    public static void addExportData(Context context, Integer id, String nameTable, String type) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db;
        db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID_OLD, id);
        values.put(DBHelper.KEY_ID_NEW, "0");
        values.put(DBHelper.KEY_NAME_TABLE, nameTable);
        values.put(DBHelper.KEY_SYNC, "0");
        values.put(DBHelper.KEY_TYPE, type);
        values.put(DBHelper.KEY_STATUS, "1");
        db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);
    }

    public static void addExportData(Context context, Integer id, String nameTable, String type, String date) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db;
        db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID_OLD, id);
        values.put(DBHelper.KEY_ID_NEW, "0");
        values.put(DBHelper.KEY_NAME_TABLE, nameTable);
        values.put(DBHelper.KEY_SYNC, "0");
        values.put(DBHelper.KEY_TYPE, type);
        values.put(DBHelper.KEY_STATUS, "1");
        values.put(DBHelper.KEY_DATE, date);
        db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);
    }

    public static void addCallsStatusHistory(Context context, int clientId, int status, int callLength){

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        SharedPreferences SPI = context.getSharedPreferences("user_id", MODE_PRIVATE);
        String user_id = SPI.getString("", "");

        int max_id = lastIdTable("rgzbn_gm_ceiling_calls_status_history", context, user_id);

        String date = HelperClass.now_date();
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID, max_id);
        values.put(DBHelper.KEY_MANAGER_ID, user_id);
        values.put(DBHelper.KEY_CLIENT_ID, clientId);
        values.put(DBHelper.KEY_STATUS, status);
        values.put(DBHelper.KEY_CALL_LENGTH, callLength);
        values.put(DBHelper.KEY_CHANGE_TIME, date);
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, null, values);

        HelperClass.addExportData(
                context,
                max_id,
                "rgzbn_gm_ceiling_calls_status_history",
                "send");

    }

    public static String associated_client(Context context, String user_id) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Log.d("logd", "associated_client: " + user_id);

        String associated_client = "";
        String sqlQuewy = "SELECT associated_client "
                + "FROM rgzbn_users " +
                "where _id = ?";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{user_id});
        if (c != null) {
            if (c.moveToFirst()) {
                associated_client = c.getString(c.getColumnIndex(c.getColumnName(0)));
            }
        }
        c.close();

        Log.d("logd", "associated_client: " + associated_client);

        return associated_client;
    }
}