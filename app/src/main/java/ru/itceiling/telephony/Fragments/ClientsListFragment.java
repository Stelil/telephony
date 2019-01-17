package ru.itceiling.telephony.Fragments;


import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ru.itceiling.telephony.Activity.ClientActivity;
import ru.itceiling.telephony.Adapter.RVAdapterClient;
import ru.itceiling.telephony.Adapter.RecyclerViewClickListener;
import ru.itceiling.telephony.AdapterList;
import ru.itceiling.telephony.Broadcaster.ExportDataReceiver;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.Person;
import ru.itceiling.telephony.R;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class ClientsListFragment extends Fragment implements RecyclerViewClickListener {
    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id, user_id;
    ArrayList<AdapterList> client_mas = new ArrayList<>();

    String TAG = "logd";

    String getPhone = "", textSearch = "";

    private View view;

    List<Person> persons;
    RecyclerView recyclerView;
    RVAdapterClient adapter;

    public ClientsListFragment() {
        // Required empty public constructor
    }

    public static ClientsListFragment newInstance() {
        return new ClientsListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_clients_list, container, false);

        SharedPreferences SP = getActivity().getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        SP = getActivity().getSharedPreferences("user_id", MODE_PRIVATE);
        user_id = SP.getString("", "");

        dbHelper = new DBHelper(getActivity());
        db = dbHelper.getWritableDatabase();

        if (getActivity().getIntent().getStringExtra("phone") == null) {
        } else {
            getPhone = getActivity().getIntent().getStringExtra("phone");

            long notifyID = getActivity().getIntent().getLongExtra("notifyID", 0);

            NotificationManager notificationManager =
                    (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel((int) notifyID);
        }

        Button addClient = view.findViewById(R.id.AddClient);
        addClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonAddClient(view);
            }
        });

        if (!getPhone.equals("")) {
            View view = null;
            onButtonAddClient(view);
        }

        recyclerView = view.findViewById(R.id.recyclerViewClients);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(llm);
        recyclerView.setHasFixedSize(true);

        getActivity().setTitle("Клиенты");

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        MyTask mt = new MyTask();
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
            ListClients("");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mProgressDialog.dismiss();
        }
    }

    public void onButtonAddClient(View view) {

        final Context context = getActivity();
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.dialog_add_client, null);
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
        mDialogBuilder.setView(promptsView);
        final EditText nameClient = (EditText) promptsView.findViewById(R.id.nameClient);
        final EditText phoneClient = (EditText) promptsView.findViewById(R.id.phoneClient);

        if (!getPhone.equals("")) {
            phoneClient.setText(getPhone);
        }

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(promptsView)
                .setTitle("Добавление нового клиента")
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO Do something

                        String name = nameClient.getText().toString();
                        String phone = phoneClient.getText().toString();

                        if (name.length() > 0) {
                            int maxIdClient = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients",
                                    getActivity(), user_id);
                            String nowDate = HelperClass.now_date();
                            ContentValues values = new ContentValues();
                            values.put(DBHelper.KEY_ID, maxIdClient);
                            values.put(DBHelper.KEY_CLIENT_NAME, name);
                            values.put(DBHelper.KEY_TYPE_ID, "1");
                            values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                            values.put(DBHelper.KEY_MANAGER_ID, user_id);
                            values.put(DBHelper.KEY_CREATED, nowDate);
                            values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                            values.put(DBHelper.KEY_API_PHONE_ID, "null");
                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, null, values);

                            HelperClass.addExportData(
                                    context,
                                    maxIdClient,
                                    "rgzbn_gm_ceiling_clients",
                                    "send");

                            HelperClass.addHistory("Новый клиент", getActivity(), String.valueOf(maxIdClient));

                            int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_statuses_map",
                                    getActivity(), user_id);
                            values = new ContentValues();
                            values.put(DBHelper.KEY_ID, maxId);
                            values.put(DBHelper.KEY_CLIENT_ID, maxIdClient);
                            values.put(DBHelper.KEY_STATUS_ID, "1");
                            values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, null, values);

                            HelperClass.addExportData(
                                    context,
                                    maxId,
                                    "rgzbn_gm_ceiling_clients_statuses_map",
                                    "send");

                            if ((phone.length() == 11)) {
                                int maxIdContacts = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_contacts",
                                        getActivity(), user_id);
                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, maxIdContacts);
                                values.put(DBHelper.KEY_CLIENT_ID, maxIdClient);
                                values.put(DBHelper.KEY_PHONE, HelperClass.phone_edit(phone));
                                values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                                db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, null, values);

                                HelperClass.addExportData(
                                        context,
                                        maxIdContacts,
                                        "rgzbn_gm_ceiling_clients_contacts",
                                        "send");
                            }

                            String nameManager = "-";
                            String sqlQuewy = "SELECT name "
                                    + "   FROM rgzbn_users" +
                                    "    WHERE _id = ? " +
                                    "order by _id";
                            Cursor cc = db.rawQuery(sqlQuewy, new String[]{user_id});
                            if (cc != null) {
                                if (cc.moveToLast()) {
                                    nameManager = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                                }
                            }
                            cc.close();

                            persons.add(0, new Person(name, phone, nameManager, "#000000",
                                    "Холодный", "Необработанный", Integer.valueOf(maxIdClient)));
                            adapter.notifyItemInserted(0);
                            ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(0, 0);

                            dialog.dismiss();
                            Toast.makeText(getActivity().getApplicationContext(), "Клиент добавлен", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity().getApplicationContext(), "Введите имя", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        dialog.show();

    }

    private void ListClients(String query) {

        client_mas.clear();

        persons = new ArrayList<>();

        String sqlQuewy;
        Cursor c;

        String associated_client = HelperClass.associated_client(getActivity(), dealer_id);
        sqlQuewy = "SELECT created, " +
                "          client_name, " +
                "          _id," +
                "          manager_id " +
                "     FROM rgzbn_gm_ceiling_clients" +
                "    WHERE dealer_id = ? and " +
                "         _id <> ? " +
                " order by created desc";
        c = db.rawQuery(sqlQuewy, new String[]{dealer_id, associated_client});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    String id_client = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    String manager_id = c.getString(c.getColumnIndex(c.getColumnName(3)));
                    String title = "-";

                    String client_status = null;
                    sqlQuewy = "SELECT status_id, change_time "
                            + "   FROM rgzbn_gm_ceiling_clients_statuses_map" +
                            "    WHERE client_id = ? " +
                            "order by _id";
                    Cursor cc = db.rawQuery(sqlQuewy, new String[]{id_client});
                    if (cc != null) {
                        if (cc.moveToLast()) {
                            client_status = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                        }
                    }
                    cc.close();

                    try {
                        sqlQuewy = "SELECT title "
                                + "FROM rgzbn_gm_ceiling_clients_statuses" +
                                " WHERE _id = ? ";
                        cc = db.rawQuery(sqlQuewy, new String[]{client_status});
                        if (cc != null) {
                            if (cc.moveToFirst()) {
                                title = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                            }
                        }
                        cc.close();
                    } catch (Exception e) {
                    }

                    String phone = "-";
                    sqlQuewy = "SELECT phone "
                            + "   FROM rgzbn_gm_ceiling_clients_contacts" +
                            "    WHERE client_id = ?";
                    cc = db.rawQuery(sqlQuewy, new String[]{id_client});
                    if (cc != null) {
                        if (cc.moveToLast()) {
                            phone = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                        }
                    }
                    cc.close();

                    String nameManager = "-";
                    sqlQuewy = "SELECT name "
                            + "   FROM rgzbn_users" +
                            "    WHERE _id = ? " +
                            "order by _id";
                    cc = db.rawQuery(sqlQuewy, new String[]{manager_id});
                    if (cc != null) {
                        if (cc.moveToLast()) {
                            nameManager = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                        }
                    }
                    cc.close();

                    persons.add(new Person(client_name, phone, nameManager, "#000000",
                            "Холодный", title, Integer.valueOf(id_client)));
                } while (c.moveToNext());
            }
        }
        c.close();

        adapter = new RVAdapterClient(persons, this);
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
        intent.putExtra("check", "false");
        startActivity(intent);
    }

    @Override
    public void recyclerViewListLongClicked(View v, int id, int pos) {

    }

}