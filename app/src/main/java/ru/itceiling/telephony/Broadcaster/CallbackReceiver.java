package ru.itceiling.telephony.Broadcaster;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;

import ru.itceiling.telephony.Activity.ClientActivity;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

import static android.content.Context.MODE_PRIVATE;

public class CallbackReceiver extends BroadcastReceiver {

    static DBHelper dbHelper;
    private static final String TAG = "serviceCallback";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "run");
        dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sqlQuewy = "SELECT cb.client_id, cb.date_time "
                + "FROM rgzbn_gm_ceiling_callback as cb " +
                "order by date_time DESC";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String client_id = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String date_time = c.getString(c.getColumnIndex(c.getColumnName(1)));

                    String now_date = HelperClass.nowDate();
                    now_date = now_date.substring(0, now_date.length());

                    Date one = null;
                    Date two = null;

                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                    try {
                        one = format.parse(date_time);
                        two = format.parse(now_date);
                    } catch (Exception e) {
                    }

                    if (client_id.equals("")) {
                    } else if (two.getTime() < one.getTime()) {

                        SharedPreferences SP = context.getSharedPreferences("dealer_id", MODE_PRIVATE);
                        String dealer_id = SP.getString("", "");

                        int checkTimeCallback = 0;
                        String stringToParse = "";
                        sqlQuewy = "SELECT settings "
                                + "FROM rgzbn_users " +
                                "WHERE _id = ? ";
                        Cursor cc = db.rawQuery(sqlQuewy, new String[]{dealer_id});
                        if (cc != null) {
                            if (cc.moveToFirst()) {
                                do {
                                    stringToParse = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                                } while (cc.moveToNext());
                            }
                        }
                        cc.close();

                        try {
                            org.json.JSONObject json = new org.json.JSONObject(stringToParse);
                            checkTimeCallback = json.getInt("CheckTimeCallback");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.d(TAG, "onReceive: checkTimeCallback " + checkTimeCallback);
                        try {
                            one = format.parse(date_time);
                            two = format.parse(now_date);
                        } catch (Exception e) {
                        }

                        long difference = one.getTime() - two.getTime();
                        int min = (int) (difference / (60 * 1000)); // миллисекунды / (24ч * 60мин * 60сек * 1000мс)

                        if (min == checkTimeCallback) {

                            intent = new Intent(context, BroadcasterCallbackClient.class);
                            intent.putExtra("id", client_id);
                            context.sendBroadcast(intent);

                        }
                    }
                } while (c.moveToNext());
            }
        }
        c.close();
    }

    public void SetAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CallbackReceiver.class);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra("onetime", Boolean.FALSE);//Задаем параметр интента
        PendingIntent pi = PendingIntent.getBroadcast(context,
                0,
                intent,
                0);
        am.setRepeating(AlarmManager.RTC,
                System.currentTimeMillis(),
                60000,
                pi);

    }

    public void CancelAlarm(Context context) {
        Intent intent = new Intent(context, CallbackReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);//Отменяем будильник, связанный с интентом данного класса
    }
}
