package ru.itceiling.telephony.fragments;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.internal.NavigationMenu;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.github.yavski.fabspeeddial.FabSpeedDial;
import ru.itceiling.telephony.activity.ClientActivity;
import ru.itceiling.telephony.adapter.RVAdapterClient;
import ru.itceiling.telephony.adapter.RVAdapterLabels;
import ru.itceiling.telephony.adapter.RecyclerViewClickListener;
import ru.itceiling.telephony.AdapterList;
import ru.itceiling.telephony.broadcaster.ExportDataReceiver;
import ru.itceiling.telephony.broadcaster.ImportDataReceiver;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.data.Labels;
import ru.itceiling.telephony.data.Person;
import ru.itceiling.telephony.R;
import yuku.ambilwarna.AmbilWarnaDialog;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class ClientsListFragment extends Fragment implements RecyclerViewClickListener, SearchView.OnQueryTextListener {
    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id, user_id;
    ArrayList<AdapterList> client_mas = new ArrayList<>();

    String TAG = "logd";

    String getPhone = "", textSearch = "";
    Integer add = 0;

    private View view;

    List<Person> persons;
    List<Labels> labels;
    RecyclerView recyclerView;
    RVAdapterClient adapter;
    RVAdapterLabels adapterLabels;

    int itemSelected = 0;

    public ClientsListFragment() {
        // Required empty public constructor
    }

    public static ClientsListFragment newInstance() {
        return new ClientsListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setTitle("Клиенты");
        view = inflater.inflate(R.layout.fragment_clients_list, container, false);

        SharedPreferences SP = getActivity().getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        SP = getActivity().getSharedPreferences("user_id", MODE_PRIVATE);
        user_id = SP.getString("", "");

        dbHelper = new DBHelper(getActivity());
        db = dbHelper.getWritableDatabase();


        final FabSpeedDial fabSpeedDial = (FabSpeedDial) view.findViewById(R.id.fab_speed_dial);
        fabSpeedDial.setMenuListener(new FabSpeedDial.MenuListener() {
            @Override
            public boolean onPrepareMenu(NavigationMenu navigationMenu) {
                return true;
            }

            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.add_client:
                        onButtonAddClient(view);
                        break;
                    case R.id.label_menu:
                        labelView();
                        break;
                }
                return true;
            }

            @Override
            public void onMenuClosed() {

            }
        });

        recyclerView = view.findViewById(R.id.recyclerViewClients);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(llm);
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && fabSpeedDial.isShown()) {
                    fabSpeedDial.hide();
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    fabSpeedDial.show();
                }

                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        setHasOptionsMenu(true);

        if (getActivity().getIntent().getStringExtra("phone") == null) {
        } else {
            getPhone = getActivity().getIntent().getStringExtra("phone");
        }

        if (getActivity().getIntent().getStringExtra("add") == null ||
                getActivity().getIntent().getStringExtra("add").equals("0")) {
        } else {
            add = 1;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Добавление")
                    .setMessage("Выберите контакт для привязки номера")
                    .setCancelable(false)
                    .setNegativeButton("ОК",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        }

        if (!getPhone.equals("") && add == 0) {
            View view = null;
            onButtonAddClient(view);
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);

        super.onCreateOptionsMenu(menu, inflater);
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

    @Override
    public void onResume() {
        super.onResume();

        MyTask mt = new MyTask();
        mt.execute();

        ExportDataReceiver exportDataReceiver = new ExportDataReceiver();
        Intent intent = new Intent(getActivity(), ExportDataReceiver.class);
        exportDataReceiver.onReceive(getActivity(), intent);

        ImportDataReceiver importDataReceiver = new ImportDataReceiver();
        intent = new Intent(getActivity(), ImportDataReceiver.class);
        importDataReceiver.onReceive(getActivity(), intent);

        String ass_client = HelperClass.associated_client(getActivity(), user_id);
        int count = 0;
        String sqlQuewy = "SELECT count(_id) "
                + "FROM rgzbn_gm_ceiling_clients " +
                "WHERE dealer_id = ? and deleted_by_user <> 1 and _id <> ?";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{dealer_id, ass_client});
        if (c != null) {
            if (c.moveToFirst()) {
                count = c.getInt(c.getColumnIndex(c.getColumnName(0)));
            }
        }
        c.close();

        getActivity().setTitle("Клиенты(" + String.valueOf(count) + ")");
    }

    class MyTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("Отображаем...");
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

                        String name = nameClient.getText().toString();
                        String phone = phoneClient.getText().toString();

                        if (name.length() > 0) {
                            int maxIdClient = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients",
                                    getActivity(), user_id);
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
                                    context,
                                    maxIdClient,
                                    "rgzbn_gm_ceiling_clients",
                                    "send");

                            int idOldClient = 0;
                            String sqlQuewy = "SELECT c.client_id " +
                                    "FROM rgzbn_gm_ceiling_clients_contacts AS c " +
                                    "WHERE c.phone = ?";
                            Cursor cc = db.rawQuery(sqlQuewy, new String[]{phone});
                            if (cc != null) {
                                if (cc.moveToLast()) {
                                    idOldClient = cc.getInt(cc.getColumnIndex(cc.getColumnName(0)));

                                    values = new ContentValues();
                                    values.put(DBHelper.KEY_CLIENT_ID, maxIdClient);
                                    db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, values, "client_id = ? ",
                                            new String[]{String.valueOf(idOldClient)});

                                    int idCallsStatusHistory = 0;
                                    sqlQuewy = "SELECT _id "
                                            + "   FROM rgzbn_gm_ceiling_calls_status_history" +
                                            "    WHERE client_id = ? ";
                                    cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(maxIdClient)});
                                    if (cc != null) {
                                        if (cc.moveToLast()) {
                                            idCallsStatusHistory = cc.getInt(cc.getColumnIndex(cc.getColumnName(0)));
                                        }
                                    }
                                    cc.close();

                                    HelperClass.addExportData(
                                            context,
                                            idCallsStatusHistory,
                                            "rgzbn_gm_ceiling_calls_status_history",
                                            "send");

                                    values = new ContentValues();
                                    values.put(DBHelper.KEY_CLIENT_ID, maxIdClient);
                                    db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, values, "client_id = ? ",
                                            new String[]{String.valueOf(idOldClient)});

                                    int idClientHistory = 0;
                                    sqlQuewy = "SELECT _id "
                                            + "   FROM rgzbn_gm_ceiling_client_history" +
                                            "    WHERE client_id = ? ";
                                    cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(maxIdClient)});
                                    if (cc != null) {
                                        if (cc.moveToLast()) {
                                            idClientHistory = cc.getInt(cc.getColumnIndex(cc.getColumnName(0)));
                                        }
                                    }
                                    cc.close();

                                    HelperClass.addExportData(
                                            context,
                                            idClientHistory,
                                            "rgzbn_gm_ceiling_client_history",
                                            "send");

                                    db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS,
                                            "_id = ?",
                                            new String[]{String.valueOf(idOldClient)});

                                    db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS,
                                            "client_id = ?",
                                            new String[]{String.valueOf(idOldClient)});
                                }
                            }
                            cc.close();

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
                                values.put(DBHelper.KEY_PHONE, HelperClass.phoneEdit(phone));
                                values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                                db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, null, values);

                                HelperClass.addExportData(
                                        context,
                                        maxIdContacts,
                                        "rgzbn_gm_ceiling_clients_contacts",
                                        "send");
                            }

                            String nameManager = "-";
                            sqlQuewy = "SELECT name "
                                    + "   FROM rgzbn_users" +
                                    "    WHERE _id = ? " +
                                    "order by _id";
                            cc = db.rawQuery(sqlQuewy, new String[]{user_id});
                            if (cc != null) {
                                if (cc.moveToLast()) {
                                    nameManager = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                                }
                            }
                            cc.close();

                            Log.d(TAG, "phone: " + phone);

                            persons.add(0, new Person(name, phone, nameManager, "#000000",
                                    " ", "Необработанный", Integer.valueOf(maxIdClient)));
                            adapter.notifyItemInserted(0);
                            ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(0, 0);

                            getActivity().getIntent().removeExtra("phone");

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

    private String currentColor = "000";

    public void labelView() {

        final Context context = getActivity();
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.dialog_lable_view, null);
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
        mDialogBuilder.setView(promptsView);
        final EditText nameLabel = (EditText) promptsView.findViewById(R.id.nameLabel);
        final Button selectColor = (Button) promptsView.findViewById(R.id.selectColor);
        final Button addLabel = (Button) promptsView.findViewById(R.id.addLabel);

        final RecyclerView linear_color = promptsView.findViewById(R.id.linear_color);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        linear_color.setLayoutManager(llm);
        linear_color.setHasFixedSize(true);

        final boolean[] bool = {false};
        selectColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AmbilWarnaDialog dialog = new AmbilWarnaDialog(getActivity(),
                        Integer.parseInt(currentColor.substring(1), 16), false,
                        new AmbilWarnaDialog.OnAmbilWarnaListener() {
                            @Override
                            public void onOk(AmbilWarnaDialog dialog, int color) {
                                String hexColor = String.format("#%06X", (0xFFFFFF & color));
                                selectColor.setBackgroundColor(Color.parseColor(hexColor));
                                currentColor = hexColor;
                                bool[0] = true;
                            }

                            @Override
                            public void onCancel(AmbilWarnaDialog dialog) {

                            }
                        });
                dialog.show();
            }
        });

        addLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (nameLabel.getText().toString().length() > 0 && bool[0]) {
                    String title = nameLabel.getText().toString();
                    int maxId = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_labels",
                            getActivity(), user_id);
                    String nowDate = HelperClass.nowDate();

                    ContentValues values = new ContentValues();
                    values.put(DBHelper.KEY_ID, maxId);
                    values.put(DBHelper.KEY_TITLE, title);
                    values.put(DBHelper.KEY_COLOR_CODE, currentColor.substring(1));
                    values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                    values.put(DBHelper.KEY_CHANGE_TIME, nowDate);
                    db.insert(DBHelper.TABLE_RGZBN_CEILING_CLIENTS_LABELS, null, values);

                    HelperClass.addExportData(
                            context,
                            maxId,
                            "rgzbn_gm_ceiling_clients_labels",
                            "send");

                    Toast.makeText(context, "Ярлык добавлен", Toast.LENGTH_SHORT).show();
                    nameLabel.setText("");

                    viewLabels(linear_color);
                } else {
                    Toast.makeText(context, "Вы что-то не заполнили", Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewLabels(linear_color);

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(promptsView)
                .setTitle("Ярлыки")
                .setPositiveButton(android.R.string.ok, null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

            }
        });
        dialog.show();
    }

    private void viewLabels(RecyclerView recyclerView) {
        labels.clear();
        String sqlQuewy = "SELECT title, color_code, _id "
                + "   FROM rgzbn_gm_ceiling_clients_labels" +
                "    WHERE dealer_id = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    try {
                        String title = c.getString(c.getColumnIndex(c.getColumnName(0)));
                        String color_code = c.getString(c.getColumnIndex(c.getColumnName(1)));
                        int id = c.getInt(c.getColumnIndex(c.getColumnName(2)));

                        int parsedColor = Color.parseColor("#" + color_code);

                        labels.add(new Labels(id, title, parsedColor));

                    } catch (Exception e) {
                        Log.d(TAG, "labelView: " + e);
                    }
                } while (c.moveToNext());
            }
        }
        c.close();

        adapterLabels = new RVAdapterLabels(labels, this);
        recyclerView.setAdapter(adapterLabels);
    }

    private void ListClients(String query) {

        client_mas.clear();

        persons = new ArrayList<>();
        labels = new ArrayList<>();

        String sqlQuewy;
        Cursor c;

        String associated_client = HelperClass.associated_client(getActivity(), user_id);

        if (associated_client == null) {
            associated_client = "";
        }

        if (query.equals("")) {
            sqlQuewy = "SELECT cl._id, cl.client_name, " +
                    "s.title, u.name, c.phone, cl.label_id " +
                    "FROM rgzbn_gm_ceiling_clients AS cl " +
                    "LEFT JOIN rgzbn_gm_ceiling_clients_statuses_map AS sm " +
                    "ON cl._id = sm.client_id " +
                    "LEFT JOIN rgzbn_gm_ceiling_clients_statuses AS s " +
                    "ON s._id = sm.status_id " +
                    "LEFT JOIN rgzbn_gm_ceiling_clients_contacts AS c " +
                    "ON cl._id = c.client_id " +
                    "INNER JOIN rgzbn_users AS u " +
                    "ON cl.manager_id = u._id " +
                    "WHERE cl.dealer_id = ? " +
                    "AND cl._id <> ? " +
                    "AND cl.deleted_by_user <> 1 " +
                    "GROUP BY cl._id " +
                    "ORDER BY cl.created DESC";
        } else {
            sqlQuewy = "SELECT cl._id, cl.client_name, " +
                    "s.title, u.name, c.phone, cl.label_id " +
                    "FROM rgzbn_gm_ceiling_clients AS cl " +
                    "LEFT JOIN rgzbn_gm_ceiling_clients_statuses_map AS sm " +
                    "ON cl._id = sm.client_id " +
                    "LEFT JOIN rgzbn_gm_ceiling_clients_statuses AS s " +
                    "ON s._id = sm.status_id " +
                    "LEFT JOIN rgzbn_gm_ceiling_clients_contacts AS c " +
                    "ON cl._id = c.client_id " +
                    "INNER JOIN rgzbn_users AS u " +
                    "ON cl.manager_id = u._id " +
                    "WHERE cl.dealer_id = ? " +
                    "AND cl._id <> ? " +
                    "AND cl.deleted_by_user <> 1 " +
                    "and cl.client_name like '%" + query + "%' " +
                    "or c.phone like '%" + query + "%' " +
                    "or s.title like '%" + query + "%' " +
                    "GROUP BY cl._id " +
                    "ORDER BY cl.created DESC";
        }
        c = db.rawQuery(sqlQuewy, new String[]{dealer_id, associated_client});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    String id_client = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    String client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                    String title = "-";
                    if (c.getString(c.getColumnIndex(c.getColumnName(2))) != null) {
                        title = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    }

                    String nameManager = "-";
                    if (c.getString(c.getColumnIndex(c.getColumnName(3))) != null) {
                        nameManager = c.getString(c.getColumnIndex(c.getColumnName(3)));
                    }

                    String phone = "";
                    if (c.getString(c.getColumnIndex(c.getColumnName(4))) != null) {
                        phone = c.getString(c.getColumnIndex(c.getColumnName(4)));
                    }

                    String label_id = "";
                    String label_code = "ffffff";
                    if (c.getString(c.getColumnIndex(c.getColumnName(5))) != null) {
                        label_id = c.getString(c.getColumnIndex(c.getColumnName(5)));

                        sqlQuewy = "SELECT color_code "
                                + "FROM rgzbn_gm_ceiling_clients_labels" +
                                " WHERE _id = ? ";
                        Cursor cc = db.rawQuery(sqlQuewy, new String[]{label_id});
                        if (cc != null) {
                            if (cc.moveToFirst()) {
                                label_code = cc.getString(cc.getColumnIndex(cc.getColumnName(0)));
                            }
                        }
                        cc.close();
                    }

                    persons.add(new Person(client_name, phone, nameManager, "#000000",
                            label_code, title, Integer.valueOf(id_client)));
                } while (c.moveToNext());
            }
        }
        c.close();

        adapter = new RVAdapterClient(persons, this);
        try {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recyclerView.setAdapter(adapter);

                    if (itemSelected == 0) {
                        recyclerView.scrollToPosition(itemSelected);
                    } else {
                        recyclerView.scrollToPosition(itemSelected - 1);
                    }
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "ListClients error: " + e);
        }

    }

    @Override
    public void recyclerViewListClicked(View v, int pos) {
        int clickedDataItem = persons.get(pos).getId();
        if (add == 1) {
            addPhone(clickedDataItem);
        }
        itemSelected = pos;
        Intent intent = new Intent(getActivity(), ClientActivity.class);
        intent.putExtra("id_client", String.valueOf(clickedDataItem));
        intent.putExtra("check", "false");
        startActivity(intent);
    }

    void addPhone(int id) {

        int idOldClient = 0;
        String sqlQuewy = "SELECT h.client_id " +
                "FROM rgzbn_gm_ceiling_clients_contacts AS c " +
                "INNER JOIN rgzbn_gm_ceiling_calls_status_history AS h " +
                "ON c.client_id = h.client_id " +
                "WHERE c.phone = ?";
        Cursor cc = db.rawQuery(sqlQuewy, new String[]{getPhone});
        if (cc != null) {
            if (cc.moveToLast()) {
                idOldClient = cc.getInt(cc.getColumnIndex(cc.getColumnName(0)));

                db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS,
                        "_id = ?",
                        new String[]{String.valueOf(idOldClient)});

                db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS,
                        "client_id = ?",
                        new String[]{String.valueOf(idOldClient)});

                ContentValues values = new ContentValues();
                values.put(DBHelper.KEY_CLIENT_ID, id);
                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, values, "client_id = ? ",
                        new String[]{String.valueOf(idOldClient)});

                int idCallsStatusHistory = 0;
                sqlQuewy = "SELECT _id "
                        + "   FROM rgzbn_gm_ceiling_calls_status_history" +
                        "    WHERE client_id = ? ";
                cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id)});
                if (cc != null) {
                    if (cc.moveToLast()) {
                        idCallsStatusHistory = cc.getInt(cc.getColumnIndex(cc.getColumnName(0)));
                    }
                }
                cc.close();

                HelperClass.addExportData(
                        getActivity(),
                        idCallsStatusHistory,
                        "rgzbn_gm_ceiling_calls_status_history",
                        "send");

                values = new ContentValues();
                values.put(DBHelper.KEY_CLIENT_ID, id);
                db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, values, "client_id = ? ",
                        new String[]{String.valueOf(idOldClient)});

                int idClientHistory = 0;
                sqlQuewy = "SELECT _id "
                        + "   FROM rgzbn_gm_ceiling_client_history" +
                        "    WHERE client_id = ? ";
                cc = db.rawQuery(sqlQuewy, new String[]{String.valueOf(id)});
                if (cc != null) {
                    if (cc.moveToLast()) {
                        idClientHistory = cc.getInt(cc.getColumnIndex(cc.getColumnName(0)));
                    }
                }
                cc.close();

                HelperClass.addExportData(
                        getActivity(),
                        idClientHistory,
                        "rgzbn_gm_ceiling_client_history",
                        "send");

                db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS,
                        "_id = ?",
                        new String[]{String.valueOf(idOldClient)});

                db.delete(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS,
                        "client_id = ?",
                        new String[]{String.valueOf(idOldClient)});
            }
        }
        cc.close();

        Integer lastIdTable = HelperClass.lastIdTable("rgzbn_gm_ceiling_clients_contacts", getActivity(), user_id);
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID, lastIdTable);
        values.put(DBHelper.KEY_PHONE, getPhone);
        values.put(DBHelper.KEY_CLIENT_ID, id);
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, null, values);

        HelperClass.addExportData(
                getActivity(),
                Integer.valueOf(lastIdTable),
                "rgzbn_gm_ceiling_clients_contacts",
                "send");
    }

    @Override
    public void recyclerViewListLongClicked(View v, int id, int pos) {

    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop: ");
        super.onStop();
        getActivity().getIntent().removeExtra("add");
        getActivity().getIntent().removeExtra("phone");
        add = 0;
        getPhone = "";
    }
}