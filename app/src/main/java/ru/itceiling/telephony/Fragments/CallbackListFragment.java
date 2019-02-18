package ru.itceiling.telephony.Fragments;


import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import ru.itceiling.telephony.Activity.ClientActivity;
import ru.itceiling.telephony.Adapter.RVAdapterCallback;
import ru.itceiling.telephony.Adapter.RecyclerViewClickListener;
import ru.itceiling.telephony.Broadcaster.ExportDataReceiver;
import ru.itceiling.telephony.Broadcaster.ImportDataReceiver;
import ru.itceiling.telephony.Callback;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class CallbackListFragment extends Fragment implements RecyclerViewClickListener, SearchView.OnQueryTextListener {

    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id, callbackDate, user_id;
    TextView txtSelectDay;

    String TAG = "logd";

    Calendar dateAndTime = new GregorianCalendar();

    int ii = 0;

    List<Callback> callbacks;
    RecyclerView recyclerView;
    RVAdapterCallback adapter;

    View view;

    int itemSelected = 0;

    public CallbackListFragment() {
        // Required empty public constructor
    }

    public static CallbackListFragment newInstance() {
        return new CallbackListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_callback_list, container, false);
        getActivity().setTitle("Перезвоны");

        dbHelper = new DBHelper(getActivity());
        db = dbHelper.getWritableDatabase();

        SharedPreferences SP = getActivity().getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        SP = getActivity().getSharedPreferences("user_id", MODE_PRIVATE);
        user_id = SP.getString("", "");

        txtSelectDay = view.findViewById(R.id.txtSelectDay);
        txtSelectDay.setText(HelperClass.nowDate().substring(0, 10));

        final TextView txtSelectDay = view.findViewById(R.id.txtSelectDay);
        txtSelectDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDate(txtSelectDay);
            }
        });

        ImageButton btnClearDay = view.findViewById(R.id.btnClearDay);
        btnClearDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listClients("2100-01-01", "");
                txtSelectDay.setText("");
            }
        });

        recyclerView = view.findViewById(R.id.recyclerViewCallback);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(llm);
        recyclerView.setHasFixedSize(true);

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        listClients(HelperClass.nowDate().substring(0, 10), query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        listClients(HelperClass.nowDate().substring(0, 10), newText);
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        MyTask mt = new MyTask();
        mt.execute();

        ExportDataReceiver exportDataReceiver = new ExportDataReceiver();
        Intent intent = new Intent(getActivity(), ExportDataReceiver.class);
        exportDataReceiver.onReceive(getActivity(), intent);

        ImportDataReceiver importDataReceiver = new ImportDataReceiver();
        intent = new Intent(getActivity(), ImportDataReceiver.class);
        importDataReceiver.onReceive(getActivity(), intent);
    }

    class MyTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("Отображаем...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            listClients(txtSelectDay.getText().toString(), "");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mProgressDialog.dismiss();
        }
    }

    /*
    class MyTaskResume extends AsyncTask<Void, Void, Void> {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("Загрузка...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            listClients(txtSelectDay.getText().toString(), "");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mProgressDialog.dismiss();
        }
    }
    */

    private void listClients(String date, String query) {
        callbacks = new ArrayList<>();

        String sqlQuery;
        Cursor c;
        if (query.equals("")) {
            sqlQuery = "SELECT callback.client_id, callback.date_time, " +
                    "callback.comment, clients.client_name, " +
                    "users.name, clients_c.phone, " +
                    "callback._id " +
                    "FROM rgzbn_gm_ceiling_callback AS callback " +
                    "INNER JOIN rgzbn_gm_ceiling_clients AS clients " +
                    "ON clients._id = callback.client_id " +
                    "left JOIN rgzbn_gm_ceiling_clients_contacts AS clients_c " +
                    "ON clients_c.client_id = callback.client_id " +
                    "INNER JOIN rgzbn_users AS users " +
                    "ON users._id = callback.manager_id " +
                    "where substr(date_time,1,10) <= ? " +
                    "group by callback._id " +
                    "order by callback.date_time desc";
        } else {
            sqlQuery = "SELECT callback.client_id, callback.date_time, " +
                    "callback.comment, clients.client_name, " +
                    "users.name, clients_c.phone, " +
                    "callback._id " +
                    "FROM rgzbn_gm_ceiling_callback AS callback " +
                    "INNER JOIN rgzbn_gm_ceiling_clients AS clients " +
                    "ON clients._id = callback.client_id " +
                    "left JOIN rgzbn_gm_ceiling_clients_contacts AS clients_c " +
                    "ON clients_c.client_id = callback.client_id " +
                    "INNER JOIN rgzbn_users AS users " +
                    "ON users._id = callback.manager_id " +
                    "where substr(date_time,1,10) <= ? " +
                    "and clients.client_name like '%" + query + "%' " +
                    "or clients_c.phone like '%" + query + "%' " +
                    "or callback.comment like '%" + query + "%' " +
                    "group by callback._id " +
                    "order by callback.date_time desc";
        }
        c = db.rawQuery(sqlQuery, new String[]{date});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String client_id = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String date_time = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    date_time = date_time.substring(0,date_time.length()-3);
                    String comment = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    String client_name = c.getString(c.getColumnIndex(c.getColumnName(3)));
                    String nameManager = c.getString(c.getColumnIndex(c.getColumnName(4)));
                    String phone = c.getString(c.getColumnIndex(c.getColumnName(5)));
                    String id = c.getString(c.getColumnIndex(c.getColumnName(6)));

                    callbacks.add(new Callback(client_name,
                            phone,
                            comment,
                            date_time,
                            Integer.valueOf(client_id),
                            Integer.valueOf(id),
                            nameManager));
                } while (c.moveToNext());
            }
        }
        c.close();

        try {
            adapter = new RVAdapterCallback(callbacks, this);
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.setAdapter(adapter);
                        if (itemSelected == 0) {
                            recyclerView.scrollToPosition(itemSelected);
                        } else {
                            recyclerView.scrollToPosition(itemSelected - 1);
                        }
                    }
                });
            }
        } catch (Exception e) {
        }
        
    }

    @Override
    public void recyclerViewListClicked(View v, int pos) {
        itemSelected = pos;
        Intent intent = new Intent(getActivity(), ClientActivity.class);
        int clickedDataItem = callbacks.get(pos).getId();
        intent.putExtra("id_client", String.valueOf(clickedDataItem));
        intent.putExtra("check", "true");
        startActivity(intent);
    }

    @Override
    public void recyclerViewListLongClicked(View v, final int idCall, final int pos) {
        SharedPreferences SP = getActivity().getSharedPreferences("group_id", MODE_PRIVATE);
        if (SP.getString("", "").equals("13")) {
        } else {
            AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
            ad.setMessage("Удалить звонок ?"); // сообщение
            ad.setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {

                    callbacks.remove(pos);
                    adapter.notifyItemRemoved(pos);

                    db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK,
                            "_id = ?",
                            new String[]{String.valueOf(idCall)});

                    HelperClass.addExportData(
                            getActivity(),
                            idCall,
                            "rgzbn_gm_ceiling_callback",
                            "delete");

                }
            });
            ad.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {

                }
            });
            ad.setCancelable(true);
            ad.show();
        }
    }

    public void setDate(View v) {
        final Calendar cal = Calendar.getInstance();
        int mYear = cal.get(Calendar.YEAR);
        int mMonth = cal.get(Calendar.MONTH);
        int mDay = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
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
                        listClients(editTextDateParam, "");
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
                DateUtils.formatDateTime(getActivity(),
                        dateAndTime.getTimeInMillis(),
                        DateUtils.FORMAT_SHOW_TIME));
        callbackDate += " " + DateUtils.formatDateTime(getActivity(),
                dateAndTime.getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME);

    }

}
