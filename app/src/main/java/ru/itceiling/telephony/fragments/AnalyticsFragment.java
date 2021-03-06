package ru.itceiling.telephony.fragments;


import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.echo.holographlibrary.PieGraph;
import com.echo.holographlibrary.PieSlice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.itceiling.telephony.activity.ClientActivity;
import ru.itceiling.telephony.adapter.RVAdapterClient;
import ru.itceiling.telephony.adapter.RecyclerViewClickListener;
import ru.itceiling.telephony.data.AdapterList;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.data.Person;
import ru.itceiling.telephony.R;
import ru.itceiling.telephony.UnderlineTextView;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class AnalyticsFragment extends Fragment implements RecyclerViewClickListener {

    TableLayout analyticsTable, titleTable;
    DBHelper dbHelper;
    SQLiteDatabase db;
    String dealer_id, analyticDate, user_id;
    private List<TextView> txtList = new ArrayList<>();
    private String[] arrayId;
    TextView txtSelectDay, txtSelectDayTwo;
    Calendar dateAndTime = new GregorianCalendar();
    ArrayList<AdapterList> client_mas = new ArrayList<>();
    String TAG = "logd", domen = "", dataManager;
    LinearLayout linearScrollView, linearManagerTable;
    View view;

    List<Person> persons;
    RecyclerView recyclerView;
    RVAdapterClient adapter;

    static RequestQueue requestQueue;

    private List listManager;
    private List<String> listManagerClients;
    int listManagerClientsStep = 0,
            stepTxt = 0,
            countStatuses = 0;

    Map<String, String> parameters = new HashMap<String, String>();

    public AnalyticsFragment() {
        // Required empty public constructor
    }

    public static AnalyticsFragment newInstance() {
        return new AnalyticsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setTitle("Аналитика");

        view = inflater.inflate(R.layout.fragment_analytics, container, false);

        analyticsTable = view.findViewById(R.id.analyticsTable);
        titleTable = view.findViewById(R.id.titleTable);

        txtSelectDay = view.findViewById(R.id.txtSelectDay);
        txtSelectDayTwo = view.findViewById(R.id.txtSelectDayTwo);

        dbHelper = new DBHelper(getActivity());
        db = dbHelper.getReadableDatabase();

        SharedPreferences SP = getActivity().getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        SP = getActivity().getSharedPreferences("user_id", MODE_PRIVATE);
        user_id = SP.getString("", "");

        linearScrollView = view.findViewById(R.id.linearScrollView);
        linearManagerTable = view.findViewById(R.id.linearManagerTable);

        final TextView txtSelectDay = view.findViewById(R.id.txtSelectDay);
        txtSelectDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDate(txtSelectDay);
            }
        });

        final TextView txtSelectDayTwo = view.findViewById(R.id.txtSelectDayTwo);
        txtSelectDayTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDateTwo(txtSelectDayTwo);
            }
        });

        final ImageButton btnClearDay = view.findViewById(R.id.btnClearDay);
        btnClearDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtSelectDay.setText("");
                txtSelectDayTwo.setText("");
                createTable();
                createTableForManager();
            }
        });

        listManagerClients = new ArrayList();

        createTitleTable();
        createTable();

        createTableForManager();

        return view;
    }

    private void createTitleTable() {

        TextView txtForHorizontalLength = view.findViewById(R.id.txtForHorizontalLength);

        int countApi = 0;
        String sqlQuewy = "select count(_id) "
                + "FROM rgzbn_gm_ceiling_clients_statuses ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    countApi = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        txtForHorizontalLength.setText("");

        String[] arrayStatus = new String[countApi];
        int index = 0;
        sqlQuewy = "select title "
                + "FROM rgzbn_gm_ceiling_clients_statuses ";
        c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    arrayStatus[index] = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    index++;
                } while (c.moveToNext());
            }
        }
        c.close();

        TableRow tableRow = new TableRow(getActivity());
        TableRow.LayoutParams tableParams = new TableRow.LayoutParams(100,
                TableRow.LayoutParams.MATCH_PARENT, 4f);

        int length = 0;
        for (int j = 0; j < countApi + 1; j++) {
            TextView txt = new TextView(getActivity());
            txt.setLayoutParams(tableParams);
            if (j == 0) {
                txt.setText("Всего");
            } else {
                String title = arrayStatus[j - 1];
                txt.setText(title);
                length += title.length();
            }
            txt.setTextColor(Color.parseColor("#414099"));
            txt.setTextSize(13);
            txt.setGravity(Gravity.CENTER);
            tableRow.addView(txt);
        }

        titleTable.addView(tableRow);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics metricsB = new DisplayMetrics();
        display.getMetrics(metricsB);

        ViewGroup.LayoutParams lp = txtForHorizontalLength.getLayoutParams();
        lp.width = length * 20;

        if (metricsB.widthPixels > lp.width) {
            lp.width = metricsB.widthPixels;
        }

        titleTable.setLayoutParams(lp);
        titleTable.setGravity(Gravity.CENTER);
    }

    private void createTable() {

        analyticsTable.removeAllViews();
        txtList.clear();

        String sqlQuewy = "select count(_id) "
                + "FROM rgzbn_gm_ceiling_clients_statuses ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    countStatuses = c.getInt(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        int[] arrayStatusCount = new int[countStatuses];
        arrayId = new String[countStatuses];

        int countClients = 0;
        int index = 0;

        String date1 = "0001-01-01",
                date2 = HelperClass.nowDate().substring(0, 10);
        if (!txtSelectDay.getText().toString().equals("")) {
            date1 = txtSelectDay.getText().toString();
        }
        if (!txtSelectDayTwo.getText().toString().equals("")) {
            date2 = txtSelectDayTwo.getText().toString();
        }

        sqlQuewy = "SELECT s._id AS status_id, " +
                "COUNT(ls.max_id) AS count, " +
                "GROUP_CONCAT(ls.client_id) AS clients " +
                "FROM rgzbn_gm_ceiling_clients_statuses AS s " +
                "LEFT JOIN rgzbn_gm_ceiling_clients_statuses_map AS sm " +
                "ON s._id = sm.status_id " +
                "LEFT JOIN (SELECT MAX(_id) AS max_id, client_id " +
                "FROM rgzbn_gm_ceiling_clients_statuses_map " +
                "GROUP BY client_id " +
                ") AS ls " +
                "ON sm._id = ls.max_id " +
                "AND sm.change_time between ? and ? " +
                "WHERE (s.dealer_id = ? " +
                "OR s.dealer_id = ?) " +
                "GROUP BY s._id " +
                "ORDER BY s._id ";
        c = db.rawQuery(sqlQuewy, new String[]{date1 + " 00:00:00", date2 + " 23:59:59", dealer_id, "null"});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    countClients += c.getInt(c.getColumnIndex(c.getColumnName(1)));
                    if (c.getString(c.getColumnIndex(c.getColumnName(1))).equals("0")) {
                        arrayStatusCount[index] = 0;
                        arrayId[index] = "0";
                    } else {
                        arrayStatusCount[index] = c.getInt(c.getColumnIndex(c.getColumnName(1)));
                        arrayId[index] = c.getString(c.getColumnIndex(c.getColumnName(2)));
                    }
                    index++;
                } while (c.moveToNext());
            }
        }
        c.close();

        TableRow tableRow = new TableRow(getActivity());
        TableRow.LayoutParams tableParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT, 4f);

        for (int j = 0; j < index + 1; j++) {

            UnderlineTextView txt = new UnderlineTextView(getActivity());
            txt.setLayoutParams(tableParams);

            if (j == 0) {
                txt.setText(String.valueOf(countClients));
            } else {
                txt.setText(String.valueOf(arrayStatusCount[j - 1]));
            }

            txt.setTextColor(Color.parseColor("#414099"));
            txt.setTextSize(17);
            txt.setGravity(Gravity.CENTER);
            txt.setId(j);
            txt.setOnClickListener(onClickTxt);
            txtList.add(txt);
            tableRow.addView(txt);

            stepTxt = j + 1;
        }

        analyticsTable.addView(tableRow);

    }

    View.OnClickListener onClickTxt = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            final TextView textView = txtList.get(id);
            int text = Integer.parseInt(textView.getText().toString());

            final Context context = getActivity();
            LayoutInflater li = LayoutInflater.from(context);
            View promptsView = li.inflate(R.layout.fragment_clients_list, null);
            AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
            mDialogBuilder.setView(promptsView);

            final AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(promptsView)
                    .setNegativeButton("Скрыть", null)
                    .create();

            FloatingActionButton addClient = promptsView.findViewById(R.id.fab);
            addClient.setVisibility(View.GONE);

            if (text == 0) {
            } else if (id == 0) {
                dialog.show();
                recyclerView = promptsView.findViewById(R.id.recyclerViewClients);
                LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                recyclerView.setLayoutManager(llm);
                recyclerView.setHasFixedSize(true);
                ListClients(null, recyclerView);
            } else {
                dialog.show();
                recyclerView = promptsView.findViewById(R.id.recyclerViewClients);
                LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                recyclerView.setLayoutManager(llm);
                recyclerView.setHasFixedSize(true);
                ListClients((id - 1), recyclerView);
            }
        }
    };

    private void ListClients(Integer id, RecyclerView listView) {
        client_mas.clear();
        persons = new ArrayList<>();

        String[] ar;
        if (id == null) {
            ar = new String[countStatuses];
            System.arraycopy(arrayId, 0, ar, 0, countStatuses);
        } else {
            ar = new String[1];
            ar[0] = arrayId[id];
        }

        if (id == null || countStatuses < id) {

            String array_clients = "(";
            for (int i = 0; ar.length > i; i++) {
                Log.d(TAG, "ListClients:1 " + ar[i]);
                if (!array_clients.contains(ar[i])) {
                    array_clients += ar[i] + ", ";
                }
            }
            array_clients = array_clients.substring(0, array_clients.length() - 2);
            array_clients += ")";

            String sqlQuewy = "SELECT DISTINCT (c._id)," +
                    "c.client_name," +
                    "c.manager_id," +
                    "cs.title, " +
                    "cc.phone, " +
                    "us.name " +
                    "FROM rgzbn_gm_ceiling_clients as c " +
                    "left join rgzbn_gm_ceiling_clients_statuses_map as csm " +
                    "on csm.client_id = c._id " +
                    "left join rgzbn_gm_ceiling_clients_statuses as cs " +
                    "on cs._id = csm.status_id " +
                    "inner join rgzbn_gm_ceiling_clients_contacts as cc " +
                    "on c._id = cc.client_id " +
                    "inner join rgzbn_users as us " +
                    "on us._id = c.manager_id " +
                    "WHERE c._id in " + array_clients +
                    " GROUP BY c._id";

            Log.d(TAG, "ListClients:1 " + sqlQuewy);

            Cursor c = db.rawQuery(sqlQuewy, new String[]{});
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        String id_client = c.getString(c.getColumnIndex(c.getColumnName(0)));
                        String client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                        String title = c.getString(c.getColumnIndex(c.getColumnName(3)));
                        String phone = c.getString(c.getColumnIndex(c.getColumnName(4)));
                        String nameManager = c.getString(c.getColumnIndex(c.getColumnName(5)));

                        persons.add(new Person(client_name, phone, nameManager, "#000000",
                                "Холодный", title, Integer.valueOf(id_client), "0"));
                    } while (c.moveToNext());
                }
            }
            c.close();
        } else {

            String array_clients = "(";
            for (int i = 0; ar.length > i; i++) {
                Log.d(TAG, "ListClients:2 " + ar[i]);
                if (!array_clients.contains(ar[i])) {
                    array_clients += ar[i] + ", ";
                }
            }
            array_clients = array_clients.substring(0, array_clients.length() - 2);
            array_clients += ")";

            String sqlQuewy = "SELECT DISTINCT (c._id)," +
                    "c.client_name," +
                    "c.manager_id," +
                    "cs.title, " +
                    "cc.phone, " +
                    "us.name " +
                    "FROM rgzbn_gm_ceiling_clients as c " +
                    "left join rgzbn_gm_ceiling_clients_statuses_map as csm " +
                    "on csm.client_id = c._id " +
                    "left join rgzbn_gm_ceiling_clients_statuses as cs " +
                    "on cs._id = csm.status_id " +
                    "inner join rgzbn_gm_ceiling_clients_contacts as cc " +
                    "on c._id = cc.client_id " +
                    "inner join rgzbn_users as us " +
                    "on us._id = c.manager_id " +
                    "WHERE c._id in " + array_clients +
                    " GROUP BY c._id";

            Log.d(TAG, "ListClients:2 " + sqlQuewy);

            Cursor c = db.rawQuery(sqlQuewy, new String[]{});
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        String id_client = c.getString(c.getColumnIndex(c.getColumnName(0)));
                        String client_name = c.getString(c.getColumnIndex(c.getColumnName(1)));
                        String title = c.getString(c.getColumnIndex(c.getColumnName(3)));
                        String phone = c.getString(c.getColumnIndex(c.getColumnName(4)));
                        String nameManager = c.getString(c.getColumnIndex(c.getColumnName(5)));

                        persons.add(new Person(client_name, phone, nameManager, "#000000",
                                "Холодный", title, Integer.valueOf(id_client), "0"));
                    } while (c.moveToNext());

                }
            }
            c.close();
        }

        adapter = new RVAdapterClient(persons, this);
        recyclerView.setAdapter(adapter);

    }

    @Override
    public void recyclerViewListClicked(View v, int id) {
        Intent intent = new Intent(getActivity(), ClientActivity.class);
        intent.putExtra("id_client", " " + id);
        intent.putExtra("check", "false");
        startActivity(intent);
    }

    @Override
    public void recyclerViewListLongClicked(View v, int id, int pos) {

    }

    void createTableForManager() {

        linearManagerTable.removeAllViews();
        listManagerClients.clear();

        SharedPreferences SP = getActivity().getSharedPreferences("link", MODE_PRIVATE);
        domen = SP.getString("", "");

        requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        listManager = new ArrayList();

        String sqlQuewy = "select _id " +
                "from rgzbn_users " +
                "where dealer_id = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{user_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    listManager.add(c.getInt(c.getColumnIndex(c.getColumnName(0))));
                } while (c.moveToNext());
            }
        }
        c.close();

        if (HelperClass.isOnline(getActivity())) {
            listManagerClientsStep = 0;
            String date1 = "0001-01-01",
                    date2 = HelperClass.nowDate().substring(0, 10);
            if (!txtSelectDay.getText().toString().equals("")) {
                date1 = txtSelectDay.getText().toString();
            }
            if (!txtSelectDayTwo.getText().toString().equals("")) {
                date2 = txtSelectDayTwo.getText().toString();
            }

            final JSONObject jsonObj = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            try {
                jsonObj.put("date1", date1);
                jsonObj.put("date2", date2);

                for (int i = 0; listManager.size() > i; i++) {
                    jsonArray.put(listManager.get(i));
                }

                jsonObj.put("managers", jsonArray);
            } catch (Exception e) {
            }

            dataManager = String.valueOf(jsonObj);
            Log.d(TAG, "createTableForManager: " + dataManager);
            parameters.put("data", HelperClass.encrypt(jsonObj.toString(), getActivity()));
            new GetManagersAnalytic().execute();
        } else {
            try {
                repeatManager();
            } catch (Exception e) {
                Log.d(TAG, "createTableForManager error: " + e);
            }
        }

    }

    void repeatManager() {
        if (listManager.size() > 0) {
            TextView titleManager = new TextView(getContext());
            TableRow.LayoutParams textParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT, 0);
            textParams.setMargins(50, 0, 0, 0);
            titleManager.setLayoutParams(textParams);
            titleManager.setText("Менеджеры");
            titleManager.setTextSize(25);
            titleManager.setTextColor(Color.parseColor("#414099"));
            linearManagerTable.addView(titleManager);
        }

        for (int i = 0; listManager.size() > i; i++) {
            createTitleManager((Integer) listManager.get(i));
        }

    }

    private void createTitleManager(Integer managerId) {
        String name = "";
        String sqlQuewy = "select name " +
                "from rgzbn_users " +
                "where _id = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(managerId)});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    name = c.getString(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        TextView textNameManager = new TextView(getActivity());
        TableRow.LayoutParams textParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT, 0);
        textParams.setMargins(25, 50, 0, 0);
        textNameManager.setLayoutParams(textParams);
        textNameManager.setText(name);
        textNameManager.setTextColor(Color.parseColor("#414099"));
        linearManagerTable.addView(textNameManager);

        createPieGraph(managerId);

        View view = new View(getActivity());
        view.setBackgroundColor(Color.parseColor("#000000"));
        TableRow.LayoutParams tableParamsView = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                5, 0);
        view.setLayoutParams(tableParamsView);
        linearManagerTable.addView(view);

        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        TableLayout analyticsTableLayout = new TableLayout(getActivity());
        TableRow.LayoutParams tableParamsAnalyticsTable = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT, 4f);
        analyticsTableLayout.setLayoutParams(tableParamsAnalyticsTable);

        linearLayout.addView(analyticsTableLayout);

        linearManagerTable.addView(linearLayout);

        TableRow tableRow = new TableRow(getActivity());
        TableRow.LayoutParams tableParams = new TableRow.LayoutParams(100,
                TableRow.LayoutParams.MATCH_PARENT, 4f);

        TextView textTitle = new TextView(getActivity());
        tableParams.setMargins(25, 0, 0, 0);
        textTitle.setLayoutParams(tableParams);
        textTitle.setText("Всего");
        textTitle.setGravity(Gravity.CENTER);
        textTitle.setTextColor(Color.parseColor("#414099"));
        tableRow.addView(textTitle);

        textTitle = new TextView(getActivity());
        tableParams.setMargins(25, 0, 0, 0);
        textTitle.setLayoutParams(tableParams);
        textTitle.setText("Недозвон");
        textTitle.setGravity(Gravity.CENTER);
        textTitle.setTextColor(Color.parseColor("#414099"));
        tableRow.addView(textTitle);

        textTitle = new TextView(getActivity());
        tableParams.setMargins(25, 0, 0, 0);
        textTitle.setLayoutParams(tableParams);
        textTitle.setText("Входящие");
        textTitle.setGravity(Gravity.CENTER);
        textTitle.setTextColor(Color.parseColor("#414099"));
        tableRow.addView(textTitle);

        textTitle = new TextView(getActivity());
        tableParams.setMargins(25, 0, 0, 0);
        textTitle.setLayoutParams(tableParams);
        textTitle.setText("Исходящие");
        textTitle.setGravity(Gravity.CENTER);
        textTitle.setTextColor(Color.parseColor("#414099"));
        tableRow.addView(textTitle);

        if (listManagerClients.size() != 0 && HelperClass.isOnline(getActivity())) {
            textTitle = new TextView(getActivity());
            tableParams.setMargins(25, 0, 0, 0);
            textTitle.setLayoutParams(tableParams);
            textTitle.setText("Замеры");
            textTitle.setGravity(Gravity.CENTER);
            textTitle.setTextColor(Color.parseColor("#414099"));
            tableRow.addView(textTitle);

            textTitle = new TextView(getActivity());
            tableParams.setMargins(25, 0, 0, 0);
            textTitle.setLayoutParams(tableParams);
            textTitle.setText("Договоры");
            textTitle.setGravity(Gravity.CENTER);
            textTitle.setTextColor(Color.parseColor("#414099"));
            tableRow.addView(textTitle);

            textTitle = new TextView(getActivity());
            tableParams.setMargins(25, 0, 0, 0);
            textTitle.setLayoutParams(tableParams);
            textTitle.setText("Сумма");
            textTitle.setGravity(Gravity.CENTER);
            textTitle.setTextColor(Color.parseColor("#414099"));
            tableRow.addView(textTitle);
        }

        analyticsTableLayout.addView(tableRow);
        analyticsTableLayout.setGravity(Gravity.CENTER);

        view = new View(getActivity());
        view.setBackgroundColor(Color.parseColor("#000000"));
        tableParamsView = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                3, 0);
        view.setLayoutParams(tableParamsView);
        linearManagerTable.addView(view);

        createTableManager(managerId);
    }

    void createPieGraph(Integer managerId) {
        String date1 = "0001-01-01",
                date2 = HelperClass.nowDate().substring(0, 10);
        if (!txtSelectDay.getText().toString().equals("")) {
            date1 = txtSelectDay.getText().toString();
        }
        if (!txtSelectDayTwo.getText().toString().equals("")) {
            date2 = txtSelectDayTwo.getText().toString();
        }

        int countAll = 0;
        int countFirstStatus = 0;
        int countSecondStatus = 0;
        int countThirdStatus = 0;
        String sqlQuewy = "select status, count(status) " +
                "from rgzbn_gm_ceiling_calls_status_history " +
                "where manager_id = ? " +
                "AND change_time between ? and ? " +
                "group by status";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(managerId), date1 + " 00:00:00", date2 + " 23:59:59"});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    if (c.getString(c.getColumnIndex(c.getColumnName(0))).equals("1"))
                        countFirstStatus += c.getInt(c.getColumnIndex(c.getColumnName(1)));

                    if (c.getString(c.getColumnIndex(c.getColumnName(0))).equals("2"))
                        countSecondStatus += c.getInt(c.getColumnIndex(c.getColumnName(1)));

                    if (c.getString(c.getColumnIndex(c.getColumnName(0))).equals("3"))
                        countThirdStatus += c.getInt(c.getColumnIndex(c.getColumnName(1)));

                } while (c.moveToNext());
            }
        }
        c.close();

        countAll = countFirstStatus + countSecondStatus + countThirdStatus;
        if (countAll > 0) {
            LinearLayout linearLayout = new LinearLayout(getActivity());
            TableRow.LayoutParams paramsLinearLayout = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT, 0);
            linearLayout.setLayoutParams(paramsLinearLayout);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);

            paramsLinearLayout = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT, 0);
            LinearLayout linearLayoutNote = new LinearLayout(getActivity());
            linearLayoutNote.setLayoutParams(paramsLinearLayout);
            linearLayoutNote.setOrientation(LinearLayout.VERTICAL);

            TableRow.LayoutParams pieGraphParams = new TableRow.LayoutParams(200,
                    200, 0);
            pieGraphParams.setMargins(50, 15, 0, 15);
            PieGraph pg = new PieGraph(getActivity());
            pg.setLayoutParams(pieGraphParams);
            PieSlice slice = new PieSlice();
            slice.setColor(Color.parseColor("#ff0000"));
            slice.setValue(countFirstStatus);
            pg.addSlice(slice);
            slice = new PieSlice();
            slice.setColor(Color.parseColor("#005400"));
            slice.setValue(countSecondStatus);
            pg.addSlice(slice);
            slice = new PieSlice();
            slice.setColor(Color.parseColor("#3cb371"));
            slice.setValue(countThirdStatus);
            pg.addSlice(slice);
            linearLayout.addView(pg);
            TextView textPieColor = new TextView(getActivity());
            TableRow.LayoutParams tableTextColor = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT, 4f);
            if (countFirstStatus > 0) {
                tableTextColor.setMargins(50, 10, 0, 0);
                textPieColor.setLayoutParams(tableTextColor);
                textPieColor.setText("Недозвон");
                textPieColor.setTextSize(15);
                textPieColor.setTextColor(Color.parseColor("#ff0000"));
                linearLayoutNote.addView(textPieColor);
            }
            if (countSecondStatus > 0) {
                textPieColor = new TextView(getActivity());
                textPieColor.setLayoutParams(tableTextColor);
                textPieColor.setText("Входящие");
                textPieColor.setTextSize(15);
                textPieColor.setTextColor(Color.parseColor("#005400"));
                linearLayoutNote.addView(textPieColor);
            }
            if (countThirdStatus > 0) {
                textPieColor = new TextView(getActivity());
                textPieColor.setLayoutParams(tableTextColor);
                textPieColor.setText("Исходящие");
                textPieColor.setTextSize(15);
                textPieColor.setTextColor(Color.parseColor("#3cb371"));
                linearLayoutNote.addView(textPieColor);
            }
            linearLayout.addView(linearLayoutNote);
            linearManagerTable.addView(linearLayout);
        }
    }

    private void createTableManager(int managerId) {

        String date1 = "0001-01-01",
                date2 = HelperClass.nowDate().substring(0, 10);
        if (!txtSelectDay.getText().toString().equals("")) {
            date1 = txtSelectDay.getText().toString();
        }
        if (!txtSelectDayTwo.getText().toString().equals("")) {
            date2 = txtSelectDayTwo.getText().toString();
        }

        Log.d(TAG, "date1: " + date1);

        Log.d(TAG, "date2: " + date2);

        int countAll = 0;
        int countFirstStatus = 0;
        int countSecondStatus = 0;
        int countThirdStatus = 0;
        String sqlQuewy = "select status, count(status) " +
                "from rgzbn_gm_ceiling_calls_status_history " +
                "where manager_id = ? " +
                "AND substr(change_time, 0, 11) between ? and ? " +
                "group by status";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(managerId), date1, date2});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    if (c.getString(c.getColumnIndex(c.getColumnName(0))).equals("1"))
                        countFirstStatus += c.getInt(c.getColumnIndex(c.getColumnName(1)));

                    if (c.getString(c.getColumnIndex(c.getColumnName(0))).equals("2"))
                        countSecondStatus += c.getInt(c.getColumnIndex(c.getColumnName(1)));

                    if (c.getString(c.getColumnIndex(c.getColumnName(0))).equals("3"))
                        countThirdStatus += c.getInt(c.getColumnIndex(c.getColumnName(1)));

                } while (c.moveToNext());
            }
        }
        c.close();

        countAll += countFirstStatus + countSecondStatus + countThirdStatus;

        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        TableLayout analyticsTableLayout = new TableLayout(getActivity());
        TableRow.LayoutParams tableParamsAnalyticsTable = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT, 4f);
        analyticsTableLayout.setLayoutParams(tableParamsAnalyticsTable);

        linearLayout.addView(analyticsTableLayout);

        linearManagerTable.addView(linearLayout);

        TableRow tableRow = new TableRow(getActivity());
        TableRow.LayoutParams tableParams = new TableRow.LayoutParams(100,
                TableRow.LayoutParams.MATCH_PARENT, 4f);
        tableParams.setMargins(25, 30, 0, 0);

        TextView textTable = new TextView(getActivity());
        textTable.setTextSize(17);
        textTable.setLayoutParams(tableParams);
        textTable.setText("" + countAll);
        textTable.setGravity(Gravity.CENTER);
        textTable.setTextColor(Color.parseColor("#414099"));
        tableRow.addView(textTable);

        textTable = new TextView(getActivity());
        textTable.setTextSize(17);
        textTable.setLayoutParams(tableParams);
        textTable.setText("" + countFirstStatus);
        textTable.setGravity(Gravity.CENTER);
        textTable.setTextColor(Color.parseColor("#414099"));
        tableRow.addView(textTable);

        textTable = new TextView(getActivity());
        textTable.setTextSize(17);
        textTable.setLayoutParams(tableParams);
        textTable.setText("" + countThirdStatus);
        textTable.setGravity(Gravity.CENTER);
        textTable.setTextColor(Color.parseColor("#414099"));
        tableRow.addView(textTable);

        textTable = new TextView(getActivity());
        textTable.setTextSize(17);
        textTable.setLayoutParams(tableParams);
        textTable.setText("" + countSecondStatus);
        textTable.setGravity(Gravity.CENTER);
        textTable.setTextColor(Color.parseColor("#414099"));
        tableRow.addView(textTable);

        if (listManagerClients.size() > 0 && HelperClass.isOnline(getActivity())) {
            UnderlineTextView underlineTextView = new UnderlineTextView(getActivity());
            underlineTextView.setTextSize(17);
            underlineTextView.setLayoutParams(tableParams);
            underlineTextView.setText(listManagerClients.get(listManagerClientsStep).toString());
            underlineTextView.setGravity(Gravity.CENTER);
            underlineTextView.setTextColor(Color.parseColor("#414099"));
            underlineTextView.setOnClickListener(onClickTxt);
            underlineTextView.setId(stepTxt);
            txtList.add(underlineTextView);
            tableRow.addView(underlineTextView);
            stepTxt++;
            listManagerClientsStep++;

            underlineTextView = new UnderlineTextView(getActivity());
            underlineTextView.setTextSize(17);
            underlineTextView.setLayoutParams(tableParams);
            underlineTextView.setText(listManagerClients.get(listManagerClientsStep).toString());
            underlineTextView.setGravity(Gravity.CENTER);
            underlineTextView.setTextColor(Color.parseColor("#414099"));
            underlineTextView.setOnClickListener(onClickTxt);
            underlineTextView.setId(stepTxt);
            txtList.add(underlineTextView);
            tableRow.addView(underlineTextView);
            stepTxt++;
            listManagerClientsStep++;

            textTable = new TextView(getActivity());
            textTable.setTextSize(17);
            textTable.setLayoutParams(tableParams);
            textTable.setText(listManagerClients.get(listManagerClientsStep).toString());
            textTable.setGravity(Gravity.CENTER);
            textTable.setTextColor(Color.parseColor("#414099"));
            tableRow.addView(textTable);
            listManagerClientsStep++;
        }

        analyticsTableLayout.addView(tableRow);
        analyticsTableLayout.setGravity(Gravity.CENTER);

        View view = new View(getActivity());
        view.setBackgroundColor(Color.parseColor("#000000"));
        TableRow.LayoutParams tableParamsView = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                5, 0);
        tableParamsView.setMargins(0, 30, 0, 0);
        view.setLayoutParams(tableParamsView);
        linearManagerTable.addView(view);
    }

    class GetManagersAnalytic extends AsyncTask<Void, Void, Void> {
        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&task=api.getManagersAnalytic";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            final StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {
                    Log.d(TAG, "come " + res);

                    String newRes = "";
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(res);
                        String data = jsonObject.getString("data");
                        String hash = jsonObject.getString("hash");
                        newRes = HelperClass.decrypt(hash, data, getActivity());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    int length = arrayId.length + listManager.size() * 2 + 1;
                    String[] array = new String[length];

                    for (int i = 0; arrayId.length > i; i++) {
                        array[i] = arrayId[i];
                    }

                    int firstStep = 0;
                    int secondStep = 1;
                    try {
                        for (int ii = 0; listManager.size() > ii; ii++) {

                            JSONArray jAr = new JSONArray();
                            JSONArray jAr2 = new JSONArray();

                            try {
                                jsonObject = new JSONObject(newRes);
                                JSONObject jsonObjectMan = jsonObject.getJSONObject(listManager.get(ii).toString());
                                JSONArray clients = jsonObjectMan.getJSONArray("clients");
                                JSONArray projects = jsonObjectMan.getJSONArray("projects");
                                String measures = jsonObjectMan.getString("measures");
                                String deals = jsonObjectMan.getString("deals");

                                Log.d(TAG, "onResponse: " + measures);
                                Log.d(TAG, "onResponse: " + deals);

                                listManagerClients.add(measures);
                                listManagerClients.add(deals);

                                Double summa = 0.0;
                                Integer[] clientsArray = new Integer[clients.length()];
                                for (int i = 0; clients.length() > i; i++) {
                                    clientsArray[i] = Integer.valueOf(clients.get(i).toString());
                                }

                                for (int i = 0; clientsArray.length > i; i++) {
                                    JSONArray projectManager = projects.getJSONObject(i).getJSONArray(clientsArray[i].toString());

                                    for (int j = 0; projectManager.length() > j; j++) {
                                        JSONObject project = projectManager.getJSONObject(j);
                                        String project_id = project.getString("project_id");
                                        Double sum = project.getDouble("sum");
                                        String status = project.getString("status");

                                        if (status.equals("1")) {
                                            jAr.put(clientsArray[i]);
                                        } else {
                                            jAr2.put(clientsArray[i]);
                                            summa += sum;
                                        }
                                    }
                                }
                                listManagerClients.add(String.valueOf(summa));
                            } catch (Exception e) {
                                listManagerClients.add("0");
                                listManagerClients.add("0");
                                listManagerClients.add("0");

                                jAr.put(0);
                                jAr2.put(0);
                            }

                            array[(arrayId.length + firstStep)] = jAr.toString().substring(1, jAr.toString().length() - 1);
                            array[(arrayId.length + secondStep)] = jAr2.toString().substring(1, jAr2.toString().length() - 1);

                            firstStep += 2;
                            secondStep += 2;

                        }
                    } catch (Exception e) {
                        Log.d(TAG, "onResponse: " + e);
                    }

                    arrayId = new String[array.length];
                    arrayId = array.clone();

                    try {
                        repeatManager();
                    } catch (Exception e) {
                        Log.d(TAG, "onResponse error: " + e);
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "onErrorResponse: " + error);
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Log.d(TAG, " send " + String.valueOf(parameters));
                    return parameters;
                }
            };
            requestQueue.add(request);
            return null;
        }
    }

    public void setDate(View v) {
        final Calendar cal = Calendar.getInstance();
        int mYear = cal.get(Calendar.YEAR);
        int mMonth = cal.get(Calendar.MONTH);
        int mDay = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        analyticDate = "";
                        String editTextDateParam;
                        if (monthOfYear < 9) {
                            editTextDateParam = year + "-0" + (monthOfYear + 1);
                        } else {
                            editTextDateParam = year + "-" + (monthOfYear + 1);
                        }
                        if (dayOfMonth < 10) {
                            editTextDateParam = editTextDateParam + "-0" + dayOfMonth;
                        } else {
                            editTextDateParam = editTextDateParam + "-" + dayOfMonth;
                        }

                        txtSelectDay.setText(editTextDateParam);
                        analyticDate = editTextDateParam;
                        createTable();
                        createTableForManager();
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    DatePickerDialog.OnDateSetListener call_date = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateAndTime.set(Calendar.YEAR, year);
            dateAndTime.set(Calendar.MONTH, monthOfYear);
            dateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            setInitialDateTimeCall();
        }
    };

    private void setInitialDateTimeCall() {
        txtSelectDay.setText(txtSelectDay.getText().toString() + " " +
                DateUtils.formatDateTime(getActivity(),
                        dateAndTime.getTimeInMillis(),
                        DateUtils.FORMAT_SHOW_TIME));
        analyticDate += " " + DateUtils.formatDateTime(getActivity(),
                dateAndTime.getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME);
    }

    public void setDateTwo(View v) {
        final Calendar cal = Calendar.getInstance();
        int mYear = cal.get(Calendar.YEAR);
        int mMonth = cal.get(Calendar.MONTH);
        int mDay = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        analyticDate = "";
                        String editTextDateParam;
                        if (monthOfYear < 9) {
                            editTextDateParam = year + "-0" + (monthOfYear + 1);
                        } else {
                            editTextDateParam = year + "-" + (monthOfYear + 1);
                        }
                        if (dayOfMonth < 10) {
                            editTextDateParam = editTextDateParam + "-0" + dayOfMonth;
                        } else {
                            editTextDateParam = editTextDateParam + "-" + dayOfMonth;
                        }

                        txtSelectDayTwo.setText(editTextDateParam);
                        analyticDate = editTextDateParam;
                        createTable();
                        createTableForManager();
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

}