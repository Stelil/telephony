package ru.itceiling.telephony.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;

import butterknife.ButterKnife;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;
import ru.itceiling.telephony.broadcaster.VKReceiver;

public class SettingsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    DBHelper dbHelper;
    SQLiteDatabase db;
    TextView textMinValue;
    SeekBar seekBarValue;
    String time;
    String stringToParse;
    String dealer_id, user_id;
    String domen, jsonPassword;
    static String TAG = "logd";
    private RequestQueue requestQueue;
    java.util.Map<String, String> parameters = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        SharedPreferences SP = this.getSharedPreferences("user_id", MODE_PRIVATE);
        user_id = SP.getString("", "");

        SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

        SP = this.getSharedPreferences("link", MODE_PRIVATE);
        domen = SP.getString("", "");

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        String sqlQuewy = "SELECT settings "
                + "FROM rgzbn_users " +
                "WHERE _id = ? ";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{dealer_id});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    stringToParse = c.getString(c.getColumnIndex(c.getColumnName(0)));
                } while (c.moveToNext());
            }
        }
        c.close();

        SP = this.getSharedPreferences("link", MODE_PRIVATE);
        String link = SP.getString("", "");

        if (link.equals("test1")) {
            LinearLayout linearLayout = findViewById(R.id.layoutSet);
            Button btn = new Button(this);
            linearLayout.addView(btn);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(SettingsActivity.this, SubscriptionsActivity.class);
                    startActivity(intent);
                }
            });

            ButterKnife.bind(this);

            /*final Adapter adapter = new Adapter();
            mRecycler.setLayoutManager(new LinearLayoutManager(this));
            mRecycler.setAdapter(adapter);

            final Billing billing = App.get().getBilling();
            mCheckout = Checkout.forActivity(this, billing);
            mCheckout.start();
            mCheckout.whenReady(new HistoryLoader(adapter));*/

        }

        //afterVK();
    }

    AlertDialog dialog;

    public void onButtonPwd(View view) {
        try {
            requestQueue = Volley.newRequestQueue(getApplicationContext());

            final Context context = this;
            LayoutInflater li = LayoutInflater.from(context);
            View promptsView = li.inflate(R.layout.layout_profile_password, null);
            AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
            mDialogBuilder.setView(promptsView);
            final EditText ed_oldPassword = (EditText) promptsView.findViewById(R.id.ed_oldPassword);
            final EditText ed_newPassword1 = (EditText) promptsView.findViewById(R.id.ed_newPassword1);
            final EditText ed_newPassword2 = (EditText) promptsView.findViewById(R.id.ed_newPassword2);

            DBHelper dbHelper = new DBHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            dialog = new AlertDialog.Builder(context)
                    .setView(promptsView)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setTitle("Изменение пароля ")
                    .create();

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface dialogInterface) {

                    Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // TODO Do something

                            if (HelperClass.isOnline(SettingsActivity.this)) {

                                if (ed_newPassword1.getText().toString().length() > 5 &&
                                        ed_newPassword2.getText().toString().length() > 5) {
                                    if (ed_newPassword1.getText().toString().equals(ed_newPassword2.getText().toString())) {

                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("old_password", ed_oldPassword.getText().toString());
                                        jsonObject.put("password", ed_newPassword1.getText().toString());
                                        jsonObject.put("user_id", user_id);

                                        parameters.put("data", HelperClass.encrypt(jsonObject.toString(), context));
                                        new ChangePwd().execute();

                                    } else {
                                        Toast.makeText(getApplicationContext(), "Новые пароли не совпадают",
                                                Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), "Длина пароля должна быть больше 5 символов",
                                            Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), "Не удалось проверить старый пароль(нет интернета)",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            });
            dialog.show();
        } catch (Exception e) {
            Log.d(TAG, "ChangePass: exception " + e);
        }
    }

    class ChangePwd extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&amp;task=api.changePwd";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new com.android.volley.Response.Listener<String>() {
                @Override
                public void onResponse(String res) {
                    try {
                        String newRes = "";
                        org.json.JSONObject jsonObject = null;
                        try {
                            jsonObject = new org.json.JSONObject(res);
                            String data = jsonObject.getString("data");
                            String hash = jsonObject.getString("hash");
                            newRes = HelperClass.decrypt(hash, data, SettingsActivity.this);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.d(TAG, "onResponse: " + newRes);

                        if (newRes.equals("true")) {
                            dialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Пароль изменён",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Неверный старый пароль",
                                    Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e) {
                    }
                }
            }, new com.android.volley.Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {

                @Override
                protected java.util.Map<String, String> getParams() throws AuthFailureError {
                    return parameters;
                }
            };

            requestQueue.add(request);

            return null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onButtonCheckTimeCallback(View view) {
        time = "минут";
        dialogCheckTime();
        setSettings("CheckTimeCallback");
    }

    public void onButtonCheckTimeCall(View view) {
        time = "секунд";
        dialogCheckTime();
        setSettings("CheckTimeCall");
    }

    private void dialogCheckTime() {
        final Context context = SettingsActivity.this;
        View promptsView;
        LayoutInflater li = LayoutInflater.from(context);
        promptsView = li.inflate(R.layout.dialog_seekbar, null);
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
        mDialogBuilder.setView(promptsView);

        textMinValue = (TextView) promptsView.findViewById(R.id.textMinValue);
        textMinValue.setText("0 " + time);
        seekBarValue = (SeekBar) promptsView.findViewById(R.id.seekBarValue);
        seekBarValue.setOnSeekBarChangeListener(this);

        mDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Ок",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                int progress = seekBarValue.getProgress();

                                JSONParser parser = new JSONParser();
                                JSONObject json = null;
                                try {
                                    json = (JSONObject) parser.parse(stringToParse);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                switch (time) {
                                    case "секунд":
                                        json.put("CheckTimeCall", progress);
                                        break;
                                    case "минут":
                                        json.put("CheckTimeCallback", progress);
                                        break;
                                }

                                stringToParse = String.valueOf(json);

                            }
                        })
                .setNegativeButton("Назад",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = mDialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(R.color.colorWhite);
        alertDialog.show();
    }

    void saveData(String json) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_SETTINGS, json);
        db.update(DBHelper.TABLE_USERS, values, "_id = ?", new String[]{dealer_id});

        HelperClass.addExportData(
                this,
                Integer.valueOf(dealer_id),
                "rgzbn_users",
                "send");
    }

    private void setSettings(String param) {
        org.json.JSONObject json;
        try {
            json = new org.json.JSONObject(stringToParse);
            textMinValue.setText(json.getString(param));
            seekBarValue.setProgress(json.getInt(param));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveData(String.valueOf(stringToParse));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        textMinValue.setText(String.valueOf(seekBar.getProgress()) + " " + time);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    /*@BindView(R.id.recycler)
    RecyclerView mRecycler;
    private Checkout mCheckout;

    private static class HistoryLoader extends Checkout.EmptyListener implements RequestListener<Purchases> {
        private final Adapter mAdapter;

        public HistoryLoader(Adapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public void onReady(@Nonnull final BillingRequests requests) {
            requests.isGetPurchaseHistorySupported(ProductTypes.IN_APP, new EmptyRequestListener<Object>() {
                @Override
                public void onSuccess(@Nonnull Object result) {
                    requests.getWholePurchaseHistory(ProductTypes.IN_APP, null, HistoryLoader.this);
                }
            });
        }

        @Override
        public void onSuccess(@Nonnull Purchases purchases) {
            mAdapter.update(purchases);
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.purchase_sku)
        TextView mSku;
        @BindView(R.id.purchase_time)
        TextView mTime;
        @BindView(R.id.purchase_icon)
        ImageView mIcon;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        void onBind(Purchase purchase) {
            Log.d(TAG, "onBind: " + purchase.toString());
            mSku.setText(purchase.sku);
            mTime.setText(DateFormat.getDateTimeInstance().format(new Date(purchase.time)));
            mIcon.setImageDrawable(new ColorDrawable(purchase.sku.hashCode()));
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final LayoutInflater mInflater = LayoutInflater.from(SettingsActivity.this);
        @Nullable
        private Purchases mPurchases;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(R.layout.purchase, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (mPurchases == null) return;
            holder.onBind(mPurchases.list.get(position));
        }

        @Override
        public int getItemCount() {
            if (mPurchases == null) return 0;
            return mPurchases.list.size();
        }

        public void update(Purchases purchases) {
            mPurchases = purchases;
            notifyDataSetChanged();
        }
    }
    */

    public void btnVK(View view) {
        if (!VKSdk.isLoggedIn()) {
            String[] scope = {VKScope.GROUPS};
            VKSdk.login(this, scope);
        } else {
            dialogVK();
        }
    }

    private void dialogVK() {
        String title = "Выйти из аккаунта?";
        String button1String = "Да";
        String button2String = "Нет";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title); // сообщение
        builder.setPositiveButton(button1String, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                VKSdk.logout();
                Toast.makeText(SettingsActivity.this, "Вы вышли из аккаунта",
                        Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton(button2String, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        builder.setCancelable(true);
        builder.create();
        builder.show();
    }

    VKReceiver vkReceiver;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                vkReceiver = new VKReceiver(SettingsActivity.this, true);
            }

            @Override
            public void onError(VKError error) {
                Toast.makeText(SettingsActivity.this, error.errorMessage, Toast.LENGTH_SHORT).show();
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}