package ru.itceiling.telephony.Broadcaster;

import android.Manifest;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.itceiling.telephony.Activity.ClientActivity;
import ru.itceiling.telephony.Activity.ClientsListActivity;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

import static android.content.Context.MODE_PRIVATE;

public class CallReceiver extends BroadcastReceiver {
    static private String phoneNumber = "";
    static private String TAG = "callReceiv";

    static private DBHelper dbHelper;
    static private SQLiteDatabase db;
    static private Context ctx;

    private static String mLastState = "";

    static private String date1, date2;

    static int callStatus = 1;

    static private MediaRecorder mediaRecorder;
    static private MediaPlayer mediaPlayer;
    static private String fileName;
    static File audiofile;

    static int notifyID = 0;

    @Override
    public void onReceive(Context context, Intent intent) {

        ctx = context;
        dbHelper = new DBHelper(ctx);
        db = dbHelper.getWritableDatabase();

        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            //получаем исходящий номер

            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            telephony.listen(new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, final String number) {
                    super.onCallStateChanged(state, number);
                    phoneNumber = number;
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);

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
                    date1 = HelperClass.now_date();
                    //recordCall();
                } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    //телефон находиться в ждущем режиме. Это событие наступает по окончанию разговора, когда мы уже знаем номер и факт звонка
                    date2 = HelperClass.now_date();
                    if (date2.equals("")) {
                    } else {
                        //timeDifference();
                    }
                    newClient();
                    addHistoryClientCall();
                }
            }
        }

    }

    void timeDifference() {

        if (date2.equals("")) {
        } else {
            if (this.mediaRecorder != null) {
                this.mediaRecorder.stop();
            }
        }
    }

    void recordCall() {
        Log.d(TAG, "startRecorging");

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

    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void newClient() {

        if (phoneNumber.indexOf("+") != -1) {
            phoneNumber = phoneNumber.substring(1, phoneNumber.length());
        }

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

        long notifyID = (int) System.currentTimeMillis();
        if (id == 0) {
            Intent resultIntent = new Intent(ctx, ClientsListActivity.class);
            resultIntent.putExtra("phone", phoneNumber);
            resultIntent.putExtra("notifyID", notifyID);
            resultIntent.setAction(Long.toString(notifyID));

            PendingIntent resultPendingIntent = PendingIntent.getActivity(ctx, 0, resultIntent,
                    PendingIntent.FLAG_ONE_SHOT);

            String message = "Данный клиент не найден. Хотите добавить его?" +
                    "\nНомер клиента: " + phoneNumber +
                    "\nВремя звонка: " + HelperClass.now_date().substring(0, 16);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String CHANNEL_ID = "my_channel_01";
                CharSequence name = "1";
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                Notification notification = new Notification.Builder(ctx)
                        .setAutoCancel(true)
                        .setTicker("Звонок")
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
                mNotificationManager.notify((int) notifyID, notification);

                notification.flags |= Notification.FLAG_AUTO_CANCEL;
            } else {
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(ctx)
                                .setAutoCancel(true)
                                .setTicker("Звонок")
                                .setDefaults(Notification.DEFAULT_ALL)
                                .setSmallIcon(R.raw.icon_notif)
                                .addAction(R.raw.plus, "Добавить", resultPendingIntent)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                                .setContentTitle("Планер звонков")
                                .setAutoCancel(true)
                                .setContentText(message);
                Notification notification = builder.build();
                NotificationManager notificationManager = (NotificationManager) ctx
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify((int) notifyID, notification);

                notification.flags |= Notification.FLAG_AUTO_CANCEL;
            }
        }
    }

    private void addHistoryClientCall() {

        if (phoneNumber.contains("+")) {
            phoneNumber = phoneNumber.substring(1, phoneNumber.length());
        }

        Log.d(TAG, "addHistoryClientCall: " + phoneNumber);

        SharedPreferences SP = ctx.getSharedPreferences("JsonCheckTime", MODE_PRIVATE);
        String checkTime = SP.getString("", "");

        JSONObject json = null;
        try {
            json = new JSONObject(checkTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Date one = null, two = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        int duration = Integer.parseInt(getCallDetails());
        try {
            if (duration >= Integer.valueOf(json.getString("CheckTimeCall"))) {
                int client_id = 0;
                String sqlQuewy = "SELECT client_id "
                        + "FROM rgzbn_gm_ceiling_clients_contacts" +
                        " WHERE phone = ? ";
                Cursor c = db.rawQuery(sqlQuewy, new String[]{phoneNumber});
                if (c != null) {
                    if (c.moveToFirst()) {
                        client_id = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                        String text = "";
                        switch (callStatus) {
                            case 2:
                                text = "Исходящий дозвон. \nДлина разговора = " + duration + " секунд(a)";
                                break;
                            case 3:
                                text = "Входящий дозвон. \nДлина разговора = " + duration + " секунд(a)";
                                break;
                        }

                        HelperClass.addHistory(text, ctx, String.valueOf(client_id));

                        HelperClass.addCallsStatusHistory(ctx, client_id, callStatus, duration);
                    }
                }
                c.close();
            }
        } catch (JSONException e) {
        }

        boolean call = true;
        if (callStatus == 1) {
            int client_id = 0;
            String sqlQuewy = "SELECT client_id "
                    + "FROM rgzbn_gm_ceiling_clients_contacts" +
                    " WHERE phone = ? ";
            Cursor c = db.rawQuery(sqlQuewy, new String[]{phoneNumber});
            if (c != null) {
                if (c.moveToFirst()) {
                    client_id = c.getInt(c.getColumnIndex(c.getColumnName(0)));

                    String text = "Исходящий недозвон";
                    HelperClass.addHistory(text, ctx, String.valueOf(client_id));

                    HelperClass.addCallsStatusHistory(ctx, client_id, callStatus, 0);

                    call = false;
                }
            }
            c.close();
        }

        if (duration <= 0 && call) {
            int client_id = 0;
            String sqlQuewy = "SELECT client_id "
                    + "FROM rgzbn_gm_ceiling_clients_contacts" +
                    " WHERE phone = ? ";
            Cursor c = db.rawQuery(sqlQuewy, new String[]{phoneNumber});
            if (c != null) {
                if (c.moveToFirst()) {
                    client_id = c.getInt(c.getColumnIndex(c.getColumnName(0)));

                    String text = "Пропущенный звонок";
                    HelperClass.addHistory(text, ctx, String.valueOf(client_id));
                }
            }
            c.close();
        }
    }

    private void historyClient() {

        if (phoneNumber.contains("+")) {
            phoneNumber = phoneNumber.substring(1, phoneNumber.length());
        }

        Log.d(TAG, "historyClient: " + phoneNumber);

        dbHelper = new DBHelper(ctx);
        db = dbHelper.getWritableDatabase();
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
                    message += c.getString(c.getColumnIndex(c.getColumnName(0))) + " ";
                    message += c.getString(c.getColumnIndex(c.getColumnName(1))) + "\n";
                    c.moveToNext();
                    message += c.getString(c.getColumnIndex(c.getColumnName(0))) + " ";
                    message += c.getString(c.getColumnIndex(c.getColumnName(1))) + "\n";
                }
            }
            c.close();

            String client_name = "";
            sqlQuewy = "SELECT client_name "
                    + "FROM rgzbn_gm_ceiling_clients" +
                    " WHERE _id = ?";
            c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id)});
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        client_name = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    } while (c.moveToNext());
                }
            }
            c.close();

            long notifyID = (int) System.currentTimeMillis();

            Intent intentClient = new Intent(ctx, ClientActivity.class);
            intentClient.putExtra("id_client", String.valueOf(id));
            intentClient.putExtra("check", "false");
            intentClient.setAction(String.valueOf(notifyID));

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(ctx);
            stackBuilder.addParentStack(ClientsListActivity.class);
            stackBuilder.addNextIntent(intentClient);

            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String CHANNEL_ID = "my_channel_01";
                CharSequence name = "1";
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                Notification notification = new Notification.Builder(ctx)
                        .setAutoCancel(true)
                        .setTicker("Звонок")
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setSmallIcon(R.raw.icon_notif)
                        .setStyle(new Notification.BigTextStyle().bigText(message))
                        .setContentTitle(client_name)
                        .setContentText(message)
                        .setChannelId(CHANNEL_ID)
                        .setAutoCancel(true)
                        .setContentIntent(resultPendingIntent)
                        .build();

                NotificationManager mNotificationManager =
                        (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.createNotificationChannel(mChannel);
                mNotificationManager.notify((int) notifyID, notification);

                notification.flags |= Notification.FLAG_AUTO_CANCEL;

            } else {

                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(ctx)
                                .setAutoCancel(true)
                                .setTicker("Звонок")
                                .setDefaults(Notification.DEFAULT_ALL)
                                .setSmallIcon(R.raw.icon_notif)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                                .setContentTitle(client_name)
                                .setAutoCancel(true)
                                .setContentIntent(resultPendingIntent)
                                .setContentText(message);
                Notification notification = builder.build();
                NotificationManager notificationManager = (NotificationManager) ctx
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify((int) notifyID, notification);

                notification.flags |= Notification.FLAG_AUTO_CANCEL;
            }
        }
    }

    static private String getCallDetails() {

        StringBuffer sb = new StringBuffer("0");
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
        } else {
            Cursor managedCursor = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    null,
                    null,
                    null,
                    null);
            int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
            managedCursor.moveToLast();
            String callDuration = managedCursor.getString(duration);

            sb.append(callDuration);

            managedCursor.close();
        }

        return sb.toString();
    }
}