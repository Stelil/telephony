package ru.itceiling.telephony.Fragments;


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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import ru.itceiling.telephony.Activity.ClientActivity;
import ru.itceiling.telephony.Activity.MainActivity;
import ru.itceiling.telephony.Adapter.RVAdapterCallLog;
import ru.itceiling.telephony.Adapter.RecyclerViewClickListener;
import ru.itceiling.telephony.CallLog;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
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

        String sqlQuewy = "SELECT _id, client_id, status, change_time, call_length "
                + "FROM rgzbn_gm_ceiling_calls_status_history " +
                "where manager_id = ? " +
                "order by change_time desc";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{user_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String id = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    Integer client_id = c.getInt(c.getColumnIndex(c.getColumnName(1)));
                    String status = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    String date_time = c.getString(c.getColumnIndex(c.getColumnName(3)));
                    String call_length = c.getString(c.getColumnIndex(c.getColumnName(4)));

                    String client_name = "";
                    sqlQuewy = "SELECT client_name "
                            + "FROM rgzbn_gm_ceiling_clients" +
                            " WHERE _id = ? ";
                    Cursor cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(client_id)});
                    if (cc != null) {
                        if (cc.moveToFirst()) {
                            do {
                                client_name = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                            } while (cc.moveToNext());
                        }
                    }
                    cc.close();

                    String client_phone = "";
                    sqlQuewy = "SELECT phone "
                            + "   FROM rgzbn_gm_ceiling_clients_contacts" +
                            "    WHERE client_id = ?";
                    cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(client_id)});
                    if (cc != null) {
                        if (cc.moveToLast()) {
                            client_phone = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                        }
                    }
                    cc.close();

                    String type = "";
                    sqlQuewy = "SELECT title "
                            + "FROM rgzbn_gm_ceiling_calls_status " +
                            "WHERE _id = ?";
                    cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(status)});
                    if (cc != null) {
                        if (cc.moveToLast()) {
                            /*if (status.equals("1") || status.equals("0")) {
                                type = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                            } else {
                                type = cc.getString(cc.getColumnIndex(cc.getColumnName(0))) +
                                        "\n(Длина: " + HelperClass.editTimeCall(call_length) + ")";
                            }*/
                            switch (cc.getString(cc.getColumnIndex(cc.getColumnName(0)))) {
                                case "Исходящий недозвон":
                                    type = "Недозвон";
                                    break;
                                case "Входящий звонок":
                                    type = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                                    break;
                                case "Исходящий дозвон":
                                    type = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                                    break;
                            }
                        }
                    }
                    cc.close();

                    callLogs.add(new CallLog(client_id,
                            client_name,
                            client_phone,
                            date_time.substring(0, date_time.length() - 3),
                            type
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
    public void run() {
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