package ru.itceiling.telephony.Activity;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.R;
import ru.itceiling.telephony.UnderlineTextView;

public class AnalyticsActivity extends AppCompatActivity {

    TableLayout analyticsTable, titleTable;
    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        analyticsTable = findViewById(R.id.analyticsTable);
        titleTable = findViewById(R.id.titleTable);

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        SharedPreferences SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        createTitleTable();
        createTable();

    }

    private void createTitleTable() {

        int countApi = 0;
        int countClient = 0;
        String sqlQuewy = "select count(_id) "
                + "FROM rgzbn_gm_ceiling_client_statuses " +
                "where dealer_id = ?";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    countApi = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        String[] arrayStatus = new String[countApi];
        int index = 0;
        sqlQuewy = "select title "
                + "FROM rgzbn_gm_ceiling_client_statuses " +
                "where dealer_id = ?";
        c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    arrayStatus[index] = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    index++;
                } while (c.moveToNext());
            }
        }
        c.close();


        sqlQuewy = "select count(_id) "
                + "FROM rgzbn_gm_ceiling_clients " +
                "where dealer_id = ?";
        c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    countClient = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        TableRow tableRow = new TableRow(this);
        TableRow.LayoutParams tableParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT, 4f);

        for (int j = 0; j < countApi + 1; j++) {

            TextView txt = new TextView(this);
            txt.setLayoutParams(tableParams);

            if (j == 0) {
                txt.setText("Всего");
            } else {
                String title = arrayStatus[j - 1];
                txt.setText(title);
            }

            txt.setTextColor(Color.parseColor("#414099"));
            txt.setTextSize(13);
            txt.setGravity(Gravity.CENTER);
            tableRow.addView(txt, j);
        }
        titleTable.addView(tableRow);

    }

    private void createTable() {
        int countApi = 0;
        String sqlQuewy = "select count(_id) "
                + "FROM rgzbn_gm_ceiling_client_statuses " +
                "where dealer_id = ?";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    countApi = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        int[] arrayStatusCount = new int[countApi];
        int index = 0;
        sqlQuewy = "select _id "
                + "FROM rgzbn_gm_ceiling_client_statuses " +
                "where dealer_id = ?";
        c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {

                    int id = c.getInt(c.getColumnIndex(c.getColumnName(0)));

                    sqlQuewy = "select count(_id) "
                            + "FROM rgzbn_gm_ceiling_clients " +
                            "where client_status = ?";
                    Cursor cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id)});
                    if (cc != null) {
                        if (cc.moveToFirst()) {
                            do {
                                arrayStatusCount[index] = cc.getInt(cc.getColumnIndex(cc.getColumnName(0)));
                                index++;
                            } while (cc.moveToNext());
                        }
                    }
                    cc.close();

                } while (c.moveToNext());
            }
        }
        c.close();

        TableRow tableRow = new TableRow(this);
        TableRow.LayoutParams tableParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT, 4f);

        for (int j = 0; j < countApi+1; j++) {

            UnderlineTextView txt = new UnderlineTextView(this);
            txt.setLayoutParams(tableParams);

            if (j == 0) {
                int countClient = 0;
                sqlQuewy = "select count(_id) "
                        + "FROM rgzbn_gm_ceiling_clients " +
                        "where dealer_id = ?";
                c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
                if (c != null) {
                    if (c.moveToFirst()) {
                        do {
                            countClient = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                        } while (c.moveToNext());
                    }
                }
                c.close();
                txt.setText(String.valueOf(countClient));

            } else {

                txt.setText(String.valueOf(arrayStatusCount[j-1]));

            }

            txt.setTextColor(Color.parseColor("#414099"));
            txt.setTextSize(13);
            txt.setGravity(Gravity.CENTER);
            tableRow.addView(txt, j);
        }
        analyticsTable.addView(tableRow);
    }
}
