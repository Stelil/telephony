package ru.itceiling.telephony.Activity;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.amigold.fundapter.BindDictionary;
import com.amigold.fundapter.FunDapter;
import com.amigold.fundapter.extractors.StringExtractor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import ru.itceiling.telephony.AdapterList;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;
import ru.itceiling.telephony.UnderlineTextView;

public class AnalyticsActivity extends AppCompatActivity {

    TableLayout analyticsTable, titleTable;
    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id, analyticDate;
    private List<TextView> txtList = new ArrayList<>();
    private String[] arrayId;
    TextView txtSelectDay, txtSelectDayTwo;
    Calendar dateAndTime = new GregorianCalendar();
    ArrayList<AdapterList> client_mas = new ArrayList<>();
    String TAG = "logd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        analyticsTable = findViewById(R.id.analyticsTable);
        titleTable = findViewById(R.id.titleTable);

        txtSelectDay = findViewById(R.id.txtSelectDay);
        txtSelectDayTwo = findViewById(R.id.txtSelectDayTwo);

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        SharedPreferences SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        createTitleTable();
        createTable();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
            case R.id.manager:
                Intent intent = new Intent(this, ManagerActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_manager, menu);
        return true;
    }
    */

    private void createTitleTable() {

        TextView txtForHorizontalLength = findViewById(R.id.txtForHorizontalLength);

        int countApi = 0;
        String sqlQuewy = "select count(_id) "
                + "FROM rgzbn_gm_ceiling_clients_statuses ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    countApi = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        txtForHorizontalLength.setText("");

        String[] arrayStatus = new String[countApi];
        int index = 0;
        sqlQuewy = "select title "
                + "FROM rgzbn_gm_ceiling_clients_statuses ";
        c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    arrayStatus[index] = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    index++;
                } while (c.moveToNext());
            }
        }
        c.close();

        TableRow tableRow = new TableRow(this);
        TableRow.LayoutParams tableParams = new TableRow.LayoutParams(100,
                TableRow.LayoutParams.WRAP_CONTENT, 4f);

        int length = 0;
        for (int j = 0; j < countApi + 1; j++) {

            TextView txt = new TextView(this);
            txt.setLayoutParams(tableParams);

            if (j == 0) {
                txt.setText("Всего");
            } else {
                String title = arrayStatus[j - 1];
                txt.setText(title);
                length += title.length();
            }

            txt.setTextColor(Color.parseColor("#414099"));
            txt.setTextSize(13);
            txt.setGravity(Gravity.CENTER);
            tableRow.addView(txt, j);
        }
        titleTable.addView(tableRow);

        ViewGroup.LayoutParams lp = txtForHorizontalLength.getLayoutParams();
        lp.width = length * 20;
        titleTable.setLayoutParams(lp);
    }

    private void createTable() {

        analyticsTable.removeAllViews();
        txtList.clear();

        int countStatuses = 0;
        String sqlQuewy = "select count(_id) "
                + "FROM rgzbn_gm_ceiling_clients_statuses ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    countStatuses = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        int[] arrayStatusCount = new int[countStatuses];
        arrayId = new String[countStatuses];

        int countClients = 0;
        int index = 0;

        String date1 = "0001-01-01",
                date2 = HelperClass.now_date().substring(0, 10);
        if (!txtSelectDay.getText().toString().equals("")) {
            date1 = txtSelectDay.getText().toString();
        }
        if (!txtSelectDayTwo.getText().toString().equals("")) {
            date2 = txtSelectDayTwo.getText().toString();
        }

        sqlQuewy = "SELECT s._id AS status_id, " +
                "COUNT(ls.max_id) AS count, " +
                "GROUP_CONCAT(ls.client_id) AS clients, " +
                "FROM rgzbn_gm_ceiling_clients_statuses AS s " +
                "LEFT JOIN rgzbn_gm_ceiling_clients_statuses_map AS sm " +
                "ON s._id = sm.status_id " +
                "LEFT JOIN (SELECT MAX(_id) AS max_id, client_id " +
                "FROM rgzbn_gm_ceiling_clients_statuses_map " +
                "GROUP BY client_id " +
                ") AS ls " +
                "ON sm._id = ls.max_id " +
                "AND sm.change_time >= ? " +
                "AND sm.change_time <= ? " +
                "WHERE (s.dealer_id = ? " +
                "OR s.dealer_id = ?) " +
                "GROUP BY s._id " +
                "ORDER BY s._id ";
        c = db.rawQuery(sqlQuewy, new String[]{date1 + "00:00:00", date2 + "23:59:59", dealer_id, "null"});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    countClients += c.getInt(c.getColumnIndex(c.getColumnName(1)));
                    if (c.getString(c.getColumnIndex(c.getColumnName(1))).equals("0")) {
                        arrayStatusCount[index] = 0;
                        arrayId[index] = "0";
                    } else {
                        arrayStatusCount[index] = c.getInt(c.getColumnIndex(c.getColumnName(1)));
                        arrayId[index] = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    }

                    index++;
                } while (c.moveToNext());
            }
        }
        c.close();

        TableRow tableRow = new TableRow(this);
        TableRow.LayoutParams tableParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT, 4f);

        for (int j = 0; j < index + 1; j++) {

            UnderlineTextView txt = new UnderlineTextView(this);
            txt.setLayoutParams(tableParams);

            if (j == 0) {
                txt.setText(String.valueOf(countClients));
            } else {
                txt.setText(String.valueOf(arrayStatusCount[j - 1]));
            }

            txt.setTextColor(Color.parseColor("#414099"));
            txt.setTextSize(17);
            txt.setId(j);
            txt.setGravity(Gravity.CENTER);
            txt.setOnClickListener(onClickTxt);
            txtList.add(txt);
            tableRow.addView(txt, j);
        }
        analyticsTable.addView(tableRow);
    }

    View.OnClickListener onClickTxt = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            final TextView textView = txtList.get(id);
            int text = Integer.parseInt(textView.getText().toString());

            final Context context = AnalyticsActivity.this;
            LayoutInflater li = LayoutInflater.from(context);
            View promptsView = li.inflate(R.layout.activity_clients_list, null);
            AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
            mDialogBuilder.setView(promptsView);

            final AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(promptsView)
                    .setNegativeButton("Скрыть", null)
                    .create();

            Button addClient = promptsView.findViewById(R.id.AddClient);
            addClient.setVisibility(View.GONE);

            if (text == 0) {

            } else if (id == 0) {
                dialog.show();
                ListView listView = promptsView.findViewById(R.id.list_client);
                ListClients(null, listView);
            } else {
                dialog.show();
                ListView listView = promptsView.findViewById(R.id.list_client);
                ListClients((id - 1), listView);
            }
        }
    };

    private void ListClients(Integer id, ListView listView) {

        client_mas.clear();

        String date1 = "0001-01-01",
                date2 = HelperClass.now_date().substring(0, 10);
        if (!txtSelectDay.getText().toString().equals("")) {
            date1 = txtSelectDay.getText().toString();
        }
        if (!txtSelectDayTwo.getText().toString().equals("")) {
            date2 = txtSelectDayTwo.getText().toString();
        }

        String title = "";
        String[] ar;
        if (id == null) {
            ar = arrayId;
        } else {
            ar = new String[1];
            ar[0] = arrayId[id];
        }

        for (int i = 0; ar.length > i; i++) {

            String clientId = ar[i];

            if (ar[i].contains(",")) {
                for (String clienId : ar[i].split(",")) {

                    String sqlQuewy = "SELECT c.created, c.client_name, c._id, s.status_id "
                            + "FROM rgzbn_gm_ceiling_clients c " +
                            "       inner join rgzbn_gm_ceiling_clients_statuses_map s " +
                            "       on c._id = s.client_id " +
                            " WHERE c._id = ? and s.change_time > ? and s.change_time <= ?";
                    Cursor c = db.rawQuery(sqlQuewy,
                            new String[]{clienId,
                                    date1 + " 00:00:01",
                                    date2 + " 23:59:59"});
                    if (c != null) {
                        if (c.moveToLast()) {
                            String created = c.getString(c.getColumnIndex(c.getColumnName(0)));
                            String client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                            String id_client = c.getString(c.getColumnIndex(c.getColumnName(2)));

                            String status_id = c.getString(c.getColumnIndex(c.getColumnName(3)));
                            sqlQuewy = "SELECT title "
                                    + "FROM rgzbn_gm_ceiling_clients_statuses " +
                                    " WHERE _id = ? ";
                            Cursor cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(status_id)});
                            if (cc != null) {
                                if (cc.moveToFirst()) {
                                    title = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                                }
                            }
                            cc.close();

                            AdapterList fc = new AdapterList(id_client,
                                    client_name, title, created, null, null);
                            client_mas.add(fc);

                        }
                    }
                    c.close();
                }
            } else {
                String sqlQuewy = "SELECT c.created, c.client_name, c._id, s.status_id "
                        + "FROM rgzbn_gm_ceiling_clients c " +
                        "       inner join rgzbn_gm_ceiling_clients_statuses_map s " +
                        "       on c._id = s.client_id " +
                        " WHERE c._id = ? and s.change_time > ? and s.change_time <= ?";
                Cursor c = db.rawQuery(sqlQuewy,
                        new String[]{clientId,
                                date1 + " 00:00:01",
                                date2 + " 23:59:59"});
                if (c != null) {
                    if (c.moveToLast()) {
                        String created = c.getString(c.getColumnIndex(c.getColumnName(0)));
                        String client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                        String id_client = c.getString(c.getColumnIndex(c.getColumnName(2)));

                        String status_id = c.getString(c.getColumnIndex(c.getColumnName(3)));
                        sqlQuewy = "SELECT title "
                                + "FROM rgzbn_gm_ceiling_clients_statuses " +
                                " WHERE _id = ? ";
                        Cursor cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(status_id)});
                        if (cc != null) {
                            if (cc.moveToFirst()) {
                                title = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                            }
                        }
                        cc.close();
                        AdapterList fc = new AdapterList(id_client,
                                client_name, title, created, null, null);
                        client_mas.add(fc);
                    }
                }
                c.close();
            }
        }

        BindDictionary<AdapterList> dict = new BindDictionary<>();
        dict.addStringField(R.id.firstColumn, new StringExtractor<AdapterList>() {
            @Override
            public String getStringValue(AdapterList nc, int position) {
                return nc.getThree();
            }
        });
        dict.addStringField(R.id.secondColumn, new StringExtractor<AdapterList>() {
            @Override
            public String getStringValue(AdapterList nc, int position) {
                return nc.getOne();
            }
        });
        dict.addStringField(R.id.thirdColumn, new StringExtractor<AdapterList>() {
            @Override
            public String getStringValue(AdapterList nc, int position) {
                return nc.getTwo();
            }
        });

        FunDapter adapter = new FunDapter(this, client_mas, R.layout.layout_dialog_list, dict);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                AdapterList selectedid = client_mas.get(position);
                String id_client = selectedid.getId();

                Intent intent = new Intent(AnalyticsActivity.this, ClientActivity.class);
                intent.putExtra("id_client", id_client);
                intent.putExtra("check", "false");
                startActivity(intent);
            }
        });
    }

    public void onButtonSelectDay(View view) {
        setDate(txtSelectDay);
    }

    public void onButtonSelectDayTwo(View view) {
        setDateTwo(txtSelectDayTwo);
    }

    public void onButtonClearDay(View view) {
        txtSelectDay.setText("");
        txtSelectDayTwo.setText("");
        createTable();
    }

    public void setDate(View v) {
        final Calendar cal = Calendar.getInstance();
        int mYear = cal.get(Calendar.YEAR);
        int mMonth = cal.get(Calendar.MONTH);
        int mDay = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        analyticDate = "";
                        String editTextDateParam;
                        if (monthOfYear < 9) {
                            editTextDateParam = year + "-0" + (monthOfYear + 1);
                        } else {
                            editTextDateParam = year + "-" + (monthOfYear + 1);
                        }
                        if (dayOfMonth < 10) {
                            editTextDateParam = editTextDateParam + "-0" + dayOfMonth;
                        } else {
                            editTextDateParam = editTextDateParam + "-" + dayOfMonth;
                        }

                        txtSelectDay.setText(editTextDateParam);
                        analyticDate = editTextDateParam;
                        createTable();
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    DatePickerDialog.OnDateSetListener call_date = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateAndTime.set(Calendar.YEAR, year);
            dateAndTime.set(Calendar.MONTH, monthOfYear);
            dateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            setInitialDateTimeCall();
        }
    };

    private void setInitialDateTimeCall() {
        txtSelectDay.setText(txtSelectDay.getText().toString() + " " +
                DateUtils.formatDateTime(this,
                        dateAndTime.getTimeInMillis(),
                        DateUtils.FORMAT_SHOW_TIME));
        analyticDate += " " + DateUtils.formatDateTime(this,
                dateAndTime.getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME);
    }

    public void setDateTwo(View v) {
        final Calendar cal = Calendar.getInstance();
        int mYear = cal.get(Calendar.YEAR);
        int mMonth = cal.get(Calendar.MONTH);
        int mDay = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        analyticDate = "";
                        String editTextDateParam;
                        if (monthOfYear < 9) {
                            editTextDateParam = year + "-0" + (monthOfYear + 1);
                        } else {
                            editTextDateParam = year + "-" + (monthOfYear + 1);
                        }
                        if (dayOfMonth < 10) {
                            editTextDateParam = editTextDateParam + "-0" + dayOfMonth;
                        } else {
                            editTextDateParam = editTextDateParam + "-" + dayOfMonth;
                        }

                        txtSelectDayTwo.setText(editTextDateParam);
                        analyticDate = editTextDateParam;
                        createTable();
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

}
