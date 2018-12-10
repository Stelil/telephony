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
import ru.itceiling.telephony.R;
import ru.itceiling.telephony.UnderlineTextView;

public class AnalyticsActivity extends AppCompatActivity {

    TableLayout analyticsTable, titleTable;
    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id, analyticDate;
    private List<TextView> txtList = new ArrayList<>();
    private int[] arrayId;
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void createTitleTable() {

        int countApi = 0;
        int countClient = 0;
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
        TableRow.LayoutParams tableParams = new TableRow.LayoutParams(100,
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
        arrayId = new int[countStatuses];

        int countClients = 0;
        int index = 0;
        sqlQuewy = "select _id "
                + "FROM rgzbn_gm_ceiling_clients_statuses ";
        c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    int id = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                    arrayId[index] = id;
                    Cursor cc = null;
                    if (txtSelectDay.getText().toString().equals("") && txtSelectDayTwo.getText().toString().equals("")) {
                        sqlQuewy = "select count(client_id) "
                                + "FROM rgzbn_gm_ceiling_clients_statuses_map " +
                                "where status_id = ? ";
                        cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id)});
                    } else {
                        if (!txtSelectDay.getText().toString().equals("") && txtSelectDayTwo.getText().toString().equals("")) {
                            sqlQuewy = "select _id "
                                    + "FROM rgzbn_gm_ceiling_clients_statuses_map " +
                                    "where status_id = ? and change_time > ?";
                            cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id), txtSelectDay.getText().toString() + " 00:00:01"});
                        } else if (txtSelectDay.getText().toString().equals("") && !txtSelectDayTwo.getText().toString().equals("")) {
                            sqlQuewy = "select count(_id) "
                                    + "FROM rgzbn_gm_ceiling_clients_statuses_map " +
                                    "where status_id = ? and change_time < ?";
                            cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id), txtSelectDayTwo.getText().toString() + " 23:59:59"});
                        } else if (!txtSelectDay.getText().toString().equals("") && !txtSelectDayTwo.getText().toString().equals("")) {
                            sqlQuewy = "select count(_id) "
                                    + "FROM rgzbn_gm_ceiling_clients_statuses_map " +
                                    "where status_id = ? and change_time > ? and change_time < ?";
                            cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id), txtSelectDay.getText().toString() + " 00:00:01",
                                    txtSelectDayTwo.getText().toString() + " 23:59:59"});
                        }
                    }
                    if (cc != null) {
                        if (cc.moveToLast()) {
                            Log.d(TAG, "createTable: 2 id = " + c.getInt(c.getColumnIndex(c.getColumnName(0))));
                            Log.d(TAG, "createTable: count = " + cc.getString(cc.getColumnIndex(cc.getColumnName(0))));
                            arrayStatusCount[index] = cc.getInt(cc.getColumnIndex(cc.getColumnName(0)));
                            index++;
                            countClients += cc.getInt(cc.getColumnIndex(cc.getColumnName(0)));
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

            if (text == 0) {

            } else if (id == 0) {
                Log.d(TAG, "onClick: ");
                final Context context = AnalyticsActivity.this;
                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.activity_clients_list, null);
                AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
                mDialogBuilder.setView(promptsView);

                final AlertDialog dialog = new AlertDialog.Builder(context)
                        .setView(promptsView)
                        .setNegativeButton("Скрыть", null)
                        .create();

                Button addCleint = promptsView.findViewById(R.id.AddClient);
                addCleint.setVisibility(View.GONE);
                dialog.show();

                ListView listView = promptsView.findViewById(R.id.list_client);
                ListClients(listView);

            } else {
                final Context context = AnalyticsActivity.this;
                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.activity_clients_list, null);
                AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
                mDialogBuilder.setView(promptsView);

                final AlertDialog dialog = new AlertDialog.Builder(context)
                        .setView(promptsView)
                        .setNegativeButton("Скрыть", null)
                        .create();

                Button addCleint = promptsView.findViewById(R.id.AddClient);
                addCleint.setVisibility(View.GONE);
                dialog.show();

                ListView listView = promptsView.findViewById(R.id.list_client);
                ListClients(arrayId[id - 1], listView);
            }

        }
    };

    private void ListClients(int idStatus, ListView listView) {

        client_mas.clear();

        String title = "";
        String sqlQuewy = "SELECT title "
                + "FROM rgzbn_gm_ceiling_clients_statuses" +
                " WHERE _id = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(idStatus)});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    title = c.getString(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        if (txtSelectDay.getText().toString().equals("") && txtSelectDayTwo.getText().toString().equals("")) {
            sqlQuewy = "SELECT c.created, c.client_name, c._id "
                    + "FROM rgzbn_gm_ceiling_clients c " +
                    "       inner join rgzbn_gm_ceiling_clients_statuses_map s " +
                    "       on c._id = s.client_id" +
                    " WHERE s.status_id = ?";
            c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(idStatus)});
        } else {
            if (!txtSelectDay.getText().toString().equals("") && txtSelectDayTwo.getText().toString().equals("")) {
                sqlQuewy = "SELECT c.created, c.client_name, c._id "
                        + "FROM rgzbn_gm_ceiling_clients c " +
                        "       inner join rgzbn_gm_ceiling_clients_statuses_map s " +
                        "       on c._id = s.client_id" +
                        " WHERE s.status_id = ? and c.change_time > ?";
                c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(idStatus), txtSelectDay.getText().toString() + " 00:00:01"});
            } else if (txtSelectDay.getText().toString().equals("") && !txtSelectDayTwo.getText().toString().equals("")) {
                sqlQuewy = "SELECT created, client_name, _id "
                        + "FROM rgzbn_gm_ceiling_clients " +
                        " WHERE s.status_id = ? and c.change_time < ?";
                c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(idStatus), txtSelectDayTwo.getText().toString() + " 23:59:59"});
            } else if (!txtSelectDay.getText().toString().equals("") && !txtSelectDayTwo.getText().toString().equals("")) {
                sqlQuewy = "SELECT created, client_name, _id "
                        + "FROM rgzbn_gm_ceiling_clients " +
                        " WHERE s.status_id = ? and c.change_time > ? and c.change_time < ?";
                c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(idStatus), txtSelectDay.getText().toString() + " 00:00:01",
                        txtSelectDayTwo.getText().toString() + " 23:59:59"});
            }
        }
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String created = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    String id_client = c.getString(c.getColumnIndex(c.getColumnName(2)));

                    AdapterList fc = new AdapterList(id_client,
                            client_name, title, created, null, null);
                    client_mas.add(fc);

                } while (c.moveToNext());
            }
        }
        c.close();

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
                startActivity(intent);
            }
        });
    }

    private void ListClients(ListView listView) {

        client_mas.clear();

        String sqlQuewy;
        Cursor c = null;

        if (txtSelectDay.getText().toString().equals("") && txtSelectDayTwo.getText().toString().equals("")) {
            sqlQuewy = "SELECT c.created, c.client_name, c._id "
                    + "FROM rgzbn_gm_ceiling_clients c " +
                    "       inner join rgzbn_gm_ceiling_clients_statuses_map s " +
                    "       on c._id = s.client_id";
            c = db.rawQuery(sqlQuewy, new String[]{});
        } else {
            if (!txtSelectDay.getText().toString().equals("") && txtSelectDayTwo.getText().toString().equals("")) {
                sqlQuewy = "SELECT c.created, c.client_name, c._id "
                        + "FROM rgzbn_gm_ceiling_clients c " +
                        "       inner join rgzbn_gm_ceiling_clients_statuses_map s " +
                        "       on c._id = s.client_id " +
                        "where c.change_time > ?";
                c = db.rawQuery(sqlQuewy, new String[]{txtSelectDay.getText().toString() + " 00:00:01"});
            } else if (txtSelectDay.getText().toString().equals("") && !txtSelectDayTwo.getText().toString().equals("")) {
                sqlQuewy = "SELECT c.created, c.client_name, c._id "
                        + "FROM rgzbn_gm_ceiling_clients c " +
                        "       inner join rgzbn_gm_ceiling_clients_statuses_map s " +
                        "       on c._id = s.client_id " +
                        "where c.change_time < ?";
                c = db.rawQuery(sqlQuewy, new String[]{txtSelectDayTwo.getText().toString() + " 23:59:59"});
            } else if (!txtSelectDay.getText().toString().equals("") && !txtSelectDayTwo.getText().toString().equals("")) {
                sqlQuewy = "SELECT c.created, c.client_name, c._id "
                        + "FROM rgzbn_gm_ceiling_clients c " +
                        "       inner join rgzbn_gm_ceiling_clients_statuses_map s " +
                        "       on c._id = s.client_id " +
                        "where c.change_time > ? and change_time < ?";
                c = db.rawQuery(sqlQuewy, new String[]{txtSelectDay.getText().toString() + " 00:00:01",
                        txtSelectDayTwo.getText().toString() + " 23:59:59"});
            }
        }
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String created = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    String id_client = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    int status_id = 0;
                    String title = null;

                    sqlQuewy = "SELECT status_id "
                            + "FROM rgzbn_gm_ceiling_clients_statuses_map " +
                            " WHERE client_id = ? ";
                    Cursor cc = db.rawQuery(sqlQuewy, new String[]{id_client});
                    if (cc != null) {
                        if (cc.moveToFirst()) {
                            status_id = cc.getInt(cc.getColumnIndex(cc.getColumnName(0)));
                        }
                    }
                    cc.close();

                    sqlQuewy = "SELECT title "
                            + "FROM rgzbn_gm_ceiling_clients_statuses " +
                            " WHERE _id = ? ";
                    cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(status_id)});
                    if (cc != null) {
                        if (cc.moveToFirst()) {
                            title = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                        }
                    }
                    cc.close();

                    AdapterList fc = new AdapterList(id_client,
                            client_name, title, created, null, null);
                    client_mas.add(fc);

                } while (c.moveToNext());
            }
        }
        c.close();

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
