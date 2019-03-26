package ru.itceiling.telephony.activity;

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
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import java.util.GregorianCalendar;
import java.util.List;

import ru.itceiling.telephony.AdapterList;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.HistoryClient;
import ru.itceiling.telephony.R;
import ru.itceiling.telephony.UnderlineTextView;
import ru.itceiling.telephony.adapter.RVAdapterHistoryClient;
import ru.itceiling.telephony.adapter.RecyclerViewClickListener;
import ru.itceiling.telephony.broadcaster.ExportDataReceiver;

public class ClientActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private String id_client, callbackDate;
    private ImageButton btnAddVoiceComment;
    private TextView nameClient;
    private TextView phoneClient;
    private TextView txtStatusOfClient;
    private TextView txtApiPhone;
    private TextView txtCallback, txtEditCallback, txtManagerOfClient;
    private RecyclerView listHistoryClient;
    private ArrayList<HistoryClient> historyClients = new ArrayList<>();
    private EditText editCommentClient, txtEditCallbackComment;
    private LinearLayout layoutPhonesClient, layoutEmailClient;
    private Button btnEditCallback;
    private LinearLayout layoutCallback, linearNewCall;
    private String dealer_id, check = "false", user_id;
    private List<TextView> txtPhoneList = new ArrayList<TextView>();
    private List<TextView> txtEmailList = new ArrayList<TextView>();
    private RVAdapterHistoryClient adapter;
    String TAG = "logd";

    Calendar dateAndTime = new GregorianCalendar();

    private SpeechRecognizer sr;

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

        Log.d(TAG, "onCreate: " + id_client);

        Log.d(TAG, "onCreate: " + getIntent().getStringExtra("check"));

        nameClient = findViewById(R.id.nameClient);
        nameClient.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent intent = new Intent(ClientActivity.this, ClientBrowserActivity.class);
                intent.putExtra("id_client", id_client);
                startActivity(intent);
                return false;
            }
        });

        phoneClient = findViewById(R.id.phoneClient);
        txtStatusOfClient = findViewById(R.id.txtStatusOfClient);
        txtManagerOfClient = findViewById(R.id.txtManagerOfClient);
        txtCallback = findViewById(R.id.txtCallback);
        txtEditCallback = findViewById(R.id.txtEditCallback);
        txtEditCallbackComment = findViewById(R.id.txtEditCallbackComment);

        btnAddVoiceComment = findViewById(R.id.btnAddVoiceComment);

        listHistoryClient = findViewById(R.id.listHistoryClient);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        listHistoryClient.setLayoutManager(llm);
        listHistoryClient.setHasFixedSize(true);
        listHistoryClient.setNestedScrollingEnabled(true);

        editCommentClient = findViewById(R.id.editCommentClient);

        check = getIntent().getStringExtra("check");
        if (check.equals("true")) {
            btnEditCallback = findViewById(R.id.btnEditCallback);
            btnEditCallback.setVisibility(View.VISIBLE);
        }

        layoutPhonesClient = findViewById(R.id.layoutPhonesClient);
        layoutEmailClient = findViewById(R.id.layoutEmailClient);

        info();
        historyClient();
        phonesClient();
        emailClient();

        SharedPreferences SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        SP = this.getSharedPreferences("user_id", MODE_PRIVATE);
        user_id = SP.getString("", "");

        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new listener());

        linearNewCall = findViewById(R.id.linearNewCall);
        layoutCallback = findViewById(R.id.layoutCallback);

        SP = this.getSharedPreferences("group_id", MODE_PRIVATE);
        if (SP.getString("", "").equals("13")) {
            LinearLayout layoutManager = findViewById(R.id.layoutManager);
            layoutManager.setVisibility(View.GONE);
        }

        ExportDataReceiver exportDataReceiver = new ExportDataReceiver();
        exportDataReceiver.CancelAlarm(this);
    }

    public void onButtonEditCallback(View view) {
        if (layoutCallback.getVisibility() == View.GONE) {
            layoutCallback.setVisibility(View.VISIBLE);
            if (linearNewCall.getVisibility() == View.VISIBLE) {
                linearNewCall.setVisibility(View.GONE);
            }
            setDateEditCallback(txtEditCallback);
        } else {
            layoutCallback.setVisibility(View.GONE);
        }
    }

    public void onEditButtonCallback(View view) {
        setDateEditCallback(txtEditCallback);
    }

    public void btnEditAddCallback(View view) {
        if (txtEditCallback.getText().toString().length() > 0) {
            String sqlQuewy;
            Cursor c;
            sqlQuewy = "SELECT _id "
                    + "FROM rgzbn_gm_ceiling_callback " +
                    "where client_id = ? " +
                    "order by date_time desc";
            c = db.rawQuery(sqlQuewy, new String[]{id_client});
            if (c != null) {
                if (c.moveToFirst()) {
                    String id = c.getString(c.getColumnIndex(c.getColumnName(0)));

                    ContentValues values = new ContentValues();
                    if (txtEditCallbackComment.getText().toString().equals("")) {
                    } else {
                        values.put(DBHelper.KEY_COMMENT, txtEditCallbackComment.getText().toString());
                    }
                    values.put(DBHelper.KEY_DATE_TIME, callbackDate + ":00");
                    values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
                    values.put(DBHelper.KEY_MANAGER_ID, user_id);
                    db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, values, "_id = ?", new String[]{id});

                    values = new ContentValues();
                    values.put(DBHelper.KEY_MANAGER_ID, user_id);
                    db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, values, "_id = ?", new String[]{id_client});

                    HelperClass.addExportData(
                            ClientActivity.this,
                            Integer.parseInt(id),
                            "rgzbn_gm_ceiling_callback",
                            "send");

                    HelperClass.addExportData(
                            ClientActivity.this,
                            Integer.parseInt(id_client),
                            "rgzbn_gm_ceiling_clients",
                            "send");

                    HelperClass.addHistory("Звонок перенесён на " + callbackDate,
                            ClientActivity.this,
                            id_client);

                    Toast toast = Toast.makeText(ClientActivity.this.getApplicationContext(),
                            "Звонок перенесён ", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
            c.close();

            txtEditCallback.setText("");
            txtEditCallbackComment.setText("");

            historyClient();
        }
    }

    public void setDateEditCallback(final View v) {
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

                        setTimeEditCallback(txtEditCallback);
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    public void setTimeEditCallback(View v) {
        new TimePickerDialog(this, callTimeEditCallback,
                dateAndTime.get(Calendar.HOUR_OF_DAY),
                dateAndTime.get(Calendar.MINUTE), true)
                .show();
    }

    TimePickerDialog.OnTimeSetListener callTimeEditCallback = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            dateAndTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            dateAndTime.set(Calendar.MINUTE, minute);
            setInitialDateTimeEditCallBack(txtEditCallback);
        }
    };

    private void setInitialDateTimeEditCallBack(TextView textView) {
        textView.setText(callbackDate + " " +
                DateUtils.formatDateTime(this,
                        dateAndTime.getTimeInMillis(),
                        DateUtils.FORMAT_SHOW_TIME));
        callbackDate += " " + DateUtils.formatDateTime(this,
                dateAndTime.getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME);

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

                //api_phone_id = c.getString(c.getColumnIndex(c.getColumnName(1)));
            }
        }
        c.close();

        sqlQuewy = "SELECT status_id "
                + "FROM rgzbn_gm_ceiling_clients_statuses_map" +
                " WHERE client_id = ? " +
                "order by _id";
        c = db.rawQuery(sqlQuewy, new String[]{id_client});
        if (c != null) {
            if (c.moveToLast()) {
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

        sqlQuewy = "SELECT us.name " +
                "FROM rgzbn_gm_ceiling_clients AS cl " +
                "INNER JOIN rgzbn_users AS us " +
                "ON us._id = cl.manager_id " +
                "WHERE cl._id = ?";
        c = db.rawQuery(sqlQuewy, new String[]{id_client});
        if (c != null) {
            if (c.moveToLast()) {
                String name = c.getString(c.getColumnIndex(c.getColumnName(0)));
                txtManagerOfClient.setText(name);
            }
        }
        c.close();
    }

    private void historyClient() {

        historyClients.clear();

        String sqlQuewy = "SELECT date_time, text, type_id "
                + "FROM rgzbn_gm_ceiling_client_history " +
                "where client_id =? " +
                "order by date_time";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{id_client});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String date_time = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String text = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    int type = c.getInt(c.getColumnIndex(c.getColumnName(2)));

                    historyClients.add(new HistoryClient(date_time, text, type));

                } while (c.moveToNext());
            }
        }
        c.close();

        adapter = new RVAdapterHistoryClient(historyClients, this);
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listHistoryClient.setAdapter(adapter);
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "ListClients error: " + e);
        }

        /*BindDictionary<AdapterList> dict = new BindDictionary<>();

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

        adapter = new FunDapter(this, client_mas, R.layout.layout_client_history_list, dict);
        listHistoryClient.setAdapter(adapter);*/

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

            SharedPreferences SP = getSharedPreferences("group_id", MODE_PRIVATE);
            if (SP.getString("", "").equals("13")) {

                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:+" + txt.getText().toString()));
                startActivity(intent);
            } else {

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

                                                                HelperClass.addExportData(
                                                                        ClientActivity.this,
                                                                        Integer.valueOf(finalNumber_id),
                                                                        "rgzbn_gm_ceiling_clients_contacts",
                                                                        "send");

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

                                                        db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS,
                                                                "_id = ?",
                                                                new String[]{id_phone});

                                                        HelperClass.addExportData(
                                                                ClientActivity.this,
                                                                Integer.valueOf(id_phone),
                                                                "rgzbn_gm_ceiling_clients_contacts",
                                                                "delete");

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

            SharedPreferences SP = getSharedPreferences("group_id", MODE_PRIVATE);
            if (SP.getString("", "").equals("13")) {

                Intent email = new Intent(Intent.ACTION_SEND);
                email.putExtra(Intent.EXTRA_EMAIL, new String[]{txt.getText().toString()});
                email.setType("message/rfc822");
                startActivity(Intent.createChooser(email, "Выберите приложение для отправки"));
            } else {
                String[] array = new String[]{"Изменить", "Отправить", "Удалить"};

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

                                                                HelperClass.addExportData(
                                                                        ClientActivity.this,
                                                                        Integer.valueOf(finalNumber_id),
                                                                        "rgzbn_gm_ceiling_clients_dop_contacts",
                                                                        "send");

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
                                Intent email = new Intent(Intent.ACTION_SEND);
                                email.putExtra(Intent.EXTRA_EMAIL, new String[]{txt.getText().toString()});
                                email.setType("message/rfc822");
                                startActivity(Intent.createChooser(email, "Выберите приложение для отправки"));
                                break;
                            case 2:
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

                                                        HelperClass.addExportData(
                                                                ClientActivity.this,
                                                                Integer.valueOf(id_phone),
                                                                "rgzbn_gm_ceiling_clients_dop_contacts",
                                                                "delete");

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
                    int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_statuses", context, user_id);
                    ContentValues values = new ContentValues();
                    values.put(DBHelper.KEY_ID, maxId);
                    values.put(DBHelper.KEY_TITLE, editText.getText().toString());
                    values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                    values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
                    db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, null, values);

                    HelperClass.addExportData(
                            ClientActivity.this,
                            maxId,
                            "rgzbn_gm_ceiling_clients_statuses",
                            "send");

                    String sqlQuewy = "select _id, title "
                            + "FROM rgzbn_gm_ceiling_clients_statuses ";
                    Cursor c = db.rawQuery(sqlQuewy, new String[]{});
                    if (c != null) {
                        if (c.moveToFirst()) {
                            do {
                                String idd = c.getString(c.getColumnIndex(c.getColumnName(0)));
                                String title = c.getString(c.getColumnIndex(c.getColumnName(1)));
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

                int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_statuses_map",
                        ClientActivity.this,
                        user_id);
                values.put(DBHelper.KEY_ID, maxId);
                values.put(DBHelper.KEY_CLIENT_ID, id_client);
                values.put(DBHelper.KEY_STATUS_ID, idStatus);
                values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
                db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, null, values);

                HelperClass.addExportData(
                        ClientActivity.this,
                        maxId,
                        "rgzbn_gm_ceiling_clients_statuses_map",
                        "send");

                Toast.makeText(getApplicationContext(), "Статус изменён",
                        Toast.LENGTH_SHORT).show();

                info();
                Alertdialog.dismiss();

            }
        });
    }

    public void onButtonEditManagerOfClient(View view) {
        final Context context = ClientActivity.this;
        DBHelper dbHelper = new DBHelper(context);
        db = dbHelper.getWritableDatabase();

        final ArrayList<String> arrayList = new ArrayList<>();
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.dialog_status_client, null);
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
        mDialogBuilder.setView(promptsView);
        final ListView listView = (ListView) promptsView.findViewById(R.id.listView);

        LinearLayout layoutText = promptsView.findViewById(R.id.layoutText);
        layoutText.setVisibility(View.GONE);

        String sqlQuewy = "select _id, name "
                + "FROM rgzbn_users";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String idd = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    arrayList.add(name);
                } while (c.moveToNext());
            }
            c.close();
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        final AlertDialog Alertdialog = new AlertDialog.Builder(context)
                .setView(promptsView)
                .setTitle("Выберите менеджера")
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

                int idManager = 0;
                String sqlQuewy = "select _id "
                        + "FROM rgzbn_users " +
                        "where name = ?";
                Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(((TextView) itemClicked).getText())});
                if (c != null) {
                    if (c.moveToFirst()) {
                        do {
                            idManager = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                        } while (c.moveToNext());
                    }
                    c.close();
                }

                ContentValues values = new ContentValues();
                values.put(DBHelper.KEY_MANAGER_ID, idManager);
                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, values, "_id = ?",
                        new String[]{id_client});

                HelperClass.addExportData(
                        ClientActivity.this,
                        Integer.valueOf(id_client),
                        "rgzbn_gm_ceiling_clients",
                        "send");

                values = new ContentValues();
                values.put(DBHelper.KEY_MANAGER_ID, idManager);
                sqlQuewy = "select _id "
                        + "FROM rgzbn_gm_ceiling_callback " +
                        "where client_id = ?";
                c = db.rawQuery(sqlQuewy, new String[]{id_client});
                if (c != null) {
                    if (c.moveToFirst()) {
                        do {
                            Integer idCallback = c.getInt(c.getColumnIndex(c.getColumnName(0)));

                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, values, "_id = ?",
                                    new String[]{String.valueOf(idCallback)});

                            HelperClass.addExportData(
                                    ClientActivity.this,
                                    idCallback,
                                    "rgzbn_gm_ceiling_callback",
                                    "send");

                        } while (c.moveToNext());
                    }
                    c.close();
                }

                Toast.makeText(getApplicationContext(), "Менеджер изменён",
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

                                        HelperClass.addExportData(
                                                ClientActivity.this,
                                                Integer.parseInt(id_client),
                                                "rgzbn_gm_ceiling_clients",
                                                "send");
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

    public void onButtonAddVoiceComment(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "voice.recognition.test");

        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        sr.startListening(intent);

        RotateAnimation rotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f); //2
        rotate.setDuration(1000); //3
        rotate.setRepeatMode(Animation.REVERSE); //4
        rotate.setRepeatCount(1000); //5

        AnimationSet set = new AnimationSet(false); //10
        set.addAnimation(rotate); //11

        btnAddVoiceComment.startAnimation(set); //12
    }

    class listener implements RecognitionListener {
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
        }

        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        public void onRmsChanged(float rmsdB) {
            Log.d(TAG, "onRmsChanged " + rmsdB);
        }


        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived");
        }

        public void onEndOfSpeech() {
            Log.d(TAG, "onEndofSpeech");
            btnAddVoiceComment.clearAnimation();
        }

        public void onError(int error) {
            Log.d(TAG, "onError!");
        }

        public void onResults(Bundle results) {
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            editCommentClient.setText(String.valueOf(data.get(0)));
        }

        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults");
        }

        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent " + eventType);
        }
    }

    public void onButtonAddEmail(View view) {
        TextView addEmailClient = findViewById(R.id.addEmailClient);
        String email = addEmailClient.getText().toString();
        if (HelperClass.validateMail(email)) {

            try {
                int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_dop_contacts",
                        ClientActivity.this, user_id);
                ContentValues values = new ContentValues();
                values.put(DBHelper.KEY_ID, maxId);
                values.put(DBHelper.KEY_CLIENT_ID, id_client);
                values.put(DBHelper.KEY_TYPE_ID, "1");
                values.put(DBHelper.KEY_CONTACT, email);
                values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
                db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS, null, values);

                HelperClass.addExportData(
                        ClientActivity.this,
                        maxId,
                        "rgzbn_gm_ceiling_clients_dop_contacts",
                        "send");

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
                        ClientActivity.this, user_id);
                ContentValues values = new ContentValues();
                values.put(DBHelper.KEY_ID, maxId);
                values.put(DBHelper.KEY_CLIENT_ID, id_client);
                values.put(DBHelper.KEY_PHONE, phone);
                values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
                db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, null, values);

                HelperClass.addExportData(
                        ClientActivity.this,
                        maxId,
                        "rgzbn_gm_ceiling_clients_contacts",
                        "send");

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
        setDate(txtCallback);
    }

    public void btnAddCallback(View view) {
        final TextView txtCallbackComment = findViewById(R.id.txtCallbackComment);
        if (txtCallback.getText().toString().length() > 0) {
            if (check.equals("false")) {

                Log.d(TAG, "onClick: history new");
                HelperClass.addHistory("Добавлен звонок на " + txtCallback.getText().toString(),
                        ClientActivity.this, id_client);

                HelperClass.addCallback(txtCallbackComment.getText().toString(),
                        ClientActivity.this, id_client, callbackDate, user_id);
                txtCallback.setText("");
                txtCallbackComment.setText("");

                historyClient();
                Toast toast = Toast.makeText(ClientActivity.this.getApplicationContext(),
                        "Звонок добавлен ", Toast.LENGTH_SHORT);
                toast.show();

            } else {

                Log.d(TAG, "onClick: history callback upd");
                String sqlQuewy;
                Cursor c;
                sqlQuewy = "SELECT _id "
                        + "FROM rgzbn_gm_ceiling_callback " +
                        "where client_id = ? and substr(date_time, 1, 10) = ?" +
                        "order by date_time desc";
                c = db.rawQuery(sqlQuewy, new String[]{id_client, txtCallback.getText().toString().substring(0, 10)});
                if (c != null) {
                    if (c.moveToFirst()) {
                        String id = c.getString(c.getColumnIndex(c.getColumnName(0)));

                        ContentValues values = new ContentValues();
                        if (txtCallbackComment.getText().toString().equals("")) {
                        } else {
                            values.put(DBHelper.KEY_COMMENT, txtCallbackComment.getText().toString());
                        }
                        values.put(DBHelper.KEY_DATE_TIME, callbackDate + ":00");
                        values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
                        values.put(DBHelper.KEY_MANAGER_ID, user_id);
                        db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, values, "_id = ?", new String[]{id});

                        values = new ContentValues();
                        values.put(DBHelper.KEY_MANAGER_ID, user_id);
                        db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, values, "_id = ?", new String[]{id_client});

                        HelperClass.addExportData(
                                ClientActivity.this,
                                Integer.parseInt(id),
                                "rgzbn_gm_ceiling_callback",
                                "send");

                        HelperClass.addExportData(
                                ClientActivity.this,
                                Integer.parseInt(id_client),
                                "rgzbn_gm_ceiling_clients",
                                "send");

                        Toast toast = Toast.makeText(ClientActivity.this.getApplicationContext(),
                                "Звонок перенесён ", Toast.LENGTH_SHORT);
                        toast.show();
                    } else {

                        Log.d(TAG, "onClick: history callback new");

                        HelperClass.addCallback(txtCallbackComment.getText().toString(),
                                ClientActivity.this, id_client, callbackDate, user_id);

                        Toast toast = Toast.makeText(ClientActivity.this.getApplicationContext(),
                                "Звонок добавлен ", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
                c.close();

                txtCallback.setText("");
                txtCallbackComment.setText("");

            }
        }
    }

    public void onButtonNewCallback(View view) {
        if (linearNewCall.getVisibility() == View.GONE) {
            linearNewCall.setVisibility(View.VISIBLE);
            if (layoutCallback.getVisibility() == View.VISIBLE) {
                layoutCallback.setVisibility(View.GONE);
            }
            setDate(txtCallback);
        } else {
            linearNewCall.setVisibility(View.GONE);
        }
    }

    public void setDate(final View v) {
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

                        setTime(txtCallback);
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    public void setTime(View v) {
        new TimePickerDialog(this, callTimeCallback,
                dateAndTime.get(Calendar.HOUR_OF_DAY),
                dateAndTime.get(Calendar.MINUTE), true)
                .show();
    }

    private void setInitialDateTimeCallback(TextView textView) {
        textView.setText(callbackDate + " " +
                DateUtils.formatDateTime(this,
                        dateAndTime.getTimeInMillis(),
                        DateUtils.FORMAT_SHOW_TIME));
        callbackDate += " " + DateUtils.formatDateTime(this,
                dateAndTime.getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME);

    }

    TimePickerDialog.OnTimeSetListener callTimeCallback = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            dateAndTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            dateAndTime.set(Calendar.MINUTE, minute);
            setInitialDateTimeCallback(txtCallback);
        }
    };

}