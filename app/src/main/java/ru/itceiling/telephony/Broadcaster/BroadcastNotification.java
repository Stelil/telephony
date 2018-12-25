package ru.itceiling.telephony.Broadcaster;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;

public class BroadcastNotification extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("serviceCall",
                "onReceive: BroadcastNotification " + intent.getStringExtra("client_id"));

        String client_id = intent.getStringExtra("client_id");
        int notific = intent.getIntExtra("notifyID",0);
        Log.d("serviceCallback", "client_id: " + client_id);
        Log.d("serviceCallback", "notifyID: " + notific);

        String sqlQuewy;
        Cursor c;
        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db;
        db = dbHelper.getWritableDatabase();

        sqlQuewy = "SELECT _id, date_time "
                + "FROM rgzbn_gm_ceiling_callback " +
                "where client_id = ? " +
                "order by date_time desc";
        c = db.rawQuery(sqlQuewy, new String[]{client_id});
        if (c != null) {
            if (c.moveToFirst()) {
                String id = c.getString(c.getColumnIndex(c.getColumnName(0)));
                String date_time = c.getString(c.getColumnIndex(c.getColumnName(1)));

                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                DateTimeFormatter outDf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                Log.d("serviceCallback", "onReceive: " + date_time);
                LocalDateTime dateTime = LocalDateTime
                        .parse(date_time,df)
                        .plusMinutes(10);
                String dateEnd = dateTime.format(outDf);

                ContentValues values = new ContentValues();
                values.put(DBHelper.KEY_DATE_TIME, dateEnd.substring(1,16));
                values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.now_date());
                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, values, "_id = ?", new String[]{id});

                Log.d("serviceCallback", "onReceive: " + dateEnd);

                HelperClass.addExportData(
                        context,
                        Integer.parseInt(id),
                        "rgzbn_gm_ceiling_callback",
                        "send");

                HelperClass.addHistory("Звонок перенесён на " + dateEnd,
                        context,
                        client_id);

                NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);;
                notificationManager.cancel(notific);

                Toast toast = Toast.makeText(context,
                        "Звонок перенесён на 10 минут", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        c.close();

    }

}