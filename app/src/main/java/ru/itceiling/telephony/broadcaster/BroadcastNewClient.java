package ru.itceiling.telephony.broadcaster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

import ru.itceiling.telephony.activity.MainActivity;
import ru.itceiling.telephony.R;

import static android.content.Context.WINDOW_SERVICE;

public class BroadcastNewClient extends BroadcastReceiver {
    private WindowManager windowManager;
    private View view;

    @Override
    public void onReceive(final Context context, final Intent intent) {

        final String phone = intent.getStringExtra("phone");

        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);

        view = LayoutInflater.from(context).inflate(R.layout.new_client_service, null);

        TextView phoneClient = view.findViewById(R.id.phoneClient);
        phoneClient.setText(phoneClient.getText() + phone);

        Button addClient = view.findViewById(R.id.addClient);
        addClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("logd", "onClick: addClient");
                Intent intentClient = new Intent(context, MainActivity.class);
                intentClient.putExtra("phone", phone);
                intentClient.putExtra("add", "0");
                intentClient.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intentClient);
                windowManager.removeView(view);
            }
        });

        Button addExistClient = view.findViewById(R.id.addExistClient);
        addExistClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("logd", "onClick: addExistClient");
                Intent intentClient = new Intent(context, MainActivity.class);
                intentClient.putExtra("phone", phone);
                intentClient.putExtra("add", "1");
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
    }
}