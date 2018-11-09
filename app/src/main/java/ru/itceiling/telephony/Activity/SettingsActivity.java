package ru.itceiling.telephony.Activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ru.itceiling.telephony.R;

public class SettingsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    TextView textMinValue;
    SeekBar seekBarValue;
    String time;
    String stringToParse;
    String TAG = "logd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        SharedPreferences SP = this.getSharedPreferences("JsonCheckTime", MODE_PRIVATE);
        stringToParse = SP.getString("", "");
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

}