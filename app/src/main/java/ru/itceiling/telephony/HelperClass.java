package ru.itceiling.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.system.ErrnoException;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static android.content.Context.MODE_PRIVATE;
import static ru.itceiling.telephony.SubscriptionsActivity.TAG;

public class HelperClass {

    static String TAG = "ImportLog";

    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static int lastIdTable(String table, Context context, String user_id) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db;
        db = dbHelper.getWritableDatabase();

        int max_id = 0;
        try {
            String sqlQuewy = "select MAX(_id) "
                    + "FROM " + table + " " +
                    "where _id>? and _id<?";
            Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(Integer.parseInt(user_id) * 100000),
                    String.valueOf(Integer.parseInt(user_id) * 100000 + 999999)});
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        max_id = Integer.parseInt(c.getString(c.getColumnIndex(c.getColumnName(0))));
                        max_id++;
                    } while (c.moveToNext());
                }
            }
        } catch (Exception e) {
            max_id = Integer.parseInt(user_id) * 100000 + 1;
        }

        return max_id;
    }

    public static String nowDate() {
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

    public static String phoneEdit(String phone) {

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

    public static boolean phoneCheck(String phone) {
        boolean bool = false;
        Log.d(TAG, "phoneCheck: " + phone.length() + " " + phone.charAt(0));
        if (phone.length() == 11 && phone.charAt(0) == '7') {
            bool = true;
        }
        return bool;
    }

    public static void addHistory(String text, Context context, String id_client) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        SharedPreferences SPI = context.getSharedPreferences("user_id", MODE_PRIVATE);
        String user_id = SPI.getString("", "");

        int max_id = lastIdTable("rgzbn_gm_ceiling_client_history", context, user_id);

        String date = HelperClass.nowDate();
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID, max_id);
        values.put(DBHelper.KEY_CLIENT_ID, id_client);
        values.put(DBHelper.KEY_DATE_TIME, date);
        values.put(DBHelper.KEY_TEXT, text);
        values.put(DBHelper.KEY_TYPE_ID, "null");
        values.put(DBHelper.KEY_CHANGE_TIME, nowDate());
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, null, values);

        HelperClass.addExportData(
                context,
                max_id,
                "rgzbn_gm_ceiling_client_history",
                "send");
    }

    public static void addHistory(String text, Context context, String id_client, int type) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        SharedPreferences SPI = context.getSharedPreferences("user_id", MODE_PRIVATE);
        String user_id = SPI.getString("", "");

        int max_id = lastIdTable("rgzbn_gm_ceiling_client_history", context, user_id);

        String date = HelperClass.nowDate();
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID, max_id);
        values.put(DBHelper.KEY_CLIENT_ID, id_client);
        values.put(DBHelper.KEY_DATE_TIME, date);
        values.put(DBHelper.KEY_TEXT, text);
        values.put(DBHelper.KEY_TYPE_ID, type);
        values.put(DBHelper.KEY_CHANGE_TIME, nowDate());
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, null, values);

        HelperClass.addExportData(
                context,
                max_id,
                "rgzbn_gm_ceiling_client_history",
                "send");

    }

    public static void addCallback(String comment, Context context, String id_client, String callDate, String user_id) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Log.d("logd", "user_id: " + user_id);

        int max_id = lastIdTable("rgzbn_gm_ceiling_callback", context, user_id);

        Log.d("logd", "max_id: " + max_id);

        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID, max_id);
        values.put(DBHelper.KEY_CLIENT_ID, id_client);
        values.put(DBHelper.KEY_DATE_TIME, callDate + ":00");
        values.put(DBHelper.KEY_COMMENT, comment);
        values.put(DBHelper.KEY_MANAGER_ID, user_id);
        values.put(DBHelper.KEY_NOTIFY, "");
        values.put(DBHelper.KEY_CHANGE_TIME, nowDate());
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, null, values);

        values = new ContentValues();
        values.put(DBHelper.KEY_MANAGER_ID, user_id);
        db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, values, "_id = ?", new String[]{id_client});

        HelperClass.addExportData(
                context,
                max_id,
                "rgzbn_gm_ceiling_callback",
                "send");

        HelperClass.addExportData(
                context,
                Integer.parseInt(id_client),
                "rgzbn_gm_ceiling_clients",
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

    public static void addCallsStatusHistory(Context context, int clientId, int status, int callLength) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        SharedPreferences SPI = context.getSharedPreferences("user_id", MODE_PRIVATE);
        String user_id = SPI.getString("", "");

        int max_id = lastIdTable("rgzbn_gm_ceiling_calls_status_history", context, user_id);

        String date = HelperClass.nowDate();
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

    public static void addCallsStatusHistory(Context context, int clientId, int status, int callLength, boolean bool) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        SharedPreferences SPI = context.getSharedPreferences("user_id", MODE_PRIVATE);
        String user_id = SPI.getString("", "");

        int max_id = lastIdTable("rgzbn_gm_ceiling_calls_status_history", context, user_id);

        String date = HelperClass.nowDate();
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID, max_id);
        values.put(DBHelper.KEY_MANAGER_ID, user_id);
        values.put(DBHelper.KEY_CLIENT_ID, clientId);
        values.put(DBHelper.KEY_STATUS, status);
        values.put(DBHelper.KEY_CALL_LENGTH, callLength);
        values.put(DBHelper.KEY_CHANGE_TIME, date);
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, null, values);

        if (bool) {
            HelperClass.addExportData(
                    context,
                    max_id,
                    "rgzbn_gm_ceiling_calls_status_history",
                    "send");
        }

    }

    public static String associated_client(Context context, String user_id) {

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

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

        return associated_client;
    }

    public static String editTimeCall(String time) {
        String newTime = "";

        int min = Integer.valueOf(time) / 60;
        int sec = Integer.valueOf(time) % 60;

        if (min == 0) {
            newTime = "0";
        } else {
            newTime = String.valueOf(min);
        }

        if (sec == 0) {
            newTime += ":00";
        } else {
            newTime += ":" + String.valueOf(sec);
        }

        return newTime;
    }

    public static String publicKey(String publicKeyPEM, String text) {
        publicKeyPEM = publicKeyPEM.replace("\n", "");
        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----", "");
        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");

        byte[] keyBytes = new byte[0];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyBytes = Base64.getDecoder().decode(publicKeyPEM);
        } else {
            keyBytes = android.util.Base64.decode(publicKeyPEM, android.util.Base64.DEFAULT);
        }

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = null;
        RSAPublicKey publicKey = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "onCreate:NoSuchAlgorithmException " + e);
        } catch (InvalidKeySpecException e) {
            Log.d(TAG, "onCreate:InvalidKeySpecException " + e);
        }

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "onCreate:NoSuchAlgorithmException " + e);
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            Log.d(TAG, "onCreate:NoSuchPaddingException " + e);
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        } catch (InvalidKeyException e) {
            Log.d(TAG, "onCreate:InvalidKeyException  " + e);
        }

        byte[] encrypted = new byte[0];
        try {
            encrypted = cipher.doFinal(text.getBytes());
        } catch (IllegalBlockSizeException e) {
            Log.d(TAG, "onCreate:IllegalBlockSizeException " + e);
        } catch (BadPaddingException e) {
            Log.d(TAG, "onCreate:BadPaddingException " + e);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(encrypted);
        } else {
            return android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT);
        }
    }

    public static String SHA512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA512");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 128) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateSecret(Context context) throws NoSuchAlgorithmException {
        String md5;

        SharedPreferences SP = context.getSharedPreferences("login_user", MODE_PRIVATE);
        String login_user = SP.getString("", "");

        SP = context.getSharedPreferences("static_key", MODE_PRIVATE);
        String static_key = SP.getString("", "");

        String param = static_key + login_user;

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(param.getBytes());

        byte byteData[] = md.digest();

        //конвертируем байт в шестнадцатеричный формат первым способом
        StringBuffer sb = new StringBuffer();
        for (byte aByteData : byteData) {
            sb.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    public static String decrypt(String hash, String data, Context context) {
        String secret = "";
        try {
            secret = generateSecret(context);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] keyBytes = new byte[0];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyBytes = Base64.getDecoder().decode(data);
        } else {
            keyBytes = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
        }

        String newData = xor(keyBytes, secret);

        String newHash = SHA512(newData + secret);

        if (!newHash.equals(hash)) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyBytes = Base64.getDecoder().decode(newData);
        } else {
            keyBytes = android.util.Base64.decode(newData, android.util.Base64.DEFAULT);
        }

        newData = new String(keyBytes);

        return newData;
    }

    public static String encrypt(String data, Context context) {
        String res = "";
        String secret = "";
        try {
            secret = generateSecret(context);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            data = Base64.getEncoder().encodeToString(data.getBytes());
        } else {
            data = android.util.Base64.encodeToString(data.getBytes(), android.util.Base64.DEFAULT);
        }

        String hash = SHA512(data + secret);

        String newData = xor(data.getBytes(), secret);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            newData = Base64.getEncoder().encodeToString(newData.getBytes());
        } else {
            newData = android.util.Base64.encodeToString(newData.getBytes(), android.util.Base64.DEFAULT);
        }

        SharedPreferences SP = context.getSharedPreferences("user_id", MODE_PRIVATE);
        String keyNumber = SP.getString("", "");

        org.json.simple.JSONObject jsonObjectClient = new org.json.simple.JSONObject();
        jsonObjectClient.put("key_number", keyNumber);
        jsonObjectClient.put("data", newData);
        jsonObjectClient.put("hash", hash);
        res = String.valueOf(jsonObjectClient);

        return res;
    }

    private static String xor(byte[] data, String key) {
        byte[] result = new byte[data.length];
        byte[] keyarr = key.getBytes();

        for (int i = 0; data.length > i; i++) {
            result[i] = (byte) (data[i] ^ keyarr[i % keyarr.length]);
        }
        return new String(result);
    }
}