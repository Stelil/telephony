package ru.itceiling.telephony.Activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import java.util.List;

import ru.itceiling.telephony.AdapterList;
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

    }

    @Override
    protected void onResume() {
        super.onResume();
        ListClients("");

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

                            int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients",
                                    ClientsListActivity.this, dealer_id);
                            String nowDate = HelperClass.now_date();
                            ContentValues values = new ContentValues();
                            values.put(DBHelper.KEY_ID, maxId);
                            values.put(DBHelper.KEY_CLIENT_NAME, name);
                            values.put(DBHelper.KEY_TYPE_ID, "1");
                            values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                            values.put(DBHelper.KEY_MANAGER_ID, dealer_id);
                            values.put(DBHelper.KEY_CREATED, nowDate);
                            values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                            values.put(DBHelper.KEY_CLIENT_STATUS, "1");
                            values.put(DBHelper.KEY_API_PHONE_ID, "null");
                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, null, values);

                            HelperClass.addHistory("Новый клиент", ClientsListActivity.this, String.valueOf(maxId));

                            if ((phone.length() == 11)) {
                                int maxIdContacts = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_contacts",
                                        ClientsListActivity.this, dealer_id);
                                nowDate = HelperClass.now_date();
                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, maxIdContacts);
                                values.put(DBHelper.KEY_CLIENT_ID, maxId);
                                values.put(DBHelper.KEY_PHONE, HelperClass.phone_edit(phone));
                                values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                                db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, null, values);

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


        ListView listView = findViewById(R.id.list_client);
        client_mas.clear();

        String sqlQuewy;
        Cursor c;

        if (!query.equals("")) {
            sqlQuewy = "SELECT change_time, client_name, client_status, _id "
                    + "FROM rgzbn_gm_ceiling_clients" +
                    " WHERE dealer_id = ? and client_name like '%"+query+"%'";
            c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
        } else {

            sqlQuewy = "SELECT change_time, client_name, client_status, _id "
                    + "FROM rgzbn_gm_ceiling_clients" +
                    " WHERE dealer_id = ?";
            c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
        }

        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String change_time = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    String client_status = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    String id_client = c.getString(c.getColumnIndex(c.getColumnName(3)));
                    String title = null;

                    sqlQuewy = "SELECT title "
                            + "FROM rgzbn_gm_ceiling_client_statuses" +
                            " WHERE _id = ? ";
                    Cursor cc = db.rawQuery(sqlQuewy, new String[]{client_status});
                    if (cc != null) {
                        if (cc.moveToFirst()) {
                            do {
                                title = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                            } while (cc.moveToNext());
                        }
                    }
                    cc.close();

                    AdapterList fc = new AdapterList(id_client,
                            client_name, title, change_time, null, null);
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

                Intent intent = new Intent(ClientsListActivity.this, ClientActivity.class);
                intent.putExtra("id_client", id_client);
                startActivity(intent);
            }
        });
    }
}