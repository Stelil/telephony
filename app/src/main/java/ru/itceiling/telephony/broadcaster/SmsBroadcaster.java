package ru.itceiling.telephony.broadcaster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.util.Log;

import ru.itceiling.telephony.activity.ClientActivity;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

import static ru.itceiling.telephony.broadcaster.CallReceiver.notifyID;

public class SmsBroadcaster extends BroadcastReceiver {

    String TAG = "sms'ka";
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        Log.d(TAG, "onReceive: start ");
        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Bundle data = intent.getExtras();

        String sender = "";
        String message = "";

        Object[] pdus = (Object[]) data.get("pdus");
        for (int i = 0; i < pdus.length; i++) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);
            sender = smsMessage.getDisplayOriginatingAddress();
            message += smsMessage.getDisplayMessageBody();
        }

        int id = 0;
        String client_name = "";
        String sqlQuewy = "SELECT cc.client_id, c.client_name"
                + " FROM rgzbn_gm_ceiling_clients_contacts as cc" +
                " INNER JOIN rgzbn_gm_ceiling_clients AS c" +
                " ON c._id = cc.client_id " +
                " WHERE cc.phone = ? AND c.deleted_by_user <> 1";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{sender.substring(1)});
        if (c != null) {
            if (c.moveToFirst()) {
                id = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
            }
        }
        c.close();

        if (id != 0) {
            HelperClass.addHistory(message, context, String.valueOf(id), 1);
            notif(id, client_name, message);
        }
    }

    void notif(int client_id, String client_name, String message) {

        //открыть клиента
        Intent intentClient = new Intent(context, ClientActivity.class);
        intentClient.putExtra("id_client", String.valueOf(client_id));
        intentClient.putExtra("check", "false");
        intentClient.setAction(Long.toString(notifyID));

        PendingIntent pendingIntentClient = PendingIntent.getActivity(
                context, 0, intentClient, PendingIntent.FLAG_ONE_SHOT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";
            CharSequence name = "1";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            Notification notification = new Notification.Builder(context)
                    .setAutoCancel(true)
                    .setTicker("СМС Сообщение")
                    .setContentTitle(client_name)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setSmallIcon(R.raw.icon_notif)
                    .setAutoCancel(true)
                    .addAction(R.raw.icon_notif,
                            "Открыть", pendingIntentClient)
                    .setStyle(new Notification.BigTextStyle().bigText(message))
                    .setContentText(message)
                    .setChannelId(CHANNEL_ID)
                    .build();

            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(mChannel);
            mNotificationManager.notify((int) notifyID, notification);

            notification.flags |= Notification.FLAG_AUTO_CANCEL;

        } else {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context)
                            .setAutoCancel(true)
                            .setTicker("СМС Сообщение")
                            .setContentTitle(client_name)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setSmallIcon(R.raw.icon_notif)
                            .setAutoCancel(true)
                            .addAction(R.raw.icon_notif,
                                    "Открыть", pendingIntentClient)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                            .setContentText(message);

            Notification notification = builder.build();
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify((int) notifyID, notification);

            notification.flags |= Notification.FLAG_AUTO_CANCEL;

        }
    }

}
