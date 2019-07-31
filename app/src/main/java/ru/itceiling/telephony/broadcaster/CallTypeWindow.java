package ru.itceiling.telephony.broadcaster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

import static android.content.Context.WINDOW_SERVICE;

public class CallTypeWindow extends BroadcastReceiver {
    private WindowManager windowManager;
    private View view;
    String TAG = "logd";
    static private DBHelper dbHelper;
    static private SQLiteDatabase db;
    Context ctx;
    String client_id = "";
    boolean bool;

    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;

        String id = intent.getStringExtra("id");
        this.client_id = id;
        boolean bool = intent.getBooleanExtra("bool", false);
        this.bool = bool;

        dbHelper = new DBHelper(context);
        db = dbHelper.getWritableDatabase();

        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        view = LayoutInflater.from(context).inflate(R.layout.call_type_window, null);

        LinearLayout made = view.findViewById(R.id.linearMade);
        made.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertCallHistory(2);
            }
        });

        LinearLayout linearReceived = view.findViewById(R.id.linearReceived);
        linearReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertCallHistory(3);
            }
        });

        LinearLayout linearMissed = view.findViewById(R.id.linearMissed);
        linearMissed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertCallHistory(1);
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
    }

    private void insertCallHistory(int status) {
        String text = "";
        switch (status) {
            case 1:
                text = "Исходящий дозвон. \nДлина разговора = " + HelperClass.editTimeCall(String.valueOf(status));
                break;
            case 2:
                text = "Входящий звонок. \nДлина разговора = " + HelperClass.editTimeCall(String.valueOf(status));
                break;
        }
        HelperClass.addHistory(text, ctx, client_id, bool);
        HelperClass.addCallsStatusHistory(ctx, Integer.parseInt(client_id), status, 0, bool);
    }

}
