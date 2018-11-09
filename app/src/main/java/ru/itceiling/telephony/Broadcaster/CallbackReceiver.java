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

        String sqlQuewy = "SELECT cb.client_id, cb.date_time, cb.comment, cl.client_name, cl._id "
                + "FROM rgzbn_gm_ceiling_callback as cb INNER JOIN " +
                " rgzbn_gm_ceiling_clients as cl ON cb.client_id = cl._id " +
                "order by date_time DESC";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String client_id = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String date_time = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    String comment = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    String client_name = c.getString(c.getColumnIndex(c.getColumnName(3)));

                    String now_date = HelperClass.now_date();
                    now_date = now_date.substring(0, now_date.length());

                    Date one = null;
                    Date two = null;

                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                    try {
                        one = format.parse(date_time);
                        two = format.parse(now_date);
                    } catch (Exception e) {
                    }

                    if (client_id.equals("") ) {
                    } else if (two.getTime()<one.getTime()){

                        int checkTimeCallback = 0;
                        SharedPreferences SP = context.getSharedPreferences("JsonCheckTime", MODE_PRIVATE);
                        String jsonObject = SP.getString("", "");
                        try {
                            org.json.JSONObject json = new org.json.JSONObject(jsonObject);
                            checkTimeCallback = json.getInt("CheckTimeCallback");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            one = format.parse(date_time);
                            two = format.parse(now_date);
                        } catch (Exception e) {
                        }

                        long difference = one.getTime() - two.getTime();
                        int min = (int) (difference / (60 * 1000)); // миллисекунды / (24ч * 60мин * 60сек * 1000мс)

                        if (min == checkTimeCallback) {

                            String phone = "";
                            sqlQuewy = "SELECT phone "
                                    + "FROM rgzbn_gm_ceiling_clients_contacts" +
                                    " WHERE client_id = ?";
                            Cursor cc = db.rawQuery(sqlQuewy, new String[]{client_id});
                            if (cc != null) {
                                if (cc.moveToFirst()) {
                                    phone = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                                }
                            }
                            cc.close();

                            String message = "Комментарий: " + comment + "\nФИО клиента: " + client_name;
                            Intent resultIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+" + phone));

                            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                int notifyID = 1;
                                String CHANNEL_ID = "my_channel_01";
                                CharSequence name = "1";
                                int importance = NotificationManager.IMPORTANCE_HIGH;
                                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                                Notification notification = new Notification.Builder(context)
                                        .setAutoCancel(true)
                                        .setTicker("Звонок")
                                        .setWhen(System.currentTimeMillis())
                                        .setDefaults(Notification.DEFAULT_ALL)
                                        .setSmallIcon(R.raw.plus)
                                        .setAutoCancel(true)
                                        .addAction(R.raw.plus, "Позвонить", resultPendingIntent)
                                        .setStyle(new Notification.BigTextStyle().bigText(message))
                                        .setContentTitle("Планер звонков")
                                        .setContentText(message)
                                        .setChannelId(CHANNEL_ID)
                                        .build();

                                NotificationManager mNotificationManager =
                                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                mNotificationManager.createNotificationChannel(mChannel);

                                mNotificationManager.notify(notifyID, notification);
                            } else {
                                NotificationCompat.Builder builder =
                                        new NotificationCompat.Builder(context)
                                                .setAutoCancel(true)
                                                .setTicker("Звонок")
                                                .setWhen(System.currentTimeMillis())
                                                .setDefaults(Notification.DEFAULT_ALL)
                                                .setSmallIcon(R.raw.plus)
                                                .setAutoCancel(true)
                                                .addAction(R.raw.plus, "Позвонить", resultPendingIntent)
                                                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                                                .setContentTitle("Планер звонков")
                                                .setContentText(message);
                                Notification notification = builder.build();
                                NotificationManager notificationManager = (NotificationManager) context
                                        .getSystemService(Context.NOTIFICATION_SERVICE);
                                notificationManager.notify(2, notification);
                            }
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
        intent.putExtra("onetime", Boolean.FALSE);//Задаем параметр интента
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60, pi);
    }

    public void CancelAlarm(Context context)
    {
        Intent intent=new Intent(context, CallbackReceiver.class);
        PendingIntent sender= PendingIntent.getBroadcast(context,0, intent,0);
        AlarmManager alarmManager=(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);//Отменяем будильник, связанный с интентом данного класса
    }
}
