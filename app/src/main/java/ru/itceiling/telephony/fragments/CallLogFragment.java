package ru.itceiling.telephony.fragments;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.activity.ClientActivity;
import ru.itceiling.telephony.activity.MainActivity;
import ru.itceiling.telephony.adapter.RVAdapterCallLog;
import ru.itceiling.telephony.adapter.RecyclerViewClickListener;
import ru.itceiling.telephony.CallLog;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.R;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class CallLogFragment extends Fragment implements RecyclerViewClickListener {

    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id, user_id;

    String TAG = "logd";
    List<CallLog> callLogs;
    RecyclerView recyclerView;
    RVAdapterCallLog adapter;

    View view;

    public CallLogFragment() {
        // Required empty public constructor
    }

    public static CallLogFragment newInstance() {
        return new CallLogFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_call_log, container, false);
        getActivity().setTitle("Журнал звонков");

        dbHelper = new DBHelper(getActivity());
        db = dbHelper.getWritableDatabase();

        SharedPreferences SP = getActivity().getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        SP = getActivity().getSharedPreferences("user_id", MODE_PRIVATE);
        user_id = SP.getString("", "");

        recyclerView = view.findViewById(R.id.recyclerViewCallLog);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(llm);
        recyclerView.setHasFixedSize(true);

        listCallLog();

        return view;
    }

    private void listCallLog() {
        callLogs = new ArrayList<>();

        String sqlQuewy = "SELECT sh.client_id, " +
                "cl.client_name, " +
                "cc.phone, " +
                "sh.change_time, " +
                "sh.status, " +
                "cs.title, " +
                "sh.call_length " +
                "FROM rgzbn_gm_ceiling_calls_status_history AS sh " +
                "LEFT JOIN rgzbn_gm_ceiling_clients AS cl " +
                "ON sh.client_id = cl._id " +
                "LEFT JOIN rgzbn_gm_ceiling_clients_contacts AS cc " +
                "ON sh.client_id = cc.client_id " +
                "LEFT JOIN rgzbn_gm_ceiling_calls_status AS cs  " +
                "ON sh.status = cs._id " +
                "WHERE sh.manager_id = ? " +
                "GROUP BY sh.change_time " +
                "ORDER BY sh.change_time DESC";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{user_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    Integer client_id = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                    String client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    String phone = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    String date_time = c.getString(c.getColumnIndex(c.getColumnName(3)));
                    Integer status = c.getInt(c.getColumnIndex(c.getColumnName(4)));
                    String title = c.getString(c.getColumnIndex(c.getColumnName(5)));
                    String call_length = c.getString(c.getColumnIndex(c.getColumnName(6)));

                    try {
                        if (!call_length.equals("0") && !call_length.equals("null")) {
                            title = title + "(" + HelperClass.editTimeCall(call_length) + ")";
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "listCallLog: " + e);
                    }

                    callLogs.add(new CallLog(client_id,
                            client_name,
                            phone,
                            date_time.substring(0, date_time.length() - 3),
                            title,
                            status
                    ));

                } while (c.moveToNext());
            }
        }
        c.close();

        adapter = new RVAdapterCallLog(callLogs, this);
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recyclerView.setAdapter(adapter);
                }
            });
        }
    }

    @Override
    public void recyclerViewListClicked(View v, final int pos) {

        final int id = callLogs.get(pos).getId();

        int deleted_by_user = 0;
        String sqlQuewy = "SELECT deleted_by_user "
                + "FROM rgzbn_gm_ceiling_clients " +
                "WHERE _id = ?";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id)});
        if (c != null) {
            if (c.moveToLast()) {
                deleted_by_user = c.getInt(c.getColumnIndex(c.getColumnName(0)));
            }
        }
        c.close();

        String phone = "";
        sqlQuewy = "SELECT phone "
                + "FROM rgzbn_gm_ceiling_clients_contacts " +
                "WHERE client_id = ?";
        c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id)});
        if (c != null) {
            if (c.moveToLast()) {
                phone = c.getString(c.getColumnIndex(c.getColumnName(0)));
            }
        }
        c.close();

        if (deleted_by_user == 0) {
            String[] array = new String[]{"Открыть", "Позвонить"};
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Выберите действие")
                    .setNegativeButton("Отмена",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            final String finalPhone = phone;
            builder.setItems(array, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    // TODO Auto-generated method stub
                    switch (item) {
                        case 0:
                            Intent intent = new Intent(getActivity(), ClientActivity.class);
                            intent.putExtra("id_client", String.valueOf(id));
                            intent.putExtra("check", "true");
                            startActivity(intent);

                            break;
                        case 1:
                            intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse("tel:+" + finalPhone));
                            startActivity(intent);
                            break;
                    }
                }
            });

            builder.setCancelable(false);
            builder.create();
            builder.show();


        } else {
            String[] array = new String[]{"Добавить", "Привязка номера", "Удалить"};
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Выберите действие")
                    .setNegativeButton("Отмена",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            final String finalPhone = phone;
            builder.setItems(array, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    // TODO Auto-generated method stub
                    switch (item) {
                        case 0:
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            intent.putExtra("phone", finalPhone);
                            intent.putExtra("add", "0");
                            startActivity(intent);
                            getActivity().finish();
                            break;
                        case 1:
                            intent = new Intent(getActivity(), MainActivity.class);
                            intent.putExtra("phone", finalPhone);
                            intent.putExtra("add", "1");
                            startActivity(intent);
                            getActivity().finish();
                            break;
                        case 2:
                            db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY,
                                    "client_id = ?",
                                    new String[]{String.valueOf(id)});

                            db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS,
                                    "_id = ?",
                                    new String[]{String.valueOf(id)});

                            db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS,
                                    "client_id = ?",
                                    new String[]{String.valueOf(id)});

                            db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY,
                                    "client_id = ?",
                                    new String[]{String.valueOf(id)});

                            callLogs.remove(pos);
                            adapter.notifyItemRemoved(pos);
                            break;
                    }
                }
            });

            builder.setCancelable(false);
            builder.create();
            builder.show();
        }

    }

    @Override
    public void recyclerViewListLongClicked(View v, int id, int pos) {

    }
}