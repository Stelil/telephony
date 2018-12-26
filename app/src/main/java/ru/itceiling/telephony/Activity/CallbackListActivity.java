package ru.itceiling.telephony.Activity;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;

import com.amigold.fundapter.BindDictionary;
import com.amigold.fundapter.FunDapter;
import com.amigold.fundapter.extractors.StringExtractor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import ru.itceiling.telephony.AdapterList;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

public class CallbackListActivity extends AppCompatActivity {

    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id, callbackDate;
    ArrayList<AdapterList> client_mas = new ArrayList<>();
    TextView txtSelectDay;

    String TAG = "logd";

    Calendar dateAndTime = new GregorianCalendar();

    int ii = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_callback_list);

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        SharedPreferences SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setTitle("Перезвоны");

        txtSelectDay = findViewById(R.id.txtSelectDay);
        txtSelectDay.setText(HelperClass.now_date().substring(0, 10));

        MyTask mt = new MyTask();
        mt.execute();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ii > 0) {
            MyTaskResume mt = new MyTaskResume();
            mt.execute();
        }
        ii++;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    class MyTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog mProgressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(CallbackListActivity.this);
            mProgressDialog.setMessage("Загрузка...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            listClients(HelperClass.now_date().substring(0, 10));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mProgressDialog.dismiss();
        }
    }

    class MyTaskResume extends AsyncTask<Void, Void, Void> {
        ProgressDialog mProgressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(CallbackListActivity.this);
            mProgressDialog.setMessage("Загрузка...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            listClients(txtSelectDay.getText().toString());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mProgressDialog.dismiss();
        }
    }

    private void listClients(String date) {

        final ListView listView = findViewById(R.id.list_client);
        client_mas.clear();

        String sqlQuewy;
        Cursor c;
        if (date.equals("")) {
            sqlQuewy = "SELECT client_id, date_time, comment, _id "
                    + "FROM rgzbn_gm_ceiling_callback " +
                    " order by date_time desc";
            c = db.rawQuery(sqlQuewy, new String[]{});
        } else {
            sqlQuewy = "SELECT client_id, date_time, comment, _id "
                    + "FROM rgzbn_gm_ceiling_callback " +
                    "where substr(date_time,1,10) <= ? " +
                    " order by date_time desc";
            c = db.rawQuery(sqlQuewy, new String[]{date});
        }
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String client_id = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String date_time = c.getString(c.getColumnIndex(c.getColumnName(1)));

                    String comment = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    if (comment.isEmpty())
                        comment = "-";

                    String client_name = "";
                    sqlQuewy = "SELECT client_name "
                            + "FROM rgzbn_gm_ceiling_clients" +
                            " WHERE _id = ? ";
                    Cursor cc = db.rawQuery(sqlQuewy, new String[]{client_id});
                    if (cc != null) {
                        if (cc.moveToFirst()) {
                            do {
                                client_name = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                            } while (cc.moveToNext());
                        }
                    }
                    cc.close();

                    String id = c.getString(c.getColumnIndex(c.getColumnName(3)));

                    if(date_time.length() == 19){
                        date_time = date_time.substring(0,16);
                    }

                    AdapterList fc = new AdapterList(client_id,
                            client_name, date_time, comment, id, null);
                    client_mas.add(fc);

                } while (c.moveToNext());
            }
        }
        c.close();

        BindDictionary<AdapterList> dict = new BindDictionary<>();

        dict.addStringField(R.id.firstColumn, new StringExtractor<AdapterList>() {
            @Override
            public String getStringValue(AdapterList nc, int position) {
                return nc.getOne();
            }
        });
        dict.addStringField(R.id.secondColumn, new StringExtractor<AdapterList>() {
            @Override
            public String getStringValue(AdapterList nc, int position) {
                return nc.getTwo();
            }
        });
        dict.addStringField(R.id.thirdColumn, new StringExtractor<AdapterList>() {
            @Override
            public String getStringValue(AdapterList nc, int position) {
                return nc.getThree();
            }
        });

        final FunDapter adapter = new FunDapter(this, client_mas, R.layout.layout_dialog_list, dict);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listView.setAdapter(adapter);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                AdapterList selectedid = client_mas.get(position);
                String id_client = selectedid.getId();

                Intent intent = new Intent(CallbackListActivity.this, ClientActivity.class);
                intent.putExtra("id_client", id_client);
                intent.putExtra("check", "true");
                startActivity(intent);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                // TODO Auto-generated method stub

                AdapterList selectedid = client_mas.get(pos);
                final int cId = Integer.parseInt(selectedid.getFour());

                AlertDialog.Builder ad = new AlertDialog.Builder(CallbackListActivity.this);
                ad.setMessage("Удалить звонок ?"); // сообщение
                ad.setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {

                        db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK,
                                "_id = ?",
                                new String[]{String.valueOf(cId)});

                        HelperClass.addExportData(
                                CallbackListActivity.this,
                                cId,
                                "rgzbn_gm_ceiling_callback",
                                "delete");

                        listClients(txtSelectDay.getText().toString());

                    }
                });
                ad.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {

                    }
                });
                ad.setCancelable(true);
                ad.show();
                return true;
            }
        });
    }

    public void onButtonSelectDay(View view) {
        setDate(txtSelectDay);
    }

    public void onButtonClearDay(View view) {
        listClients("");
        txtSelectDay.setText("");
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
                        callbackDate = "";
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
                        callbackDate = editTextDateParam;
                        listClients(editTextDateParam);
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
        callbackDate += " " + DateUtils.formatDateTime(this,
                dateAndTime.getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME);

    }
}