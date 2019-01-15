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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amigold.fundapter.BindDictionary;
import com.amigold.fundapter.FunDapter;
import com.amigold.fundapter.extractors.StringExtractor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import ru.itceiling.telephony.Activity.CallbackListActivity;
import ru.itceiling.telephony.Activity.ClientActivity;
import ru.itceiling.telephony.Adapter.RVAdapterCallback;
import ru.itceiling.telephony.Adapter.RecyclerViewClickListener;
import ru.itceiling.telephony.AdapterList;
import ru.itceiling.telephony.Broadcaster.ExportDataReceiver;
import ru.itceiling.telephony.Callback;
import ru.itceiling.telephony.Comparators.ComparatorComment;
import ru.itceiling.telephony.Comparators.ComparatorDate;
import ru.itceiling.telephony.Comparators.ComparatorFio;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class CallbackListFragment extends Fragment implements RecyclerViewClickListener {

    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id, callbackDate, user_id;
    ArrayList<AdapterList> client_mas = new ArrayList<>();
    TextView txtSelectDay;

    String TAG = "logd";

    Calendar dateAndTime = new GregorianCalendar();

    int ii = 0;

    List<Callback> callbacks;
    RecyclerView recyclerView;
    RVAdapterCallback adapter;

    View view;

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

        dbHelper = new DBHelper(getActivity());
        db = dbHelper.getWritableDatabase();

        SharedPreferences SP = getActivity().getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        SP = getActivity().getSharedPreferences("user_id", MODE_PRIVATE);
        user_id = SP.getString("", "");

        txtSelectDay = view.findViewById(R.id.txtSelectDay);
        txtSelectDay.setText(HelperClass.now_date().substring(0, 10));

        MyTask mt = new MyTask();
        mt.execute();

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
                listClients("");
                txtSelectDay.setText("");
            }
        });


        recyclerView = view.findViewById(R.id.recyclerViewCallback);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(llm);
        recyclerView.setHasFixedSize(true);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ii > 0) {
        }
        ii++;

        MyTaskResume mt = new MyTaskResume();
        mt.execute();

        ExportDataReceiver exportDataReceiver = new ExportDataReceiver();
        Intent intent = new Intent(getActivity(), ExportDataReceiver.class);
        exportDataReceiver.onReceive(getActivity(), intent);
    }

    class MyTask extends AsyncTask<Void, Void, Void> {
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
            mProgressDialog = new ProgressDialog(getActivity());
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

        client_mas.clear();

        callbacks = new ArrayList<>();

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

                    if (date_time.length() == 19) {
                        date_time = date_time.substring(0, 16);
                    }

                    String phone = "-";
                    sqlQuewy = "SELECT phone "
                            + "   FROM rgzbn_gm_ceiling_clients_contacts" +
                            "    WHERE client_id = ?";
                    cc = db.rawQuery(sqlQuewy, new String[]{client_id});
                    if (cc != null) {
                        if (cc.moveToLast()) {
                            phone = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                        }
                    }
                    cc.close();

                    callbacks.add(new Callback(client_name,
                            phone,
                            comment,
                            date_time,
                            Integer.valueOf(client_id),
                            Integer.valueOf(id)));

                } while (c.moveToNext());
            }
        }
        c.close();

        adapter = new RVAdapterCallback(callbacks, this);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setAdapter(adapter);
            }
        });

    }

    @Override
    public void recyclerViewListClicked(View v, int id) {
        Intent intent = new Intent(getActivity(), ClientActivity.class);
        intent.putExtra("id_client", " " + id);
        intent.putExtra("check", "true");
        startActivity(intent);
    }

    @Override
    public void recyclerViewListLongClicked(View v, final int idCall, final int pos) {
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

    /*

    void createList(){

        final ListView listView = view.findViewById(R.id.list_client);

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

        final FunDapter adapter = new FunDapter(getActivity(), client_mas, R.layout.layout_dialog_list, dict);

        getActivity().runOnUiThread(new Runnable() {
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

                Intent intent = new Intent(getActivity(), ClientActivity.class);
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

                AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
                ad.setMessage("Удалить звонок ?"); // сообщение
                ad.setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {

                        db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK,
                                "_id = ?",
                                new String[]{String.valueOf(cId)});

                        HelperClass.addExportData(
                                getActivity(),
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

    */

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
                DateUtils.formatDateTime(getActivity(),
                        dateAndTime.getTimeInMillis(),
                        DateUtils.FORMAT_SHOW_TIME));
        callbackDate += " " + DateUtils.formatDateTime(getActivity(),
                dateAndTime.getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME);

    }

}
