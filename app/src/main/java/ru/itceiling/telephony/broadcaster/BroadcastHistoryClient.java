package ru.itceiling.telephony.broadcaster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import java.util.ArrayList;
import java.util.List;

import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HistoryClient;
import ru.itceiling.telephony.R;
import ru.itceiling.telephony.activity.ClientActivity;
import ru.itceiling.telephony.adapter.RVAdapterHistoryClient;

import static android.content.Context.WINDOW_SERVICE;

public class BroadcastHistoryClient extends BroadcastReceiver {
    private WindowManager windowManager;
    private View view;
    String TAG = "logd";
    static private DBHelper dbHelper;
    static private SQLiteDatabase db;
    private List<HistoryClient> historyClients = new ArrayList<>();
    private RVAdapterHistoryClient adapter;
    private RecyclerView listHistoryClient;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(TAG, "onReceive: ");
        final String id = intent.getStringExtra("id");
        Log.d(TAG, "onReceive: id " + id);

        dbHelper = new DBHelper(context);
        db = dbHelper.getWritableDatabase();

        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        view = LayoutInflater.from(context).inflate(R.layout.history_client, null);

        listHistoryClient = view.findViewById(R.id.listHistoryClient);
        LinearLayoutManager llm = new LinearLayoutManager(context);
        listHistoryClient.setLayoutManager(llm);
        listHistoryClient.setHasFixedSize(true);
        listHistoryClient.setNestedScrollingEnabled(true);

        String name = "";
        String sqlQuewy = "SELECT ch.date_time, ch.text, ch.type_id, c.client_name "
                + "FROM rgzbn_gm_ceiling_client_history as ch " +
                "LEFT JOIN rgzbn_gm_ceiling_clients as c " +
                "ON c._id = ch.client_id " +
                "where ch.client_id = ? " +
                "order by ch.date_time";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
        if (c != null) {
            if (c.moveToFirst()) {
                name = c.getString(c.getColumnIndex(c.getColumnName(3)));
                do {
                    String date_time = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String text = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    int type = c.getInt(c.getColumnIndex(c.getColumnName(2)));

                    if (date_time.length() == 19) {
                        date_time = date_time.substring(0, date_time.length() - 3);
                    }

                    historyClients.add(new HistoryClient(date_time, text, type));

                } while (c.moveToNext());
            }
        }
        c.close();

        TextView nameClient = view.findViewById(R.id.nameClient);
        nameClient.setText(nameClient.getText() + " " + name);

        adapter = new RVAdapterHistoryClient(historyClients, context);
        listHistoryClient.setAdapter(adapter);
        listHistoryClient.scrollToPosition(adapter.getItemCount() - 1);

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