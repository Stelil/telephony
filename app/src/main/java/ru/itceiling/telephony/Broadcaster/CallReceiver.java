package ru.itceiling.telephony.Broadcaster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import ru.itceiling.telephony.Activity.ClientsListActivity;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

import static android.content.Context.MODE_PRIVATE;

public class CallReceiver extends BroadcastReceiver {
    private String phoneNumber = "";
    private String TAG = "callReceiv";

    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private Context ctx;

    private static String mLastState = "";

    private String date1, date2;

    int callStatus = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;
        Log.d(TAG, "onReceive: ");
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            //получаем исходящий номер
            phoneNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
            callStatus = 2;
            Log.d(TAG, "1: ");
        } else if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            String phone_state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (!phone_state.equals(mLastState)) {
                mLastState = phone_state;
                if (phone_state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    //телефон звонит, получаем входящий номер
                    callStatus = 3;
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    historyClient();
                    Log.d(TAG, "2: ");
                } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    //телефон находится в режиме звонка (набор номера / разговор)
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    newClient();
                    Log.d(TAG, "3: ");
                } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    //телефон находиться в ждущем режиме. Это событие наступает по окончанию разговора, когда мы уже знаем номер и факт звонка
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    Log.d(TAG, "4: ");
                }
            }
        }
    }

    private void newClient() {

        dbHelper = new DBHelper(ctx);
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

        if (id == 0) {
            Intent resultIntent = new Intent(ctx, ClientsListActivity.class);
            resultIntent.putExtra("phone", phoneNumber);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(ctx, 0, resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            String message = "Данный клиент не найден. Хотите добавить его?";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int notifyID = 1;
                String CHANNEL_ID = "my_channel_01";
                CharSequence name = "1";
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                Notification notification = new Notification.Builder(ctx)
                        .setAutoCancel(true)
                        .setTicker("Звонок")
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setSmallIcon(R.raw.plus)
                        .addAction(R.raw.plus, "Добавить", resultPendingIntent)
                        .setStyle(new Notification.BigTextStyle().bigText(message))
                        .setContentTitle("Планер звонков")
                        .setContentText(message)
                        .setChannelId(CHANNEL_ID)
                        .setAutoCancel(true)
                        .build();

                NotificationManager mNotificationManager =
                        (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.createNotificationChannel(mChannel);
                mNotificationManager.notify(notifyID, notification);

            } else {
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(ctx)
                                .setAutoCancel(true)
                                .setTicker("Звонок")
                                .setWhen(System.currentTimeMillis())
                                .setDefaults(Notification.DEFAULT_ALL)
                                .setSmallIcon(R.raw.plus)
                                .addAction(R.raw.plus, "Добавить", resultPendingIntent)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                                .setContentTitle("Планер звонков")
                                .setAutoCancel(true)
                                .setContentText(message);
                Notification notification = builder.build();
                NotificationManager notificationManager = (NotificationManager) ctx
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(2, notification);
            }
        } else {

            SharedPreferences SP = ctx.getSharedPreferences("dealer_id", MODE_PRIVATE);
            String dealer_id = SP.getString("", "");

            int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_calls_status_history", ctx, dealer_id);

            ContentValues values = new ContentValues();
            values.put(DBHelper.KEY_ID, maxId);
            values.put(DBHelper.KEY_MANAGER_ID, dealer_id);
            values.put(DBHelper.KEY_CLIENT_ID, id);
            values.put(DBHelper.KEY_STATUS, callStatus);
            values.put(DBHelper.KEY_DATE_TIME, HelperClass.now_date());
            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, null, values);
        }
    }

    private void historyClient() {

        dbHelper = new DBHelper(ctx);
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
                Notification notification = new Notification.Builder(ctx)
                        .setAutoCancel(true)
                        .setTicker("Звонок")
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setSmallIcon(R.raw.plus)
                        .setStyle(new Notification.BigTextStyle().bigText(message))
                        .setContentTitle("Планер звонков")
                        .setContentText(message)
                        .setChannelId(CHANNEL_ID)
                        .setAutoCancel(true)
                        .build();

                NotificationManager mNotificationManager =
                        (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.createNotificationChannel(mChannel);
                mNotificationManager.notify(notifyID, notification);

            } else {
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(ctx)
                                .setAutoCancel(true)
                                .setTicker("Звонок")
                                .setWhen(System.currentTimeMillis())
                                .setDefaults(Notification.DEFAULT_ALL)
                                .setSmallIcon(R.raw.plus)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                                .setContentTitle("Планер звонков")
                                .setAutoCancel(true)
                                .setContentText(message);
                Notification notification = builder.build();
                NotificationManager notificationManager = (NotificationManager) ctx
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(2, notification);
            }
        }
    }

}