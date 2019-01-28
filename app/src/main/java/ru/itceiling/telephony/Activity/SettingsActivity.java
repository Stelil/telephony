package ru.itceiling.telephony.Activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.BillingRequests;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.EmptyRequestListener;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Purchase;

import ru.itceiling.telephony.App;
import ru.itceiling.telephony.R;

public class SettingsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    TextView textMinValue;
    SeekBar seekBarValue;
    String time;
    String stringToParse;
    String TAG = "logd";

    private class PurchaseListener extends EmptyRequestListener<Purchase> {
        @Override
        public void onSuccess(Purchase purchase) {
            Log.d(TAG, "onSuccess: " + purchase.toString());
        }

        @Override
        public void onError(int response, Exception e) {
            Log.d(TAG, "onError: " + e);
        }
    }

    private class InventoryCallback implements Inventory.Callback {
        @Override
        public void onLoaded(Inventory.Products products) {
            // your code here
        }
    }

    private final ActivityCheckout mCheckout = Checkout.forActivity(this, App.get().getBilling());
    private Inventory mInventory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        SharedPreferences SP = this.getSharedPreferences("JsonCheckTime", MODE_PRIVATE);
        stringToParse = SP.getString("", "");

        mCheckout.start();

        mCheckout.createPurchaseFlow(new PurchaseListener());

        mInventory = mCheckout.makeInventory();
        mInventory.load(Inventory.Request.create()
                .loadAllPurchases()
                .loadSkus(ProductTypes.IN_APP, "telephony.subscription.1month"), new InventoryCallback());

        Button billing = findViewById(R.id.billing);
        billing.setOnClickListener(this);
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

                                SharedPreferences SP = getSharedPreferences("JsonCheckTime", MODE_PRIVATE);
                                SharedPreferences.Editor ed = SP.edit();
                                ed.putString("", String.valueOf(json));
                                ed.commit();

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

    private void setSettings(String param) {
        SharedPreferences SP = this.getSharedPreferences("JsonCheckTime", MODE_PRIVATE);
        String jsonObject = SP.getString("", "");

        org.json.JSONObject json = null;
        try {
            json = new org.json.JSONObject(jsonObject);
            textMinValue.setText(json.getString(param));
            seekBarValue.setProgress(json.getInt(param));
        } catch (JSONException e) {
            e.printStackTrace();
        }
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



    @Override
    protected void onDestroy() {
        mCheckout.stop();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCheckout.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        mCheckout.whenReady(new Checkout.EmptyListener() {
            @Override
            public void onReady(BillingRequests requests) {
                requests.purchase(ProductTypes.IN_APP, "telephony.subscription.1month",
                        null, mCheckout.getPurchaseFlow());
            }
        });
    }

}