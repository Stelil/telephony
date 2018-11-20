package ru.itceiling.telephony.Activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.amigold.fundapter.BindDictionary;
import com.amigold.fundapter.FunDapter;
import com.amigold.fundapter.extractors.StringExtractor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import ru.itceiling.telephony.AdapterList;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;
import ru.itceiling.telephony.UnderlineTextView;

public class ClientActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private String id_client, callbackDate;
    private TextView nameClient;
    private TextView phoneClient;
    private TextView txtStatusOfClient;
    private TextView txtApiPhone;
    private TextView txtCallback;
    private ListView listHistoryClient;
    private ArrayList<AdapterList> client_mas = new ArrayList<>();
    private EditText editCommentClient;
    private LinearLayout layoutPhonesClient, layoutEmailClient;
    private String dealer_id;
    private List<TextView> txtPhoneList = new ArrayList<TextView>();
    private List<TextView> txtEmailList = new ArrayList<TextView>();
    String TAG = "logd";

    Calendar dateAndTime = new GregorianCalendar();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        setTitle("Клиент");

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        id_client = getIntent().getStringExtra("id_client");

        Log.d(TAG, "id_client: " + id_client);

        nameClient = findViewById(R.id.nameClient);
        phoneClient = findViewById(R.id.phoneClient);
        txtStatusOfClient = findViewById(R.id.txtStatusOfClient);
        txtApiPhone = findViewById(R.id.txtApiPhone);
        txtCallback = findViewById(R.id.txtCallback);

        listHistoryClient = findViewById(R.id.listHistoryClient);
        editCommentClient = findViewById(R.id.editCommentClient);

        layoutPhonesClient = findViewById(R.id.layoutPhonesClient);
        layoutEmailClient = findViewById(R.id.layoutEmailClient);

        info();
        historyClient();
        phonesClient();
        emailClient();

        SharedPreferences SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

    }

    private void info() {
        String api_phone_id = "";
        String sqlQuewy = "SELECT client_name, api_phone_id "
                + "FROM rgzbn_gm_ceiling_clients" +
                " WHERE _id = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{id_client});
        if (c != null) {
            if (c.moveToFirst()) {
                String client_name = c.getString(c.getColumnIndex(c.getColumnName(0)));
                nameClient.setText(client_name);

                api_phone_id = c.getString(c.getColumnIndex(c.getColumnName(1)));
            }
        }
        c.close();

        sqlQuewy = "SELECT status_id "
                + "FROM rgzbn_gm_ceiling_clients_statuses_map" +
                " WHERE client_id = ? ";
        c = db.rawQuery(sqlQuewy, new String[]{id_client});
        if (c != null) {
            if (c.moveToFirst()) {
                String status_id = c.getString(c.getColumnIndex(c.getColumnName(0)));

                sqlQuewy = "SELECT title "
                        + "FROM rgzbn_gm_ceiling_clients_statuses" +
                        " WHERE _id = ? ";
                c = db.rawQuery(sqlQuewy, new String[]{status_id});
                if (c != null) {
                    if (c.moveToFirst()) {
                        String title = c.getString(c.getColumnIndex(c.getColumnName(0)));
                        txtStatusOfClient.setText(title);
                    }
                }
                c.close();
            }
        }
        c.close();


        try {
            sqlQuewy = "SELECT name "
                    + "FROM rgzbn_gm_ceiling_api_phones" +
                    " WHERE _id = ? ";
            c = db.rawQuery(sqlQuewy, new String[]{api_phone_id});
            if (c != null) {
                if (c.moveToFirst()) {
                    String title = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    txtApiPhone.setText(title);
                }
            }
            c.close();
        } catch (Exception e) {
            txtApiPhone.setText("");
        }

    }

    private void historyClient() {

        client_mas.clear();

        String sqlQuewy = "SELECT date_time, text "
                + "FROM rgzbn_gm_ceiling_client_history " +
                "where client_id =? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{id_client});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String date_time = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String text = c.getString(c.getColumnIndex(c.getColumnName(1)));

                    AdapterList fc = new AdapterList(null,
                            date_time, text, null, null, null);
                    client_mas.add(fc);

                } while (c.moveToNext());
            }
        }
        c.close();

        BindDictionary<AdapterList> dict = new BindDictionary<>();

        dict.addStringField(R.id.textDateTime, new StringExtractor<AdapterList>() {
            @Override
            public String getStringValue(AdapterList nc, int position) {
                return nc.getOne();
            }
        });
        dict.addStringField(R.id.textComment, new StringExtractor<AdapterList>() {
            @Override
            public String getStringValue(AdapterList nc, int position) {
                return nc.getTwo();
            }
        });

        FunDapter adapter = new FunDapter(this, client_mas, R.layout.layout_client_history_list, dict);
        listHistoryClient.setAdapter(adapter);
    }

    private void phonesClient() {

        layoutPhonesClient.removeAllViews();
        txtPhoneList.clear();
        LinearLayout.LayoutParams lin_calc = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lin_calc.weight = 1;
        lin_calc.gravity = Gravity.CENTER;
        lin_calc.setMargins(0, 2, 0, 20);

        int countBtn = 0;
        String sqlQuewy = "SELECT phone "
                + "FROM rgzbn_gm_ceiling_clients_contacts" +
                " WHERE client_id = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{id_client});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String phone = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    UnderlineTextView txt = new UnderlineTextView(this);
                    txt.setLayoutParams(lin_calc);
                    txt.setTextSize(15);
                    txt.setText(phone);
                    txt.setId(countBtn);
                    txt.setOnClickListener(getPhone);
                    txtPhoneList.add(txt);
                    txt.setGravity(Gravity.CENTER_VERTICAL);
                    txt.setTextColor(Color.parseColor("#414099"));
                    layoutPhonesClient.addView(txt);
                    countBtn++;
                } while (c.moveToNext());
            }
        }
        c.close();
    }

    View.OnClickListener getPhone = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int editId = v.getId();

            final TextView txt = txtPhoneList.get(editId);

            Log.d("logd", txt.getText().toString());

            String[] array = new String[]{"Изменить", "Позвонить", "Удалить"};

            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(ClientActivity.this);
            builder.setTitle("Выберите действие")
                    .setNegativeButton("Отмена",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            builder.setItems(array, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    // TODO Auto-generated method stub

                    switch (item) {
                        case 0:

                            final Context context = ClientActivity.this;
                            View promptsView;
                            LayoutInflater li = LayoutInflater.from(context);
                            promptsView = li.inflate(R.layout.dialog_add_client, null);
                            AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
                            mDialogBuilder.setView(promptsView);

                            final TextView textNameClient = (TextView) promptsView.findViewById(R.id.textNameClient);
                            textNameClient.setVisibility(View.GONE);
                            final EditText nameClient = (EditText) promptsView.findViewById(R.id.nameClient);
                            nameClient.setVisibility(View.GONE);
                            final EditText phoneClient = (EditText) promptsView.findViewById(R.id.phoneClient);

                            phoneClient.setText(txt.getText().toString());
                            final String old_number = txt.getText().toString();
                            String number_id = "";

                            SQLiteDatabase db = dbHelper.getReadableDatabase();
                            String sqlQuewy = "select _id "
                                    + "FROM rgzbn_gm_ceiling_clients_contacts " +
                                    "where phone = ?";
                            Cursor cc = db.rawQuery(sqlQuewy, new String[]{txt.getText().toString()});
                            if (cc != null) {
                                if (cc.moveToFirst()) {
                                    do {
                                        number_id = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                                    } while (cc.moveToNext());
                                }
                            }
                            cc.close();

                            final String finalNumber_id = number_id;
                            mDialogBuilder
                                    .setCancelable(false)
                                    .setTitle("Изменение номера")
                                    .setPositiveButton("OK",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {

                                                    if (phoneClient.getText().toString().length() == 11) {
                                                        if (old_number.equals(phoneClient.getText().toString())) {
                                                        } else {
                                                            DBHelper dbHelper = new DBHelper(context);
                                                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                                                            ContentValues values = new ContentValues();
                                                            values.put(DBHelper.KEY_PHONE, phoneClient.getText().toString());
                                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, values, "_id = ?",
                                                                    new String[]{finalNumber_id});

                                                            phonesClient();
                                                            Toast toast = Toast.makeText(context.getApplicationContext(),
                                                                    "Номер изменён ", Toast.LENGTH_SHORT);
                                                            toast.show();
                                                        }
                                                    } else {
                                                        Toast toast = Toast.makeText(context.getApplicationContext(),
                                                                "Проверьте правильность телефона ", Toast.LENGTH_SHORT);
                                                        toast.show();
                                                    }
                                                }
                                            })
                                    .setNegativeButton("Отмена",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                }
                                            });

                            AlertDialog alertDialog = mDialogBuilder.create();
                            alertDialog.getWindow().setBackgroundDrawableResource(R.color.colorWhite);
                            alertDialog.show();

                            break;
                        case 1:
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse("tel:+" + txt.getText().toString()));
                            startActivity(intent);
                            break;
                        case 2:
                            AlertDialog.Builder builder = new AlertDialog.Builder(ClientActivity.this);
                            builder.setTitle("Удалить номер " + txt.getText().toString() + " ?")
                                    .setMessage(null)
                                    .setIcon(null)
                                    .setCancelable(false)
                                    .setPositiveButton("Да",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {

                                                    dbHelper = new DBHelper(ClientActivity.this);
                                                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                                                    String id_phone = "";
                                                    String sqlQuewy = "SELECT _id "
                                                            + "FROM rgzbn_gm_ceiling_clients_contacts" +
                                                            " WHERE phone = ? ";
                                                    Cursor cc = db.rawQuery(sqlQuewy, new String[]{txt.getText().toString()});
                                                    if (cc != null) {
                                                        if (cc.moveToFirst()) {
                                                            id_phone = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));

                                                        }
                                                    }
                                                    cc.close();

                                                    db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, "_id = ?", new String[]{id_phone});

                                                    phonesClient();

                                                    Toast toast = Toast.makeText(ClientActivity.this.getApplicationContext(),
                                                            "Номер удалён ", Toast.LENGTH_SHORT);
                                                    toast.show();
                                                }
                                            })
                                    .setNegativeButton("Отмена",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                }
                                            });
                            AlertDialog alert = builder.create();
                            alert.show();
                            break;
                    }
                }
            });

            builder.setCancelable(false);
            builder.create();
            builder.show();

        }
    };

    private void emailClient() {

        LinearLayout.LayoutParams lin_calc = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lin_calc.weight = 1;
        lin_calc.gravity = Gravity.CENTER;
        lin_calc.setMargins(0, 2, 0, 10);

        layoutEmailClient.removeAllViews();
        int countTxt = 0;
        String sqlQuewy = "SELECT contact "
                + "FROM rgzbn_gm_ceiling_clients_dop_contacts" +
                " WHERE client_id = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{id_client});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String contact = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    UnderlineTextView txt = new UnderlineTextView(this);
                    txt.setLayoutParams(lin_calc);
                    txt.setTextSize(15);
                    txt.setText(contact);
                    txt.setId(countTxt);
                    txt.setOnClickListener(getEmail);
                    txt.setGravity(Gravity.CENTER_VERTICAL);
                    txt.setTextColor(Color.parseColor("#414099"));
                    layoutEmailClient.addView(txt);
                    txtEmailList.add(txt);
                    countTxt++;
                } while (c.moveToNext());
            }
        }
        c.close();
    }

    View.OnClickListener getEmail = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int editId = v.getId();

            final TextView txt = txtEmailList.get(editId);

            Log.d("logd", txt.getText().toString());

            String[] array = new String[]{"Изменить", "Удалить"};

            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(ClientActivity.this);
            builder.setTitle("Выберите действие")
                    .setNegativeButton("Отмена",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            builder.setItems(array, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    // TODO Auto-generated method stub

                    switch (item) {
                        case 0:

                            final Context context = ClientActivity.this;
                            View promptsView;
                            LayoutInflater li = LayoutInflater.from(context);
                            promptsView = li.inflate(R.layout.dialog_add_client, null);
                            AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
                            mDialogBuilder.setView(promptsView);

                            final TextView textNameClient = (TextView) promptsView.findViewById(R.id.textNameClient);
                            textNameClient.setVisibility(View.GONE);
                            final EditText nameClient = (EditText) promptsView.findViewById(R.id.nameClient);
                            nameClient.setVisibility(View.GONE);
                            final TextView textPhoneClient = (TextView) promptsView.findViewById(R.id.textPhoneClient);
                            textPhoneClient.setText("Почта");
                            final EditText emailClient = (EditText) promptsView.findViewById(R.id.phoneClient);

                            emailClient.setText(txt.getText().toString());
                            final String oldEmail = txt.getText().toString();
                            String contact_id = "";

                            SQLiteDatabase db = dbHelper.getReadableDatabase();
                            String sqlQuewy = "select _id "
                                    + "FROM rgzbn_gm_ceiling_clients_dop_contacts " +
                                    "where contact = ?";
                            Cursor cc = db.rawQuery(sqlQuewy, new String[]{txt.getText().toString()});
                            if (cc != null) {
                                if (cc.moveToFirst()) {
                                    do {
                                        contact_id = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                                    } while (cc.moveToNext());
                                }
                            }
                            cc.close();

                            final String finalNumber_id = contact_id;
                            mDialogBuilder
                                    .setCancelable(false)
                                    .setTitle("Изменение почты")
                                    .setPositiveButton("OK",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {

                                                    if (HelperClass.validateMail(emailClient.getText().toString())) {
                                                        if (oldEmail.equals(emailClient.getText().toString())) {
                                                        } else {
                                                            DBHelper dbHelper = new DBHelper(context);
                                                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                                                            ContentValues values = new ContentValues();
                                                            values.put(DBHelper.KEY_CONTACT, emailClient.getText().toString());
                                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS, values, "_id = ?",
                                                                    new String[]{finalNumber_id});

                                                            emailClient();
                                                            Toast toast = Toast.makeText(context.getApplicationContext(),
                                                                    "Почта изменёна ", Toast.LENGTH_SHORT);
                                                            toast.show();
                                                        }
                                                    } else {
                                                        Toast toast = Toast.makeText(context.getApplicationContext(),
                                                                "Проверьте правильность почты ", Toast.LENGTH_SHORT);
                                                        toast.show();
                                                    }
                                                }
                                            })
                                    .setNegativeButton("Отмена",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                }
                                            });

                            AlertDialog alertDialog = mDialogBuilder.create();
                            alertDialog.getWindow().setBackgroundDrawableResource(R.color.colorWhite);
                            alertDialog.show();

                            break;
                        case 1:
                            AlertDialog.Builder builder = new AlertDialog.Builder(ClientActivity.this);
                            builder.setTitle("Удалить почту " + txt.getText().toString() + " ?")
                                    .setMessage(null)
                                    .setIcon(null)
                                    .setCancelable(false)
                                    .setPositiveButton("Да",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {

                                                    dbHelper = new DBHelper(ClientActivity.this);
                                                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                                                    String id_phone = "";
                                                    String sqlQuewy = "SELECT _id "
                                                            + "FROM rgzbn_gm_ceiling_clients_dop_contacts" +
                                                            " WHERE contact = ? ";
                                                    Cursor cc = db.rawQuery(sqlQuewy, new String[]{txt.getText().toString()});
                                                    if (cc != null) {
                                                        if (cc.moveToFirst()) {
                                                            id_phone = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                                                        }
                                                    }
                                                    cc.close();

                                                    db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS,
                                                            "_id = ?", new String[]{id_phone});
                                                    emailClient();
                                                    Toast toast = Toast.makeText(ClientActivity.this.getApplicationContext(),
                                                            "Почта удаленa ", Toast.LENGTH_SHORT);
                                                    toast.show();
                                                }
                                            })
                                    .setNegativeButton("Отмена",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                }
                                            });
                            AlertDialog alert = builder.create();
                            alert.show();
                            break;
                    }
                }
            });

            builder.setCancelable(false);
            builder.create();
            builder.show();

        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onButtonEditStatusOfClient(View view) {
        final Context context = ClientActivity.this;
        DBHelper dbHelper = new DBHelper(context);
        db = dbHelper.getWritableDatabase();

        final ArrayList<String> arrayList = new ArrayList<>();
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.dialog_status_client, null);
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
        mDialogBuilder.setView(promptsView);
        final EditText editText = (EditText) promptsView.findViewById(R.id.editText);
        Button button = (Button) promptsView.findViewById(R.id.button);
        final ListView listView = (ListView) promptsView.findViewById(R.id.listView);

        String sqlQuewy = "select _id, title "
                + "FROM rgzbn_gm_ceiling_clients_statuses";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String idd = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String title = c.getString(c.getColumnIndex(c.getColumnName(1)));

                    Log.d(TAG, "id: " + idd + " " + "title " + title);
                    arrayList.add(title);
                } while (c.moveToNext());
            }
            c.close();
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (editText.getText().toString().length() > 0) {

                    arrayList.clear();
                    int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_statuses", context, dealer_id);
                    ContentValues values = new ContentValues();
                    values.put(DBHelper.KEY_ID, maxId);
                    values.put(DBHelper.KEY_TITLE, editText.getText().toString());
                    values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                    values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.now_date());
                    db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, null, values);

                    String sqlQuewy = "select _id, title "
                            + "FROM rgzbn_gm_ceiling_clients_statuses ";
                    Cursor c = db.rawQuery(sqlQuewy, new String[]{});
                    if (c != null) {
                        if (c.moveToFirst()) {
                            do {
                                String idd = c.getString(c.getColumnIndex(c.getColumnName(0)));
                                String title = c.getString(c.getColumnIndex(c.getColumnName(1)));

                                Log.d(TAG, "id: " + idd + " " + "title " + title);
                                arrayList.add(title);
                            } while (c.moveToNext());
                        }
                        c.close();
                    }

                    String[] array = arrayList.toArray(new String[0]);
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                            android.R.layout.simple_list_item_1, array);
                    listView.setAdapter(adapter);
                    editText.setText("");

                } else {
                    Toast.makeText(context.getApplicationContext(), "Введите название статуса",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        final AlertDialog Alertdialog = new AlertDialog.Builder(context)
                .setView(promptsView)
                .setTitle("Добавьте или выберите статус")
                .setNegativeButton("Назад", null)
                .setCancelable(false)
                .create();

        Alertdialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button_negative = ((AlertDialog) Alertdialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                button_negative.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Alertdialog.dismiss();
                    }
                });
            }
        });

        Alertdialog.getWindow().setBackgroundDrawableResource(R.color.colorWhite);
        Alertdialog.show();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
                                    long id) {

                int idStatus = 0;
                String sqlQuewy = "select _id "
                        + "FROM rgzbn_gm_ceiling_clients_statuses " +
                        "where title = ?";
                Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(((TextView) itemClicked).getText())});
                if (c != null) {
                    if (c.moveToFirst()) {
                        do {
                            idStatus = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                        } while (c.moveToNext());
                    }
                    c.close();
                }

                ContentValues values = new ContentValues();
                int count = 0;
                sqlQuewy = "SELECT * "
                        + "FROM rgzbn_gm_ceiling_clients_statuses_map" +
                        " WHERE client_id = ?";
                c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id_client)});
                if (c != null) {
                    if (c.moveToFirst()) {
                        values.put(DBHelper.KEY_STATUS_ID, idStatus);
                        values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.now_date());
                        db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, values,
                                "client_id = ?",
                                new String[]{id_client});
                        count++;
                    }
                }
                c.close();

                if (count == 0) {
                    int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_statuses_map",
                            ClientActivity.this,
                            dealer_id);
                    values.put(DBHelper.KEY_ID, maxId);
                    values.put(DBHelper.KEY_CLIENT_ID, id_client);
                    values.put(DBHelper.KEY_STATUS_ID, idStatus);
                    values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.now_date());
                    db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, null, values);
                }

                Toast.makeText(getApplicationContext(), "Статус изменён",
                        Toast.LENGTH_SHORT).show();

                info();
                Alertdialog.dismiss();

            }
        });
    }

    public void onButtonEditApiPhone(View view) {
        final Context context = ClientActivity.this;
        DBHelper dbHelper = new DBHelper(context);
        db = dbHelper.getWritableDatabase();

        final ArrayList<String> arrayList = new ArrayList<>();
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.dialog_status_client, null);
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
        mDialogBuilder.setView(promptsView);
        final EditText editText = (EditText) promptsView.findViewById(R.id.editText);
        Button button = (Button) promptsView.findViewById(R.id.button);
        final ListView listView = (ListView) promptsView.findViewById(R.id.listView);

        String sqlQuewy = "select name "
                + "FROM rgzbn_gm_ceiling_api_phones";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String name = c.getString(c.getColumnIndex(c.getColumnName(0)));

                    arrayList.add(name);

                } while (c.moveToNext());
            }
            c.close();
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (editText.getText().toString().length() > 0) {

                    arrayList.clear();

                    int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_api_phones", context, dealer_id);

                    ContentValues values = new ContentValues();
                    values.put(DBHelper.KEY_ID, maxId);
                    values.put(DBHelper.KEY_NAME, editText.getText().toString());
                    values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.now_date());
                    db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_API_PHONES, null, values);

                    String sqlQuewy = "select name "
                            + "FROM rgzbn_gm_ceiling_api_phones ";
                    Cursor c = db.rawQuery(sqlQuewy, new String[]{});
                    if (c != null) {
                        if (c.moveToFirst()) {
                            do {
                                String name = c.getString(c.getColumnIndex(c.getColumnName(0)));
                                arrayList.add(name);

                            } while (c.moveToNext());
                        }
                        c.close();
                    }

                    String[] array = arrayList.toArray(new String[0]);

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                            android.R.layout.simple_list_item_1, array);
                    listView.setAdapter(adapter);
                    editText.setText("");

                } else {
                    Toast.makeText(context.getApplicationContext(), "Введите название рекламы",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        final AlertDialog Alertdialog = new AlertDialog.Builder(context)
                .setView(promptsView)
                .setTitle("Добавьте или выберите рекламу")
                .setNegativeButton("Назад", null)
                .setCancelable(false)
                .create();

        Alertdialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button_negative = ((AlertDialog) Alertdialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                button_negative.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Alertdialog.dismiss();
                    }
                });
            }
        });

        Alertdialog.getWindow().setBackgroundDrawableResource(R.color.colorWhite);
        Alertdialog.show();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
                                    long id) {

                int idApi = 0;
                String sqlQuewy = "select _id "
                        + "FROM rgzbn_gm_ceiling_api_phones " +
                        "where name = ?";
                Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(((TextView) itemClicked).getText())});
                if (c != null) {
                    if (c.moveToFirst()) {
                        do {
                            idApi = c.getInt(c.getColumnIndex(c.getColumnName(0)));

                        } while (c.moveToNext());
                    }
                    c.close();
                }

                ContentValues values = new ContentValues();
                values.put(DBHelper.KEY_API_PHONE_ID, idApi);
                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, values,
                        "_id = ?",
                        new String[]{id_client});

                Toast.makeText(getApplicationContext(), "Реклама изменён",
                        Toast.LENGTH_SHORT).show();

                info();
                Alertdialog.dismiss();
            }
        });
    }

    public void onButtonEditNameClient(View view) {
        final Context context = ClientActivity.this;
        View promptsView;
        LayoutInflater li = LayoutInflater.from(context);
        promptsView = li.inflate(R.layout.dialog_add_client, null);
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
        mDialogBuilder.setView(promptsView);

        final EditText nameClient = (EditText) promptsView.findViewById(R.id.nameClient);

        final TextView textPhoneClient = (TextView) promptsView.findViewById(R.id.textPhoneClient);
        textPhoneClient.setVisibility(View.GONE);
        final EditText phoneClient = (EditText) promptsView.findViewById(R.id.phoneClient);
        phoneClient.setVisibility(View.GONE);

        nameClient.setText(this.nameClient.getText().toString());
        final String old_name = this.nameClient.getText().toString();

        mDialogBuilder
                .setCancelable(false)
                .setTitle("Изменение имени")
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                String new_name = nameClient.getText().toString();
                                if (new_name.length() > 0) {
                                    if (old_name.equals(new_name)) {
                                    } else {
                                        DBHelper dbHelper = new DBHelper(context);
                                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                                        ContentValues values = new ContentValues();
                                        values.put(DBHelper.KEY_CLIENT_NAME, new_name);
                                        db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, values, "_id = ?",
                                                new String[]{id_client});

                                        info();
                                        Toast toast = Toast.makeText(context.getApplicationContext(),
                                                "Имя изменено ", Toast.LENGTH_SHORT);
                                        toast.show();
                                    }
                                } else {
                                    Toast toast = Toast.makeText(context.getApplicationContext(),
                                            "Проверьте правильность имени ", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            }
                        })
                .setNegativeButton("Отмена",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = mDialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(R.color.colorWhite);
        alertDialog.show();
    }

    public void onButtonAddComment(View view) {
        String comment = editCommentClient.getText().toString();

        if (comment.length() > 0) {
            HelperClass.addHistory(comment, ClientActivity.this, String.valueOf(id_client));

            Toast.makeText(getApplicationContext(), "Комментарий добавлен",
                    Toast.LENGTH_SHORT).show();

            historyClient();
            editCommentClient.setText("");
        } else {
            Toast.makeText(getApplicationContext(), "Введите текст комментария",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void onButtonAddEmail(View view) {
        TextView addEmailClient = findViewById(R.id.addEmailClient);
        String email = addEmailClient.getText().toString();
        if (HelperClass.validateMail(email)) {

            try {
                int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_dop_contacts",
                        ClientActivity.this, dealer_id);
                ContentValues values = new ContentValues();
                values.put(DBHelper.KEY_ID, maxId);
                values.put(DBHelper.KEY_CLIENT_ID, id_client);
                values.put(DBHelper.KEY_TYPE_ID, "1");
                values.put(DBHelper.KEY_CONTACT, email);
                db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS, null, values);
            } catch (Exception e) {
                Log.d("logd", "error: " + e);
            }

            Toast.makeText(getApplicationContext(), "Почта добавлена", Toast.LENGTH_LONG).show();
            emailClient();
            addEmailClient.setText("");
        } else {
            Toast.makeText(getApplicationContext(), "Неверный формат почты", Toast.LENGTH_LONG).show();
        }
    }

    public void onButtonAddPhone(View view) {
        TextView addPhoneClient = findViewById(R.id.addPhoneClient);
        String phone = addPhoneClient.getText().toString();
        if (phone.length() == 11) {

            try {
                int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_contacts",
                        ClientActivity.this, dealer_id);
                ContentValues values = new ContentValues();
                values.put(DBHelper.KEY_ID, maxId);
                values.put(DBHelper.KEY_CLIENT_ID, id_client);
                values.put(DBHelper.KEY_PHONE, phone);
                db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, null, values);
            } catch (Exception e) {
                Log.d("logd", "error: " + e);
            }

            Toast.makeText(getApplicationContext(), "Телефон добавлен", Toast.LENGTH_LONG).show();
            phonesClient();
            addPhoneClient.setText("");
        } else {
            Toast.makeText(getApplicationContext(), "Неверный формат номера", Toast.LENGTH_LONG).show();
        }
    }

    public void onButtonVisibleEmail(View view) {
        LinearLayout linearLayout = findViewById(R.id.linearEmail);
        if (linearLayout.getVisibility() == View.VISIBLE) {
            linearLayout.setVisibility(View.GONE);
        } else if (linearLayout.getVisibility() == View.GONE) {
            linearLayout.setVisibility(View.VISIBLE);
        }
    }

    public void onButtonVisiblePhones(View view) {
        LinearLayout linearLayout = findViewById(R.id.linearPhones);
        if (linearLayout.getVisibility() == View.VISIBLE) {
            linearLayout.setVisibility(View.GONE);
        } else if (linearLayout.getVisibility() == View.GONE) {
            linearLayout.setVisibility(View.VISIBLE);
        }
    }

    public void onButtonCallback(View view) {
        setTime(txtCallback);
        setDate(txtCallback);

        ImageButton btnAddCallback = findViewById(R.id.btnAddCallback);
        final TextView txtCallbackComment = findViewById(R.id.txtCallbackComment);
        final SQLiteDatabase finalDb4 = db;
        btnAddCallback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (txtCallback.getText().toString().length() > 0) {
                    HelperClass.addHistory("Добавлен звонок на " + txtCallback.getText().toString(),
                            ClientActivity.this, id_client);
                    HelperClass.addCallback(txtCallbackComment.getText().toString(),
                            ClientActivity.this, id_client, callbackDate);
                    txtCallback.setText("");
                    txtCallbackComment.setText("");

                    historyClient();
                    Toast toast = Toast.makeText(ClientActivity.this.getApplicationContext(),
                            "Звонок добавлен ", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    public void setDate(View v) {
        final Calendar cal = Calendar.getInstance();
        int mYear = cal.get(Calendar.YEAR);
        int mMonth = cal.get(Calendar.MONTH);
        int mDay = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String editTextDateParam;
                        if (monthOfYear < 9) {
                            editTextDateParam = year + "-0" + (monthOfYear + 1);
                        } else {
                            editTextDateParam = year + "-" + (monthOfYear + 1);
                        }
                        if (dayOfMonth < 10) {
                            editTextDateParam += "-0" + dayOfMonth;
                        } else {
                            editTextDateParam += "-" + dayOfMonth;
                        }
                        callbackDate = editTextDateParam;
                        Log.d(TAG, callbackDate);
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    public void setTime(View v) {
        new TimePickerDialog(this, call_time,
                dateAndTime.get(Calendar.HOUR_OF_DAY),
                dateAndTime.get(Calendar.MINUTE), true)
                .show();
    }

    DatePickerDialog.OnDateSetListener call_date = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateAndTime.set(Calendar.YEAR, year);
            dateAndTime.set(Calendar.MONTH, monthOfYear);
            dateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            setInitialDateTimeCall();
        }
    };

    TimePickerDialog.OnTimeSetListener call_time = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            dateAndTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            dateAndTime.set(Calendar.MINUTE, minute);
            setInitialDateTimeCall();
        }
    };

    private void setInitialDateTimeCall() {
        txtCallback.setText(callbackDate + " " +
                DateUtils.formatDateTime(this,
                        dateAndTime.getTimeInMillis(),
                        DateUtils.FORMAT_SHOW_TIME));
        callbackDate += " " + DateUtils.formatDateTime(this,
                dateAndTime.getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME);

    }

}