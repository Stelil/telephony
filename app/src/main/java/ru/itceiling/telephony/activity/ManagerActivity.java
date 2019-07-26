package ru.itceiling.telephony.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amigold.fundapter.BindDictionary;
import com.amigold.fundapter.FunDapter;
import com.amigold.fundapter.extractors.StringExtractor;

import java.util.ArrayList;

import ru.itceiling.telephony.data.AdapterList;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

public class ManagerActivity extends AppCompatActivity {

    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id;
    String TAG = "logd";
    ArrayList<AdapterList> client_mas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        SharedPreferences SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        createTable();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void buttonRegisterManager(View view) {

        final Context context = this;
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.dialog_add_client, null);
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
        mDialogBuilder.setView(promptsView);
        final EditText nameClient = (EditText) promptsView.findViewById(R.id.nameClient);
        final EditText phoneClient = (EditText) promptsView.findViewById(R.id.phoneClient);

        final TextView textEmailClient = (TextView) promptsView.findViewById(R.id.textEmailClient);
        final EditText emailClient = (EditText) promptsView.findViewById(R.id.emailClient);

        textEmailClient.setVisibility(View.VISIBLE);
        emailClient.setVisibility(View.VISIBLE);

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(promptsView)
                .setTitle("Добавить")
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

                        if (name.length() > 0 && phone.length() == 11 &&
                                HelperClass.validateMail(emailClient.getText().toString())) {
                            int maxIdClient = HelperClass.lastIdTable("rgzbn_users",
                                    ManagerActivity.this, dealer_id);

                            String settings = "";
                            String sqlQuewy = "SELECT settings " +
                                    "     FROM rgzbn_users" +
                                    "    WHERE dealer_id = ? ";
                            Cursor c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    settings = c.getString(c.getColumnIndex(c.getColumnName(0)));
                                }
                            }
                            c.close();

                            ContentValues values = new ContentValues();
                            values.put(DBHelper.KEY_ID, maxIdClient);
                            values.put(DBHelper.KEY_NAME, name);
                            values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                            values.put(DBHelper.KEY_USERNAME, phone);
                            values.put(DBHelper.KEY_EMAIL, emailClient.getText().toString());
                            values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
                            db.insert(DBHelper.TABLE_USERS, null, values);

                            org.json.simple.JSONObject jsonObjectClient = new org.json.simple.JSONObject();
                            jsonObjectClient.put("email", emailClient.getText().toString());
                            jsonObjectClient.put("fio", name);
                            jsonObjectClient.put("username", phone);
                            jsonObjectClient.put("group", "13");
                            jsonObjectClient.put("dealer_id", dealer_id);
                            String data = String.valueOf(jsonObjectClient);

                            int maxId = HelperClass.lastIdTable("rgzbn_user_usergroup_map",
                                    ManagerActivity.this, dealer_id);

                            dialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Менеджер добавлен", Toast.LENGTH_LONG).show();

                            HelperClass.addExportData(
                                    context,
                                    0,
                                    "rgzbn_users_manager",
                                    "send",
                                    data);
                        } else {
                            Toast.makeText(getApplicationContext(), "Проверьте введенные данные", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    void createTable() {

        client_mas.clear();

        String sqlQuewy = "SELECT _id, name, username, email " +
                "     FROM rgzbn_users" +
                "    WHERE dealer_id = ? " +
                " order by registerDate desc";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String id = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    String username = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    String email = c.getString(c.getColumnIndex(c.getColumnName(3)));

                    AdapterList fc = new AdapterList(id,
                            name, username, email, null, null);
                    client_mas.add(fc);

                } while (c.moveToNext());
            }
        }
        c.close();

        final ListView listView = findViewById(R.id.listManager);
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
        listView.setAdapter(adapter);

        /*
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
        */

    }
}