package ru.itceiling.telephony.Activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

public class ManagerActivity extends AppCompatActivity {

    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id;
    String TAG = "logd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        SharedPreferences SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

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

                            ContentValues values = new ContentValues();
                            values.put(DBHelper.KEY_ID, maxIdClient);
                            values.put(DBHelper.KEY_NAME, name);
                            values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                            values.put(DBHelper.KEY_USERNAME, phone);
                            values.put(DBHelper.KEY_EMAIL, emailClient.getText().toString());
                            values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.now_date());
                            db.insert(DBHelper.TABLE_USERS, null, values);

                            org.json.simple.JSONObject jsonObjectClient = new org.json.simple.JSONObject();
                            jsonObjectClient.put("email", emailClient.getText().toString());
                            jsonObjectClient.put("fio", name);
                            jsonObjectClient.put("username", phone);
                            jsonObjectClient.put("group", "13");
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
}