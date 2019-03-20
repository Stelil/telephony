package ru.itceiling.telephony.Activity;

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

import org.json.simple.JSONObject;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.constraint.UniqueHashCode;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import ru.itceiling.telephony.Broadcaster.CallReceiver;
import ru.itceiling.telephony.Broadcaster.CallbackReceiver;
import ru.itceiling.telephony.Broadcaster.ExportDataReceiver;
import ru.itceiling.telephony.Broadcaster.ImportDataReceiver;
import ru.itceiling.telephony.CSVWriter;
import ru.itceiling.telephony.ClientCSV;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.Fragments.AnalyticsFragment;
import ru.itceiling.telephony.Fragments.CallLogFragment;
import ru.itceiling.telephony.Fragments.CallbackListFragment;
import ru.itceiling.telephony.Fragments.ClientsListFragment;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;

public class MainActivity extends AppCompatActivity {

    public CallReceiver callRecv;
    public CallbackReceiver callbackReceiver;
    DBHelper dbHelper;
    SQLiteDatabase db;

    private static String dealer_id;
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
        registerCallbackReceiver();

        SharedPreferences SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

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

        //test();

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
                "WHERE manager_id = ? and substr(date_time,1,10) <= ?";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{user_id, HelperClass.nowDate().substring(0, 10)});
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
                alertDialog();
                break;

            case R.id.importDataCSV:
                importDB();
                break;
        }
        return false;
    }

    void alertDialog() {
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

    private void exportDBB(String nameFile) {

        File exportDir = new File(Environment.getExternalStorageDirectory(), "");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, nameFile + ".csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            String sqlQuery = "SELECT cl.client_name AS `Имя клиента`, " +
                    "GROUP_CONCAT(DISTINCT clc.phone) AS `Номер`, " +
                    "GROUP_CONCAT(DISTINCT cldc.contact) AS `Почта`, " +
                    "stat.title AS `Статус`, " +
                    "us.name AS `Менеджер` ," +
                    "cl.created AS `Создан` " +
                    "FROM `rgzbn_gm_ceiling_clients` AS cl " +
                    "LEFT JOIN `rgzbn_gm_ceiling_clients_contacts` AS clc " +
                    "ON clc.client_id = cl._id " +
                    "LEFT JOIN `rgzbn_gm_ceiling_clients_dop_contacts` AS cldc " +
                    "ON cldc.client_id = cl._id " +
                    "LEFT JOIN `rgzbn_gm_ceiling_clients_statuses_map` AS statm " +
                    "ON statm.client_id = cl._id " +
                    "LEFT JOIN `rgzbn_gm_ceiling_clients_statuses` AS stat " +
                    "ON stat._id = statm._id " +
                    "LEFT JOIN `rgzbn_users` AS us " +
                    "ON us._id = cl.manager_id " +
                    "WHERE cl.dealer_id = ? " +
                    "GROUP BY cl._id " +
                    "ORDER BY cl._id ";
            Cursor curCSV = db.rawQuery(sqlQuery, new String[]{dealer_id});
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to exprort
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2),
                        curCSV.getString(3), curCSV.getString(4), curCSV.getString(5)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            Toast.makeText(this, "Экспорт завершён", Toast.LENGTH_SHORT).show();
        } catch (Exception sqlEx) {
            Toast.makeText(this, "Произошла какая-та ошибка... \n" + sqlEx, Toast.LENGTH_SHORT).show();
        }
    }

    private void exportDB(String nameFile) {

        File exportDir = new File(Environment.getExternalStorageDirectory(), "");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, nameFile + ".csv");
        try {
            file.createNewFile();
            List<ClientCSV> clientCSVS = generateData();
            ICsvBeanWriter csvBeanWriter = new CsvBeanWriter(new FileWriter(file), CsvPreference.STANDARD_PREFERENCE);
            String[] header = new String[]{"name", "number", "mail", "status", "manager", "create"};
            csvBeanWriter.writeHeader(header);
            for (ClientCSV clientCSV : clientCSVS) {
                csvBeanWriter.write(clientCSV, header, getProcessors());
            }
            csvBeanWriter.close();

            Toast.makeText(this, "Экспорт завершён", Toast.LENGTH_SHORT).show();
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
                "cl.created AS `Создан` " +
                "FROM `rgzbn_gm_ceiling_clients` AS cl " +
                "LEFT JOIN `rgzbn_gm_ceiling_clients_contacts` AS clc " +
                "ON clc.client_id = cl._id " +
                "LEFT JOIN `rgzbn_gm_ceiling_clients_dop_contacts` AS cldc " +
                "ON cldc.client_id = cl._id " +
                "LEFT JOIN `rgzbn_gm_ceiling_clients_statuses_map` AS statm " +
                "ON statm.client_id = cl._id " +
                "LEFT JOIN `rgzbn_gm_ceiling_clients_statuses` AS stat " +
                "ON stat._id = statm._id " +
                "LEFT JOIN `rgzbn_users` AS us " +
                "ON us._id = cl.manager_id " +
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

    private static CellProcessor[] getProcessors() {
        return new CellProcessor[]{
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional()
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
                    Log.d(TAG, "onActivityResult: " + path);
                    proImportCSV(path);
                }
                break;
        }
    }

    private void proImportCSV(String from) {
        /*try {
            CSVReader reader = new CSVReader(new FileReader(from));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                Log.d(TAG, "proImportCSV: " + nextLine[0] + " " + nextLine[1]);
            }
        } catch (IOException e) {
            Log.d(TAG, "proImportCSV: " + e);
        }*/

        /*File myFile = new File(Environment.getExternalStorageDirectory().toString() + "/" + from);
        try {
            FileInputStream inputStream = new FileInputStream(myFile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                Log.d(TAG, "proImportCSV: " + stringBuilder);
            } catch (IOException e) {
                Log.d(TAG, "proImportCSV: IOException " + e);
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "proImportCSV: FileNotFoundException " + e);
        }*/

        List<ClientCSV> clientCSVS = new ArrayList<>();
        ICsvBeanReader csvBeanReader = null;
        try {
            csvBeanReader = new CsvBeanReader(new FileReader(
                    Environment.getExternalStorageDirectory().toString() + "/" + from),
                    CsvPreference.STANDARD_PREFERENCE);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "proImportCSV: " + e);
        }

        // указываем как будем мапить
        String[] mapping = new String[]{"name", "number", "mail", "status", "manager", "create"};

        try {
            // получаем обработчики
            CellProcessor[] procs = getProcessors();
            ClientCSV clientCSV;
            // обходим весь csv файлик до конца
            while ((clientCSV = csvBeanReader.read(ClientCSV.class, mapping, procs)) != null) {
                clientCSVS.add(clientCSV);
            }
            csvBeanReader.close();

            createClientCSV(clientCSVS);

        } catch (Exception e) {
            Log.d(TAG, "proImportCSV: " + e);
        }
    }

    static void createClientCSV(List<ClientCSV> clientCSVS){
        Log.d(TAG, "createClientCSV: " + clientCSVS.get(1).getName() + " " + clientCSVS.get(1).getNumber() + " " +
                clientCSVS.get(1).getMail() + " " + clientCSVS.get(1).getStatus() + " " + clientCSVS.get(1).getManager() + " " +
                clientCSVS.get(1).getCreate() + " ");

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
    }

    private void registerCallbackReceiver() {
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
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }


        Log.d(TAG, "onStart: ");
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