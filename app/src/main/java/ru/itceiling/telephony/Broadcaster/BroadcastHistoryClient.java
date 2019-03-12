package ru.itceiling.telephony.Broadcaster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;

import ru.itceiling.telephony.Activity.ClientActivity;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.R;

import static android.content.Context.WINDOW_SERVICE;

public class BroadcastHistoryClient extends BroadcastReceiver {
    private WindowManager windowManager;
    private View view;
    String TAG = "logd";
    static private DBHelper dbHelper;
    static private SQLiteDatabase db;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(TAG, "onReceive: ");
        final String id = intent.getStringExtra("id");
        Log.d(TAG, "onReceive: id " + id);

        dbHelper = new DBHelper(context);
        db = dbHelper.getWritableDatabase();

        ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
        HashMap<String, String> history;
        String sqlQuewy = "SELECT date_time, text "
                + "FROM rgzbn_gm_ceiling_client_history" +
                " WHERE client_id = ? " +
                "order by date_time";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    history = new HashMap<>();
                    history.put("date_time", c.getString(c.getColumnIndex(c.getColumnName(0))));
                    history.put("text", c.getString(c.getColumnIndex(c.getColumnName(1))));
                    arrayList.add(history);
                } while (c.moveToNext());
            }
        }
        c.close();

        String client_name = "";
        sqlQuewy = "SELECT client_name "
                + "FROM rgzbn_gm_ceiling_clients" +
                " WHERE _id = ? and deleted_by_user = ?";
        c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id), "0"});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    client_name = c.getString(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        view = LayoutInflater.from(context).inflate(R.layout.history_client, null);

        TextView nameClient = view.findViewById(R.id.nameClient);
        nameClient.setText(nameClient.getText() + " " + client_name);

        ListView listView = view.findViewById(R.id.listHistoryClient);
        SimpleAdapter adapter = new SimpleAdapter(context, arrayList, android.R.layout.simple_list_item_2,
                new String[]{"date_time", "text"},
                new int[]{android.R.id.text1, android.R.id.text2});
        listView.setAdapter(adapter);

        Button closeView = view.findViewById(R.id.closeView);
        closeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
}