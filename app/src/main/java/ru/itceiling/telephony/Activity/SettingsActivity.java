package ru.itceiling.telephony.Activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.BillingRequests;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.EmptyRequestListener;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Purchase;
import org.solovyev.android.checkout.Purchases;
import org.solovyev.android.checkout.RequestListener;

import java.text.DateFormat;
import java.util.Date;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.itceiling.telephony.App;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;
import ru.itceiling.telephony.SubscriptionsActivity;

public class SettingsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    DBHelper dbHelper;
    SQLiteDatabase db;
    TextView textMinValue;
    SeekBar seekBarValue;
    String time;
    String stringToParse;
    String dealer_id;
    static String TAG = "logd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        SharedPreferences SP = this.getSharedPreferences("dealer_id", MODE_PRIVATE);
        dealer_id = SP.getString("", "");

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

            final Adapter adapter = new Adapter();
            mRecycler.setLayoutManager(new LinearLayoutManager(this));
            mRecycler.setAdapter(adapter);

            final Billing billing = App.get().getBilling();
            mCheckout = Checkout.forActivity(this, billing);
            mCheckout.start();
            mCheckout.whenReady(new HistoryLoader(adapter));

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

    @BindView(R.id.recycler)
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
}