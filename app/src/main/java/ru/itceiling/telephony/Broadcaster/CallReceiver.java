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
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    static private String date1, date2;

    int callStatus = 0;

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String fileName;
    File audiofile;

    @Override
    public void onReceive(Context context, Intent intent) {

        ctx = context;
        dbHelper = new DBHelper(ctx);
        db = dbHelper.getWritableDatabase();

        Log.d(TAG, "onReceive: ");
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            //получаем исходящий номер
            phoneNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
            callStatus = 2;
        } else if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            String phone_state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (!phone_state.equals(mLastState)) {
                mLastState = phone_state;
                if (phone_state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    //телефон звонит, получаем входящий номер
                    callStatus = 3;
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    historyClient();
                } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    //телефон находится в режиме звонка (набор номера / разговор)
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    date1 = HelperClass.now_date();
                    Log.d(TAG, "date1: " + date1);
                    newClient();
                    recordCall();
                } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    //телефон находиться в ждущем режиме. Это событие наступает по окончанию разговора, когда мы уже знаем номер и факт звонка
                    timeDifference();
                    date2 = HelperClass.now_date();
                    Log.d(TAG, "date2: " + date2);
                    addHistoryClientCall();
                }
            }
        }

        Log.d(TAG, "date1: " + date1);
        Log.d(TAG, "date2: " + date2);

    }

    void timeDifference() {
        if (this.mediaRecorder != null) {
            this.mediaRecorder.stop();
        }
    }

    void recordCall() {
        Log.d(TAG, "startRecorging");

        //try {
        releaseRecorder();

        String formatDateTime = HelperClass.now_date();

        Log.d(TAG, "recordCall: " + formatDateTime);
        if (audiofile == null) {
            File sampleDir = new File("/storage/emulated/0/" + formatDateTime + ".flac");
            audiofile = sampleDir;
        }

        mediaRecorder = new MediaRecorder();
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer.toLowerCase().contains("samsung")) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        } else {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        }
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audiofile.getAbsolutePath());
        Log.d(TAG, "recordCall: " + audiofile);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
        }
        mediaRecorder.start();

        //} catch (Exception e) {
        //    Log.d(TAG, "recordCall er: 3" + e);
        //}

    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void newClient() {

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
                        .setSmallIcon(R.raw.icon_notif)
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
                                .setSmallIcon(R.raw.icon_notif)
                                .addAction(R.raw.plus, "Добавить", resultPendingIntent)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                                .setContentTitle("Планер звонков")
                                .setAutoCancel(true)
                                .setContentText(message);
                Notification notification = builder.build();
                NotificationManager notificationManager = (NotificationManager)
                        ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(2, notification);
            }
        }
    }

    private void addHistoryClientCall() {

        SharedPreferences SP = ctx.getSharedPreferences("JsonCheckTime", MODE_PRIVATE);
        String checkTime = SP.getString("", "");

        Date one = null, two = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            one = format.parse(date1);
            two = format.parse(date2);
        } catch (Exception e) {
        }

        long difference = two.getTime() - one.getTime();

        int min = (int) (difference / 1000); // миллисекунды / (24ч * 60мин * 60сек * 1000мс)

        Log.d(TAG, "addHistoryClientCall min : " + min);
        Log.d(TAG, "addHistoryClientCall checkTime : " + checkTime);

        if (min == Integer.valueOf(checkTime)) {
            phoneNumber = phoneNumber.substring(1, phoneNumber.length());
            int id = 0;
            String sqlQuewy = "SELECT client_id "
                    + "FROM rgzbn_gm_ceiling_clients_contacts" +
                    " WHERE phone = ? ";
            Cursor c = db.rawQuery(sqlQuewy, new String[]{phoneNumber});
            if (c != null) {
                if (c.moveToFirst()) {
                    id = c.getInt(c.getColumnIndex(c.getColumnName(0)));

                    String text = "";
                    switch (callStatus) {
                        case 1:
                            text = "Исходящий недозвон";
                            break;
                        case 2:
                            text = "Исходящий дозвон";
                            break;
                        case 3:
                            text = "Входящий дозвон";
                            break;
                    }

                    HelperClass.addHistory(text, ctx, String.valueOf(id));
                }
            }
            c.close();
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
                        .setSmallIcon(R.raw.icon_notif)
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
                                .setSmallIcon(R.raw.icon_notif)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                                .setContentTitle("Планер звонков")
                                .setAutoCancel(true)
                                .setContentText(message);
                Notification notification = builder.build();
                NotificationManager notificationManager = (NotificationManager)
                        ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(2, notification);
            }
        }
    }

}