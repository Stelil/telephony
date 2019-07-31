package ru.itceiling.telephony.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;

import org.json.simple.JSONObject;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import ru.itceiling.telephony.broadcaster.BroadcastMessagesFromMessengers;
import ru.itceiling.telephony.broadcaster.CallReceiver;
import ru.itceiling.telephony.broadcaster.CallbackReceiver;
import ru.itceiling.telephony.broadcaster.ExportDataReceiver;
import ru.itceiling.telephony.broadcaster.ImportDataReceiver;
import ru.itceiling.telephony.broadcaster.SmsBroadcaster;
import ru.itceiling.telephony.broadcaster.VKReceiver;
import ru.itceiling.telephony.data.ClientCSV;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.fragments.AnalyticsFragment;
import ru.itceiling.telephony.fragments.CallLogFragment;
import ru.itceiling.telephony.fragments.CallbackListFragment;
import ru.itceiling.telephony.fragments.ClientsListFragment;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

public class MainActivity extends AppCompatActivity {

    private CallReceiver callRecv;
    private CallbackReceiver callbackReceiver;
    private SmsBroadcaster smsBroadcaster;
    private VKReceiver vkReceiver;
    DBHelper dbHelper;
    SQLiteDatabase db;

    private static String dealer_id, user_id;
    private String phoneNumber = "";
    private static String mLastState = "";
    private String date1, date2 = "";
    int callStatus = 0;
    private static String TAG = "callReceiver";
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String fileName;
    File audiofile;

    private ImportDataReceiver importDataReceiver;
    private ExportDataReceiver exportDataReceiver;

    private static long back_pressed;

    String getPhone = "", textSearch = "";

    public BottomNavigationView navigation;

    private TextView b;
    private TextView cl;

    View badgeCallback;
    View badgeClient;

    File myExternalFile = null;
    private String filename = "SampleFile.txt";
    private String filepath = "Внутренняя память";
    String myData = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadFragment(CallbackListFragment.newInstance());

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        registerReceiver();

        SharedPreferences SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        SP = this.getSharedPreferences("user_id", MODE_PRIVATE);
        user_id = SP.getString("", "");

        navigation = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                alertDialogPermission();
            }
        }

        SP = this.getSharedPreferences("group_id", MODE_PRIVATE);
        if (SP.getString("", "").equals("13")) {
        } else {
            SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
            String user_id = SP.getString("", "");

            // если нет настроек
            String stringToParse = "";
            String sqlQuewy = "SELECT settings "
                    + "FROM rgzbn_users " +
                    "WHERE _id = ? ";
            Cursor c = db.rawQuery(sqlQuewy, new String[]{user_id});
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        stringToParse = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    } while (c.moveToNext());
                }
            }
            c.close();

            if (stringToParse.equals("null") || stringToParse.equals("")) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("CheckTimeCallback", 10); // для CallbackReceiver
                jsonObject.put("CheckTimeCall", 5);    // для CallReceiver
                saveData(String.valueOf(jsonObject), user_id);
            }
        }

        bubble();

        /*String sqlQuewy = "SELECT h._id " +
                "FROM rgzbn_gm_ceiling_client_history AS h " +
                "LEFT JOIN rgzbn_gm_ceiling_clients AS c " +
                "ON c._id = h.client_id " +
                "WHERE c._id IS NULL";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String id = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, "_id=?", new String[]{id});
                } while (c.moveToNext());
            }
        }
        c.close();*/

        myExternalFile = new File(getExternalFilesDir(filepath), filename);
    }

    void bubble() {

        BottomNavigationMenuView bottomNavigationMenuView =
                (BottomNavigationMenuView) navigation.getChildAt(0);
        View v = bottomNavigationMenuView.getChildAt(0);
        BottomNavigationItemView itemView = (BottomNavigationItemView) v;

        badgeCallback = LayoutInflater.from(this)
                .inflate(R.layout.notification_badge, itemView, true);
        b = findViewById(R.id.b);

        bottomNavigationMenuView =
                (BottomNavigationMenuView) navigation.getChildAt(0);
        v = bottomNavigationMenuView.getChildAt(1);
        itemView = (BottomNavigationItemView) v;

        badgeClient = LayoutInflater.from(this)
                .inflate(R.layout.notification_badge_client, itemView, true);
        cl = findViewById(R.id.c);
    }

    void bubbleCount() {
        SharedPreferences SP = this.getSharedPreferences("user_id", MODE_PRIVATE);
        String user_id = SP.getString("", "");

        int count = 0;
        String sqlQuewy = "SELECT count(_id) "
                + "FROM rgzbn_gm_ceiling_callback " +
                "WHERE substr(date_time,1,10) <= ?";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{HelperClass.nowDate().substring(0, 10)});
        if (c != null) {
            if (c.moveToFirst()) {
                count = c.getInt(c.getColumnIndex(c.getColumnName(0)));
            }
        }
        c.close();

        if (count > 0) {
            b.setText(String.valueOf(count));
        } else {
            b.setVisibility(View.GONE);
        }

        String as_client = HelperClass.associated_client(this, user_id);
        count = 0;
        sqlQuewy = "SELECT count(_id) "
                + "FROM rgzbn_gm_ceiling_clients " +
                "WHERE dealer_id = ? and deleted_by_user <> 1 and _id <> ?";
        c = db.rawQuery(sqlQuewy, new String[]{dealer_id, as_client});
        if (c != null) {
            if (c.moveToFirst()) {
                count = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                Log.d("ImportLog", "bubbleCount: " + count);
            }
        }
        c.close();

        if (count > 0) {
            cl.setText(String.valueOf(count));
        } else {
            cl.setVisibility(View.GONE);
        }
    }

    void saveData(String json, String user_id) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_SETTINGS, json);
        db.update(DBHelper.TABLE_USERS, values, "_id = ?", new String[]{user_id});

        HelperClass.addExportData(
                this,
                Integer.valueOf(user_id),
                "rgzbn_users",
                "send");
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.recall:
                    bubbleCount();
                    loadFragment(CallbackListFragment.newInstance());
                    return true;
                case R.id.clients:
                    bubbleCount();
                    loadFragment(ClientsListFragment.newInstance());
                    return true;
                case R.id.call_log:
                    bubbleCount();
                    loadFragment(CallLogFragment.newInstance());
                    return true;
                case R.id.analytics:
                    bubbleCount();
                    loadFragment(AnalyticsFragment.newInstance());
                    return true;
            }
            return false;
        }
    };

    private void loadFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_content, fragment);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SharedPreferences SP = this.getSharedPreferences("group_id", MODE_PRIVATE);
        if (SP.getString("", "").equals("13")) {
            MenuItem item = menu.getItem(0);
            item.setVisible(false);
            item = menu.getItem(2);
            item.setVisible(false);
        }
        //MenuItem item = menu.getItem(3);
        //item.setVisible(false);
        //item = menu.getItem(4);
        //item.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // получим идентификатор выбранного пункта меню
        int id = item.getItemId();
        // Операции для выбранного пункта меню
        switch (id) {
            case R.id.exit:

                SharedPreferences SP = getSharedPreferences("dealer_id", MODE_PRIVATE);
                SharedPreferences.Editor ed = SP.edit();
                ed.putString("", "");
                ed.commit();

                SP = getSharedPreferences("user_id", MODE_PRIVATE);
                ed = SP.edit();
                ed.putString("", "");
                ed.commit();

                SP = getSharedPreferences("enter", MODE_PRIVATE);
                ed = SP.edit();
                ed.putString("", "0");
                ed.commit();

                SP = getSharedPreferences("group_id", MODE_PRIVATE);
                ed = SP.edit();
                ed.putString("", "");
                ed.commit();

                callbackReceiver.CancelAlarm(this);

                ExportDataReceiver exportDataReceiver = new ExportDataReceiver();
                exportDataReceiver.CancelAlarm(this);

                ImportDataReceiver importDataReceiver = new ImportDataReceiver();
                importDataReceiver.CancelAlarm(this);

                finish();
                Intent intent = new Intent(this, AuthorizationActivity.class);
                intent.putExtra("exit", "true");
                startActivity(intent);
                break;

            case R.id.settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;

            case R.id.addFromPhoneBook:
                intent = new Intent(this, PhoneBookActivity.class);
                startActivity(intent);
                break;

            case R.id.manager:
                intent = new Intent(this, ManagerActivity.class);
                startActivity(intent);
                break;

            case R.id.exportDataCSV:
                alertDialogExport();
                break;

            case R.id.importDataCSV:
                alertDialogImport();
                break;
        }
        return false;
    }

    void alertDialogExport() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.alert_file_name, null);
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(this);
        mDialogBuilder.setView(promptsView);
        final EditText nameFile = promptsView.findViewById(R.id.nameFile);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(promptsView)
                .setTitle("Название файла")
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
                        String name = nameFile.getText().toString();
                        if (name.length() > 0) {
                            exportDB(name);
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getApplicationContext(), "Введите название", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    void alertDialogImport() {
        AlertDialog.Builder ad;
        ad = new AlertDialog.Builder(this);
        ad.setTitle("Выберите действие");  // заголовок
        ad.setPositiveButton("Импортировать CSV", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                importDB();
            }
        });
        ad.setNegativeButton("Пример CSV", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                exportDBExample();
            }
        });
        ad.show();
    }

    private void exportDB(String nameFile) {

        File exportDir = new File(Environment.getExternalStorageDirectory(), "");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, nameFile + ".csv");
        try {
            file.createNewFile();
            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "windows-1251"));
            List<ClientCSV> clientCSVS = generateData();
            ICsvBeanWriter csvBeanWriter = new CsvBeanWriter(writer,
                    CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);
            String[] header = new String[]{"Name", "Number", "Mail", "Comment", "Callback", "Status", "Manager", "Create"};
            csvBeanWriter.writeHeader(header);
            for (ClientCSV clientCSV : clientCSVS) {
                csvBeanWriter.write(clientCSV, header, getProcessorsExport());
            }
            csvBeanWriter.close();

            Toast.makeText(this, "Экспорт завершён. Файл находится в корне телефона", Toast.LENGTH_LONG).show();

        } catch (Exception sqlEx) {
            Toast.makeText(this, "Произошла какая-та ошибка... \n" + sqlEx, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "exportDB: " + sqlEx);
        }
    }

    private void exportDBExample() {

        File exportDir = new File(Environment.getExternalStorageDirectory(), "");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String fileName = "ExampleCSV.csv";
        File file = new File(exportDir, fileName);
        try {
            file.createNewFile();
            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "windows-1251"));
            List<ClientCSV> clientCSVS = generateDataExample();
            ICsvBeanWriter csvBeanWriter = new CsvBeanWriter(writer,
                    CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);
            String[] header = new String[]{"Name", "Number", "Mail", "Comment"};
            csvBeanWriter.writeHeader(header);
            for (ClientCSV clientCSV : clientCSVS) {
                csvBeanWriter.write(clientCSV, header, getProcessors());
            }
            csvBeanWriter.close();

            Toast.makeText(this, "Экспорт завершён. Файл находится в корне телефона", Toast.LENGTH_LONG).show();
        } catch (Exception sqlEx) {
            Toast.makeText(this, "Произошла какая-та ошибка... \n" + sqlEx, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "exportDB: " + sqlEx);
        }
    }

    private List<ClientCSV> generateData() {
        List<ClientCSV> clientCSVS = new ArrayList<>();
        String sqlQuery = "SELECT cl.client_name AS `Имя клиента`, " +
                "GROUP_CONCAT(DISTINCT clc.phone) AS `Номер`, " +
                "GROUP_CONCAT(DISTINCT cldc.contact) AS `Почта`, " +
                "stat.title AS `Статус`, " +
                "us.name AS `Менеджер` ," +
                "cl.created AS `Создан`, " +
                "GROUP_CONCAT(hist.text, '; ') AS `Коммент`, " +
                "cal.callback AS `Перезвон` " +
                "FROM `rgzbn_gm_ceiling_clients` AS cl " +
                "LEFT JOIN `rgzbn_gm_ceiling_clients_contacts` AS clc " +
                "ON clc.client_id = cl._id " +
                "LEFT JOIN `rgzbn_gm_ceiling_clients_dop_contacts` AS cldc " +
                "ON cldc.client_id = cl._id " +
                "LEFT JOIN `rgzbn_gm_ceiling_clients_statuses_map` AS statm " +
                "ON statm.client_id = cl._id " +
                "LEFT JOIN `rgzbn_gm_ceiling_clients_statuses` AS stat " +
                "ON stat._id = statm._id " +
                "LEFT JOIN (SELECT DISTINCT(h.text), h.client_id " +
                "   FROM rgzbn_gm_ceiling_client_history as h) AS hist " +
                "ON hist.client_id = cl._id " +
                "LEFT JOIN `rgzbn_users` AS us " +
                "ON us._id = cl.manager_id " +
                "LEFT JOIN (SELECT c.client_id, substr(c.date_time, 1, 16) || ' : ' || c.comment as callback " +
                "FROM `rgzbn_gm_ceiling_callback` AS c " +
                "ORDER BY c.date_time DESC) AS cal " +
                "ON cal.client_id = cl._id " +
                "WHERE cl.dealer_id = ? " +
                "GROUP BY cl._id " +
                "ORDER BY cl._id ";
        Cursor curCSV = db.rawQuery(sqlQuery, new String[]{dealer_id});
        if (curCSV != null) {
            if (curCSV.moveToFirst()) {
                do {
                    ClientCSV clientCSV = new ClientCSV();
                    String name = curCSV.getString(curCSV.getColumnIndex(curCSV.getColumnName(0)));
                    clientCSV.setName(name);
                    String phone = curCSV.getString(curCSV.getColumnIndex(curCSV.getColumnName(1)));
                    clientCSV.setNumber(phone);
                    String mail = curCSV.getString(curCSV.getColumnIndex(curCSV.getColumnName(2)));
                    clientCSV.setMail(mail);
                    String hist = curCSV.getString(curCSV.getColumnIndex(curCSV.getColumnName(6)));
                    clientCSV.setComment(hist);
                    String callback = curCSV.getString(curCSV.getColumnIndex(curCSV.getColumnName(7)));
                    clientCSV.setCallback(callback);
                    String status = curCSV.getString(curCSV.getColumnIndex(curCSV.getColumnName(3)));
                    clientCSV.setStatus(status);
                    String manager = curCSV.getString(curCSV.getColumnIndex(curCSV.getColumnName(4)));
                    clientCSV.setManager(manager);
                    String create = curCSV.getString(curCSV.getColumnIndex(curCSV.getColumnName(5)));
                    clientCSV.setCreate(create);
                    clientCSVS.add(clientCSV);
                } while (curCSV.moveToNext());
            }
        }
        curCSV.close();
        return clientCSVS;
    }

    private List<ClientCSV> generateDataExample() {
        List<ClientCSV> clientCSVS = new ArrayList<>();
        ClientCSV clientCSV = new ClientCSV();
        String name = "Имя может содержать любые символы";
        clientCSV.setName(name);
        String phone = "Номер должен начинаться с 7, и иметь 11 символов. Если несколько номеров, разделите их запятой";
        clientCSV.setNumber(phone);
        String mail = "Формат соответствует стандартной почте. Если несколько почт, разделите их запятой ";
        clientCSV.setMail(mail);
        String comment = "Комментарий может содержать всё что угодно. Он пойдёт в историю к пользователю";
        clientCSV.setComment(comment);
        clientCSVS.add(clientCSV);

        clientCSV = new ClientCSV();
        name = "Иван Пупкин";
        clientCSV.setName(name);
        phone = "79028371942, 79210727389";
        clientCSV.setNumber(phone);
        mail = "ivanpupkin@mail.ru";
        clientCSV.setMail(mail);
        comment = "Новый пользователь";
        clientCSV.setComment(comment);
        clientCSVS.add(clientCSV);

        clientCSV = new ClientCSV();
        name = "Сотрите 2,3 и 4 строку, и начните заносить данные";
        clientCSV.setName(name);
        clientCSVS.add(clientCSV);

        return clientCSVS;
    }

    private static CellProcessor[] getProcessorsExport() {
        return new CellProcessor[]{
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional(),
        };
    }

    private static CellProcessor[] getProcessors() {
        return new CellProcessor[]{
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional(),
        };
    }

    public static final int requestcode = 42;

    public void importDB() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, requestcode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case requestcode:
                if (resultCode == RESULT_OK) {

                    Uri uri = data.getData();
                    String path = uri.getPath();
                    Log.d(TAG, "onActivityResult: " + path);
                    path = path.substring(path.indexOf(":") + 1);
                    String expansion = path.substring(path.length() - 4);
                    if (expansion.equals(".csv")) {
                        proImportCSV(path);
                    } else {
                        Toast.makeText(this, "Неверный формат файла. Расширение файла долно быть csv", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    private void proImportCSV(String from) {

        List<ClientCSV> clientCSVS = new ArrayList<>();
        ICsvBeanReader csvBeanReader = null;
        try {
            csvBeanReader = new CsvBeanReader(new InputStreamReader(
                    new FileInputStream(Environment.getExternalStorageDirectory().toString() + "/" + from), "windows-1251"),
                    CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);
        } catch (Exception e) {
        }

        // указываем как будем мапить
        String[] mapping = new String[]{"Name", "Number", "Mail", "Comment"};

        try {
            // получаем обработчики
            CellProcessor[] procs = getProcessors();
            ClientCSV clientCSV;
            // обходим весь csv файлик до конца
            while ((clientCSV = csvBeanReader.read(ClientCSV.class, mapping, procs)) != null) {
                clientCSVS.add(clientCSV);
            }
            csvBeanReader.close();

            new ClientTask().execute(clientCSVS);

            Toast.makeText(this, "Импорт завершён завершён", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.d(TAG, "proImportCSV:2 " + e);
        }
    }

    class ClientTask extends AsyncTask<List<ClientCSV>, Void, Void> {

        @Override
        protected Void doInBackground(List<ClientCSV>... lists) {
            createClientCSV(lists[0]);
            return null;
        }
    }

    void createClientCSV(List<ClientCSV> clientCSVS) {
        for (int i = 1; clientCSVS.size() > i; i++) {
            //try {
            if (clientCSVS.get(i).getName().length() > 0) {
                String name = clientCSVS.get(i).getName();
                String number = clientCSVS.get(i).getNumber();
                String mail = clientCSVS.get(i).getMail();
                String comment = clientCSVS.get(i).getComment();
                String delimetr = ",";

                Log.d(TAG, "createClientCSV: " + name + " " + number + " " + mail + " " + comment);

                String numberSQL = "";
                if (number.indexOf(',') == -1) {
                    if (HelperClass.phoneCheck(number)) {
                        numberSQL = number;
                    }
                } else {
                    String[] numbers = number.split(delimetr);
                    numbers[0] = numbers[0].replaceAll(" ", "");
                    if (HelperClass.phoneCheck(numbers[0])) {
                        numberSQL = numbers[0];
                    }
                }

                int id = 0;
                String sqlQuewy = "SELECT cc.client_id"
                        + " FROM rgzbn_gm_ceiling_clients_contacts as cc" +
                        " INNER JOIN rgzbn_gm_ceiling_clients AS c" +
                        " ON c._id = cc.client_id " +
                        " WHERE cc.phone = ? AND c.deleted_by_user <> 1";
                Cursor c = db.rawQuery(sqlQuewy, new String[]{numberSQL});
                if (c != null) {
                    if (c.moveToFirst()) {
                        id = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                    }
                }
                c.close();

                if (id == 0) {
                    //client
                    int maxIdClient = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients",
                            this, user_id);
                    String nowDate = HelperClass.nowDate();
                    ContentValues values = new ContentValues();
                    values.put(DBHelper.KEY_ID, maxIdClient);
                    values.put(DBHelper.KEY_CLIENT_NAME, name);
                    values.put(DBHelper.KEY_TYPE_ID, "1");
                    values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                    values.put(DBHelper.KEY_MANAGER_ID, user_id);
                    values.put(DBHelper.KEY_CREATED, nowDate);
                    values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                    values.put(DBHelper.KEY_API_PHONE_ID, "null");
                    values.put(DBHelper.KEY_DELETED_BY_USER, 0);
                    db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, null, values);

                    HelperClass.addExportData(
                            this,
                            maxIdClient,
                            "rgzbn_gm_ceiling_clients",
                            "send");

                    HelperClass.addHistory("Новый клиент", this, String.valueOf(maxIdClient));

                    // phone
                    if (number.indexOf(',') == -1) {
                        if (HelperClass.phoneCheck(number)) {
                            int maxIdContacts = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_contacts",
                                    this, user_id);
                            values = new ContentValues();
                            values.put(DBHelper.KEY_ID, maxIdContacts);
                            values.put(DBHelper.KEY_CLIENT_ID, maxIdClient);
                            values.put(DBHelper.KEY_PHONE, number);
                            values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, null, values);

                            HelperClass.addExportData(
                                    this,
                                    maxIdContacts,
                                    "rgzbn_gm_ceiling_clients_contacts",
                                    "send");
                        }
                    } else {
                        String[] numbers = number.split(delimetr);
                        for (int n = 0; numbers.length > n; n++) {
                            numbers[n] = numbers[n].replaceAll(" ", "");
                            if (HelperClass.phoneCheck(numbers[n])) {
                                int maxIdContacts = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_contacts",
                                        this, user_id);
                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, maxIdContacts);
                                values.put(DBHelper.KEY_CLIENT_ID, maxIdClient);
                                values.put(DBHelper.KEY_PHONE, numbers[n]);
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

                    //mail
                    if (mail.indexOf(',') == -1) {
                        if (HelperClass.validateMail(mail)) {
                            int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_dop_contacts",
                                    this, user_id);
                            values = new ContentValues();
                            values.put(DBHelper.KEY_ID, maxId);
                            values.put(DBHelper.KEY_CLIENT_ID, maxIdClient);
                            values.put(DBHelper.KEY_TYPE_ID, "1");
                            values.put(DBHelper.KEY_CONTACT, mail);
                            values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS, null, values);
                            HelperClass.addExportData(
                                    this,
                                    maxId,
                                    "rgzbn_gm_ceiling_clients_dop_contacts",
                                    "send");
                        }
                    } else {
                        String[] mails = mail.split(delimetr);
                        for (int m = 0; mails.length > m; m++) {
                            mails[m] = mails[m].replaceAll(" ", "");
                            if (HelperClass.validateMail(mails[m])) {
                                int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_dop_contacts",
                                        this, user_id);
                                values = new ContentValues();
                                values.put(DBHelper.KEY_ID, maxId);
                                values.put(DBHelper.KEY_CLIENT_ID, maxIdClient);
                                values.put(DBHelper.KEY_TYPE_ID, "1");
                                values.put(DBHelper.KEY_CONTACT, mails[m]);
                                values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
                                db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS, null, values);
                                HelperClass.addExportData(
                                        this,
                                        maxId,
                                        "rgzbn_gm_ceiling_clients_dop_contacts",
                                        "send");
                            }
                        }
                    }

                    if (comment.length() > 0) {
                        HelperClass.addHistory(comment, MainActivity.this, String.valueOf(maxIdClient));
                    }

                } else {

                    String text = "";
                    if (name != null && !name.equals("null")) {
                        text += "Имя клиента: " + name + "\n";
                    }

                    if (number != null && !number.equals("null")) {
                        text += "Номер клиента: " + number + "\n";
                    }

                    if (mail != null && !mail.equals("null")) {
                        text += "Почта клиента: " + mail + "\n";
                    }

                    if (comment != null && !comment.equals("null")) {
                        text += "Комментарий: " + comment;
                    }

                    HelperClass.addHistory(text, MainActivity.this, String.valueOf(id));
                }
            }
        }

    }

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), "Нажмите ещё раз, для того чтобы закрыть приложение",
                    Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getIntent().getStringExtra("phone") == null) {
        } else {
            navigation.setSelectedItemId(R.id.clients);
        }

        importDataReceiver = new ImportDataReceiver();
        if (importDataReceiver != null) {
            importDataReceiver.SetAlarm(this);
        }

        exportDataReceiver = new ExportDataReceiver();
        if (exportDataReceiver != null) {
            exportDataReceiver.SetAlarm(this);
        }

        bubbleCount();
    }

    public void registerReceiver() {
        callRecv = new CallReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        filter.addAction(Intent.EXTRA_PHONE_NUMBER);
        registerReceiver(callRecv, filter);

        smsBroadcaster = new SmsBroadcaster();
        filter = new IntentFilter();
        registerReceiver(smsBroadcaster, filter);

        if (VKSdk.isLoggedIn()) {
            vkReceiver = new VKReceiver(this, true);
        }

        callbackReceiver = new CallbackReceiver();
        if (callbackReceiver != null)
            callbackReceiver.SetAlarm(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_CALL_LOG,
                            Manifest.permission.INTERNET,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_SMS},
                    1);
        }

        String sqlQuewy = "SELECT _id "
                + "FROM rgzbn_gm_ceiling_clients";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    Log.d(TAG, "onStart: " + c.getString(c.getColumnIndex(c.getColumnName(0))));
                } while (c.moveToNext());
            }
        }
        c.close();


        sqlQuewy = "SELECT  date_time, text, type_id, client_id "
                + "FROM rgzbn_gm_ceiling_client_history " +
                "order by date_time";
        c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String date_time = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String text = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    int type = c.getInt(c.getColumnIndex(c.getColumnName(2)));
                    int client_id = c.getInt(c.getColumnIndex(c.getColumnName(3)));

                    if (date_time.length() == 19) {
                        date_time = date_time.substring(0, date_time.length() - 3);
                    }

                    Log.d(TAG, "historyClient: " + client_id + " " + type + " " + text + " " + date_time);
                } while (c.moveToNext());
            }
        }
        c.close();

    }

    void alertDialogPermission() {
        final Context context = MainActivity.this;
        String title = "Разрешение";
        String message = "Для корректной работы приложения, вам надо разрешить нам отображать окно приложения поверх других приложений";
        String button1String = "Разрешаю";
        String button2String = "Нет";
        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        ad.setTitle(title);  // заголовок
        ad.setMessage(message); // сообщение
        ad.setPositiveButton(button1String, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                checkPermission();
            }
        });
        ad.setNegativeButton(button2String, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {

            }
        });
        ad.setCancelable(false);
        ad.show();
    }

    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 5469;

    public void checkPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}