package ru.itceiling.telephony.Activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ru.itceiling.telephony.Adapter.PhoneBookAdapter;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.PhoneBook;
import ru.itceiling.telephony.R;

public class PhoneBookActivity extends AppCompatActivity {

    DBHelper dbHelper;
    SQLiteDatabase db;
    String user_id, dealer_id;
    List<PhoneBook> mListe;
    PhoneBookAdapter adapter;
    ListView listView;
    Button selectAllClients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_book);
        setTitle("Выберите контакты");

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        SharedPreferences SP = getSharedPreferences("user_id", MODE_PRIVATE);
        user_id = SP.getString("", "");

        SP = getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        listView = findViewById(R.id.list_twoligne_itineraire);

        selectAllClients = findViewById(R.id.selectAllClients);
        selectAllClients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; mListe.size() > i; i++) {
                    mListe.get(i).setCb1(true);
                }
                listView.setAdapter(adapter);
            }
        });

        Button importClients = findViewById(R.id.importClients);
        importClients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; mListe.size() > i; i++) {
                    if (mListe.get(i).getCb1()) {
                        String name = mListe.get(i).getTexte1();
                        String phone = HelperClass.phone_edit(mListe.get(i).getTexte2());
                        addContact(name, phone);
                    }
                }

                finish();
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        MyTask mt = new MyTask();
        mt.execute();

    }

    class MyTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(PhoneBookActivity.this);
            mProgressDialog.setMessage("Загрузка контактов ...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            mListe = contacts();

            adapter = new PhoneBookAdapter(PhoneBookActivity.this, mListe);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listView.setAdapter(adapter);
                    selectAllClients.setText(selectAllClients.getText().toString() + "(" + mListe.size() + ")");
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mProgressDialog.dismiss();
        }
    }

    void addContact(String name, String phone) {

        String sqlQuewy = "select * "
                + "FROM rgzbn_gm_ceiling_clients_contacts " +
                "where phone = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{phone});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                } while (c.moveToNext());
            } else {
                int maxIdClient = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients",
                        this, user_id);
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
                        this,
                        maxIdClient,
                        "rgzbn_gm_ceiling_clients",
                        "send");

                HelperClass.addHistory("Новый клиент", this, String.valueOf(maxIdClient));

                int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_statuses_map",
                        this, user_id);
                values = new ContentValues();
                values.put(DBHelper.KEY_ID, maxId);
                values.put(DBHelper.KEY_CLIENT_ID, maxIdClient);
                values.put(DBHelper.KEY_STATUS_ID, "1");
                values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, null, values);

                HelperClass.addExportData(
                        this,
                        maxId,
                        "rgzbn_gm_ceiling_clients_statuses_map",
                        "send");

                if ((phone.length() == 11)) {
                    int maxIdContacts = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_contacts",
                            this, user_id);
                    Log.d("logd", "addContact: " + maxIdContacts);
                    Log.d("logd", "addContact: " + maxIdClient);
                    Log.d("logd", "addContact: " + phone);
                    Log.d("logd", "addContact: " + nowDate);
                    values = new ContentValues();
                    values.put(DBHelper.KEY_ID, maxIdContacts);
                    values.put(DBHelper.KEY_CLIENT_ID, maxIdClient);
                    values.put(DBHelper.KEY_PHONE, phone);
                    values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                    db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, null, values);

                    HelperClass.addExportData(
                            this,
                            maxIdContacts,
                            "rgzbn_gm_ceiling_clients_contacts",
                            "send");
                }
            }
        }
        c.close();


    }

    List<PhoneBook> contacts() {
        List<PhoneBook> mListe = new ArrayList<PhoneBook>();
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
        String phoneNumber = "";

        ContentResolver contentResolver = getContentResolver();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
        } else {
            Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null, DISPLAY_NAME);
            if (cursor.getCount() > 0) {

                while (cursor.moveToNext()) {
                    String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
                    String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
                    int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));

                    if (hasPhoneNumber > 0) {
                        Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null,
                                Phone_CONTACT_ID + " = ?", new String[]{contact_id}, null);

                        while (phoneCursor.moveToNext()) {
                            phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                        }
                    }
                    mListe.add(new PhoneBook(name, phoneNumber, false));
                }
            }
        }

        return mListe;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
