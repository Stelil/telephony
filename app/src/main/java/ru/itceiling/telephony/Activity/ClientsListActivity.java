package ru.itceiling.telephony.Activity;

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
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.amigold.fundapter.BindDictionary;
import com.amigold.fundapter.FunDapter;
import com.amigold.fundapter.extractors.StringExtractor;

import java.util.ArrayList;

import ru.itceiling.telephony.AdapterList;
import ru.itceiling.telephony.Broadcaster.ExportDataReceiver;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

public class ClientsListActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id;
    ArrayList<AdapterList> client_mas = new ArrayList<>();

    String TAG = "logd";

    String getPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clients_list);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setTitle("Клиенты");

        SharedPreferences SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        if (getIntent().getStringExtra("phone") == null) {
        } else {
            getPhone = getIntent().getStringExtra("phone");
            Log.d(TAG, "phone: " + getPhone);
        }

        if (!getPhone.equals("")) {
            View view = null;
            onButtonAddClient(view);
        }

        ExportDataReceiver exportDataReceiver = new ExportDataReceiver();
        Intent intent = new Intent(this, ExportDataReceiver.class);
        exportDataReceiver.onReceive(this, intent);

    }

    @Override
    protected void onResume() {
        super.onResume();

        MyTask mt = new MyTask();
        mt.execute();

        ExportDataReceiver exportDataReceiver = new ExportDataReceiver();
        if (exportDataReceiver != null) {
            exportDataReceiver.SetAlarm(this);
        }

    }

    class MyTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog mProgressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(ClientsListActivity.this);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        ListClients(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        ListClients(newText);
        return false;
    }

    public void onButtonAddClient(View view) {

        final Context context = this;
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
                                    ClientsListActivity.this, dealer_id);
                            String nowDate = HelperClass.now_date();
                            ContentValues values = new ContentValues();
                            values.put(DBHelper.KEY_ID, maxIdClient);
                            values.put(DBHelper.KEY_CLIENT_NAME, name);
                            values.put(DBHelper.KEY_TYPE_ID, "1");
                            values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                            values.put(DBHelper.KEY_MANAGER_ID, dealer_id);
                            values.put(DBHelper.KEY_CREATED, nowDate);
                            values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                            values.put(DBHelper.KEY_API_PHONE_ID, "null");
                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, null, values);

                            HelperClass.addExportData(
                                    context,
                                    maxIdClient,
                                    "rgzbn_gm_ceiling_clients",
                                    "send");

                            HelperClass.addHistory("Новый клиент", ClientsListActivity.this, String.valueOf(maxIdClient));

                            int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_statuses_map",
                                    ClientsListActivity.this, dealer_id);
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
                                        ClientsListActivity.this, dealer_id);
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

                            ListClients("");
                            dialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Клиент добавлен", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Введите имя", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        dialog.show();

    }

    private void ListClients(String query) {

        final ListView listView = findViewById(R.id.list_client);
        client_mas.clear();

        String sqlQuewy;
        Cursor c;

        String associated_client = HelperClass.associated_client(this, dealer_id);

        if (!query.equals("")) {
            sqlQuewy = "SELECT created, " +
                    "          client_name," +
                    "          _id " +
                    "     FROM rgzbn_gm_ceiling_clients" +
                    "    WHERE dealer_id = ? and " +
                    "          _id <> ? and " +
                    "         client_name like '%" + query + "%'" +
                    " order by created desc";
            c = db.rawQuery(sqlQuewy, new String[]{dealer_id, associated_client});
        } else {

            sqlQuewy = "SELECT created, " +
                    "          client_name, " +
                    "          _id " +
                    "     FROM rgzbn_gm_ceiling_clients" +
                    "    WHERE dealer_id = ? and " +
                    "         _id <> ? " +
                    " order by created desc";
            c = db.rawQuery(sqlQuewy, new String[]{dealer_id, associated_client});
        }
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String created = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    String id_client = c.getString(c.getColumnIndex(c.getColumnName(2)));
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

                Intent intent = new Intent(ClientsListActivity.this, ClientActivity.class);
                intent.putExtra("id_client", id_client);
                intent.putExtra("check", "false");
                startActivity(intent);
            }
        });

        //listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
        //    @Override
        //    public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
        //                                   int pos, long id) {
        //        // TODO Auto-generated method stub

        //        AdapterList selectedid = client_mas.get(pos);
        //        final String cId = selectedid.getId();

        //        AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
        //        ad.setMessage("Удалить перезвон " + cId + " ?"); // сообщение
        //        ad.setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
        //            public void onClick(DialogInterface dialog, int arg1) {

        //                db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, "_id = ?",
        //                        new String[]{cId});

        //                db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, "_id = ?",
        //                        new String[]{cId});

        //                values = new ContentValues();
        //                values.put(DBHelper.KEY_ID_OLD, cId);
        //                values.put(DBHelper.KEY_ID_NEW, "0");
        //                values.put(DBHelper.KEY_NAME_TABLE, "rgzbn_gm_ceiling_projects");
        //                values.put(DBHelper.KEY_SYNC, "0");
        //                values.put(DBHelper.KEY_TYPE, "send");
        //                values.put(DBHelper.KEY_STATUS, "1");
        //                db.insert(DBHelper.HISTORY_SEND_TO_SERVER, null, values);

        //                onResume();
        //            }
        //        });
        //        ad.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
        //            public void onClick(DialogInterface dialog, int arg1) {

        //            }
        //        });
        //        ad.setCancelable(true);
        //        ad.show();
        //        return true;
        //    }
        //});
    }

}









