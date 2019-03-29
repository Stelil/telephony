package ru.itceiling.telephony.broadcaster;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import ru.itceiling.telephony.activity.ClientActivity;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.R;

import static android.content.Context.WINDOW_SERVICE;

public class BroadcasterCallbackClient extends BroadcastReceiver {
    private WindowManager windowManager;
    private View view;
    String TAG = "callbackClient";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(TAG, "onReceive: ");

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        final String id = intent.getStringExtra("id");

        String client_name = "";
        String phone = "";
        String comment = "";
        String date_time = "";
        String sqlQuewy = "SELECT c.client_name, cc.phone, cal.comment, cal.date_time " +
                "FROM rgzbn_gm_ceiling_clients AS c " +
                "LEFT JOIN rgzbn_gm_ceiling_clients_contacts AS cc " +
                "ON c._id = cc.client_id " +
                "LEFT JOIN rgzbn_gm_ceiling_callback AS cal " +
                "ON cal.client_id = c._id " +
                "WHERE c._id = ? " +
                "ORDER BY cal.date_time DESC";
        Cursor cc = db.rawQuery(sqlQuewy, new String[]{id});
        if (cc != null) {
            if (cc.moveToFirst()) {
                client_name = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                phone = cc.getString(cc.getColumnIndex(cc.getColumnName(1)));
                comment = cc.getString(cc.getColumnIndex(cc.getColumnName(2)));
                date_time = cc.getString(cc.getColumnIndex(cc.getColumnName(3)));
            }
        }
        cc.close();

        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);

        view = LayoutInflater.from(context).inflate(R.layout.callback_service, null);

        String message = "<b>ФИО клиента:</b> " + client_name +
                "<br><b>Комментарий:</b> " + comment +
                "<br><b>Время перезвона:</b> " + date_time.substring(0, date_time.length() - 3);
        TextView dataClient = view.findViewById(R.id.dataClient);
        dataClient.setText(Html.fromHtml(message));

        Button callClient = view.findViewById(R.id.callClient);
        final String finalPhone = phone;
        callClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+" + finalPhone));
                context.startActivity(resultIntent);
                windowManager.removeView(view);
            }
        });

        Button postponeCall = view.findViewById(R.id.postponeCall);
        postponeCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentBr = new Intent(context, BroadcastCallToPostpone.class);
                intentBr.putExtra("client_id", id);
                context.sendBroadcast(intentBr);
                windowManager.removeView(view);
            }
        });

        Button openClient = view.findViewById(R.id.openClient);
        openClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentClient = new Intent(context, ClientActivity.class);
                intentClient.putExtra("id_client", id);
                intentClient.putExtra("check", "false");
                intentClient.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intentClient);
                windowManager.removeView(view);
            }
        });

        Button closeView = view.findViewById(R.id.closeView);
        closeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                windowManager.removeView(view);
            }
        });

        //here is all the science of params
        final ViewGroup.LayoutParams myParams;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            myParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        } else {
            myParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }
        ((WindowManager.LayoutParams) myParams).gravity = Gravity.CENTER;
        // add a floatingfacebubble icon in window
        windowManager.addView(view, myParams);

        try {
            //for moving the picture on touch and slide
            view.setOnTouchListener(new View.OnTouchListener() {
                ViewGroup.LayoutParams paramsT = myParams;
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                private long touchStartTime = 0;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    //remove face bubble on long press
                    if (System.currentTimeMillis() - touchStartTime > ViewConfiguration.getLongPressTimeout() && initialTouchX == event.getX()) {
                        windowManager.removeView(view);
                        //stopSelf();
                        return false;
                    }
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            touchStartTime = System.currentTimeMillis();
                            initialX = ((WindowManager.LayoutParams) myParams).x;
                            initialY = ((WindowManager.LayoutParams) myParams).y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            break;
                        case MotionEvent.ACTION_MOVE:
                            ((WindowManager.LayoutParams) myParams).x = initialX + (int) (event.getRawX() - initialTouchX);
                            ((WindowManager.LayoutParams) myParams).y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(v, myParams);
                            break;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Uri notify = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notify);
            r.play();

            turnOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    @SuppressLint("InvalidWakeLockTag")
    public void turnOnScreen() {
        // turn on screen
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
        mWakeLock.acquire();
    }

}
