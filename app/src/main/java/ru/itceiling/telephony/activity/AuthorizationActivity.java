package ru.itceiling.telephony.activity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.itceiling.telephony.App;
import ru.itceiling.telephony.AppVK;
import ru.itceiling.telephony.DBHelper;
import ru.itceiling.telephony.HelperClass;
import ru.itceiling.telephony.R;
import ru.itceiling.telephony.broadcaster.CallbackReceiver;
import ru.itceiling.telephony.broadcaster.ExportDataReceiver;
import ru.itceiling.telephony.broadcaster.ImportDataReceiver;

public class AuthorizationActivity extends AppCompatActivity implements View.OnClickListener,
        GoogleApiClient.OnConnectionFailedListener {

    static DBHelper dbHelper;
    static SQLiteDatabase db;
    String domen = "calc";
    static String TAG = "ImportLog";
    String user_id = "";
    String change_time_global = "";
    String sync_import = "";
    static RequestQueue requestQueue;
    static org.json.simple.JSONObject jsonSync_Import = new org.json.simple.JSONObject();

    static ProgressDialog pd;

    static Intent intent;

    StringRequest request = null;

    Map<String, String> parameters = new HashMap<String, String>();

    String jsonAuth = "";
    public static ProgressDialog mProgressDialog;
    EditText login, password;
    Button btn_vhod;

    final public static String ONE_TIME = "onetime";

    AlertDialog dialogSubs;

    private static final int RC_SIGN_IN = 9001;
    private static final int G_SIGN_IN = 51966;

    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    private TextView mStatusTextView;
    private TextView mDetailTextView;
    private ProgressBar progressBar;

    private static final List<String> SKUS = Arrays.asList("telephony.subscription.1month", "telephony.subscription.6month");

    private boolean subs = false;
    private String timeSubs;
    private int typeEnter = 0;

    //private String[] scope = new String[]{VKScope.EMAIL};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization);

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        login = findViewById(R.id.login);
        password = findViewById(R.id.password);

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        mStatusTextView = (TextView) findViewById(R.id.status);
        mDetailTextView = (TextView) findViewById(R.id.detail);

        //Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("549262362686-fqjaiichc2vuegqmtesoe6pii6l9ci82.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();

        //Button listeners

        /*findViewById(R.id.buttonVK).setOnClickListener(this);
        VKSdk.initialize(this);
        VKSdk.login(this, scope);*/

        prBar();
    }

    void prBar() {
        SharedPreferences SP = getSharedPreferences("enter", MODE_PRIVATE);
        Log.d(TAG, "prBar: " + SP.getString("", ""));
        if (SP.getString("", "").equals("1")) {
            if (this != null) {
                importData();
            }
        } else if (subs) {
            Toast.makeText(this, "else prBar", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }

    @Override
    public void onDestroy() {
        //mCheckout.stop();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //GP
        if (requestCode == G_SIGN_IN && resultCode != 0) {
            subs = true;
            dialogSubs.dismiss();
            if (typeEnter == 1) {
                signIn();
            } else if (typeEnter == 2) {
                new GetPublicKey().execute();
            }
            /*else if (typeEnter == 3) {
                VKSdk.login(this, scope);
            }*/
        }

        // Google
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            }
        }

        //VK

        /*
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(final VKAccessToken res) {

                String email = "";
                final String[] fullName = {""};
                email = res.email;

                if (email == "") {
                    Toast.makeText(AuthorizationActivity.this, "Для авторизации нам необходимо знать Вашу почту",
                            Toast.LENGTH_SHORT).show();
                } else {
                    VKRequest request = VKApi.users().get();
                    final String finalEmail = email;
                    request.executeWithListener(new VKRequest.VKRequestListener() {
                        public void onComplete(VKResponse response) {
                            try {
                                JSONObject obj = new JSONObject(response.json.toString());
                                JSONArray arr = obj.getJSONArray("response");
                                String first_name = arr.getJSONObject(0).getString("first_name");
                                String last_name = arr.getJSONObject(0).getString("last_name");

                                fullName[0] = first_name + " " + last_name;
                                createUserVK(finalEmail, fullName[0]);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(VKError error) {
                        }

                        @Override
                        public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                        }
                    });
                }
            }

            @Override
            public void onError(VKError error) {
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }

        */

    }

    FirebaseUser user;

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        // [START_EXCLUDE silent]
        progressBar.setVisibility(View.VISIBLE);
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            user = mAuth.getCurrentUser();
                            new GetPublicKey().execute();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(AuthorizationActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // [START_EXCLUDE]
                        progressBar.setVisibility(View.GONE);
                        // [END_EXCLUDE]
                    }
                });
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this,
                "Google Play Services error.",
                Toast.LENGTH_SHORT).show();

        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.sign_in_button) {
            typeEnter = 1;
            signIn();
        }
        /*if (i == R.id.buttonVK) {
            typeEnter = 3;
            if (subs) {
                VKSdk.login(this, scope);
            } else {
                alertSubs();
            }
        }*/
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    class GetPublicKey extends AsyncTask<Void, Void, Void> {
        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&task=api.getPublicKey";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(AuthorizationActivity.this);
            mProgressDialog.setMessage("Проверяем...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(final Void... params) {

            request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {
                    Log.d(TAG, "onResponse GetPublicKey: " + res);
                    try {
                        JSONObject jsonObject = new JSONObject(res);
                        String key_number = jsonObject.getString("key_number");
                        String public_key = jsonObject.getString("public_key");
                        switch (typeEnter) {
                            case 1:
                                registrationGoogleOnDB(key_number, public_key, user);
                                break;
                            case 2:
                                registrationButton(key_number, public_key);
                                break;
                        }

                    } catch (Exception e) {
                        Log.d(TAG, "onResponse: GetPublicKey " + e);
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mProgressDialog.dismiss();
                    Log.d(TAG, "onErrorResponse: " + error);
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Проверьте подключение к интернету, или возможны работы на сервере", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    parameters.put("data", "give me a public key");
                    Log.d(TAG, "getParams: " + parameters);
                    return parameters;
                }
            };

            request.setShouldCache(false);
            RequestQueue requestQueue = Volley.newRequestQueue(AuthorizationActivity.this);
            requestQueue.add(request);

            return null;
        }
    }

    Map<String, String> parametersAuth = new HashMap<String, String>();

    private void registrationGoogleOnDB(String key_number, String public_key, FirebaseUser user) {

        org.json.simple.JSONObject jsonObjectAuth1 = new org.json.simple.JSONObject();
        jsonObjectAuth1.put("email", user.getEmail());
        jsonObjectAuth1.put("fio", user.getDisplayName());

        org.json.simple.JSONObject jsonObjectAuth2 = new org.json.simple.JSONObject();
        jsonObjectAuth2.put("key_number", key_number);
        jsonObjectAuth2.put("data", HelperClass.publicKey(public_key, String.valueOf(jsonObjectAuth1)));

        parametersAuth.put("data", String.valueOf(jsonObjectAuth2));
        Log.d(TAG, "registrationGoogleOnDB: " + parametersAuth.toString());
        new SocialAuth().execute();
    }

    private void createUserVK(String email, String fullName) {

        org.json.simple.JSONObject jsonObjectAuth = new org.json.simple.JSONObject();
        jsonObjectAuth.put("email", email);
        jsonObjectAuth.put("fio", fullName);
        jsonAuth = String.valueOf(jsonObjectAuth);

        mProgressDialog = new ProgressDialog(AuthorizationActivity.this);
        mProgressDialog.setMessage("Проверяем...");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        new SocialAuth().execute();
    }

    class SocialAuth extends AsyncTask<Void, Void, Void> {
        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&task=api.register";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {

            request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {

                    mProgressDialog.dismiss();

                    String log = user.getEmail();
                    int i = log.indexOf("@");
                    String login = log.substring(0, i);
                    login = login.toLowerCase();

                    Pattern pattern = Pattern.compile("[^a-z0-9\\_\\-\\.]");
                    Matcher matcher = pattern.matcher(login);
                    String result = matcher.replaceAll("-");
                    if (result.charAt(0) == '-') {
                        result = result.substring(1);
                    }
                    if (result.charAt(result.length() - 1) == '-') {
                        result = result.substring(0, result.length() - 1);
                    }

                    SharedPreferences SP = getSharedPreferences("login_user", MODE_PRIVATE);
                    SharedPreferences.Editor ed = SP.edit();
                    ed.putString("", result);
                    ed.commit();

                    SP = getSharedPreferences("static_key", MODE_PRIVATE);
                    ed = SP.edit();
                    ed.putString("", "crutch");
                    ed.commit();

                    String newRes = "";
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(res);
                        String data = jsonObject.getString("data");
                        String hash = jsonObject.getString("hash");
                        newRes = HelperClass.decrypt(hash, data, AuthorizationActivity.this);
                    } catch (JSONException e) {
                        Log.d(TAG, "onResponse: socialAuth " + e);
                        newRes = "null";
                    }

                    if (newRes != null && !newRes.equals("null")) {
                        try {
                            jsonObject = new JSONObject(newRes);

                            Log.d(TAG, "onResponse: " + newRes);
                            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            final SimpleDateFormat dateFormatDD = new SimpleDateFormat("dd");
                            int user_id = jsonObject.getInt("id");
                            String name = jsonObject.getString("name");
                            String email = jsonObject.getString("email");
                            String block = jsonObject.getString("block");
                            String sendEmail = jsonObject.getString("sendEmail");
                            String registerDate = jsonObject.getString("registerDate");
                            String lastvisitDate = jsonObject.getString("lastvisitDate");
                            String activation = jsonObject.getString("activation");
                            String params = jsonObject.getString("params");
                            String dealer_id = jsonObject.getString("dealer_id");
                            String change_time = jsonObject.getString("change_time");
                            String associated_client = jsonObject.getString("associated_client");
                            String period_start_date = jsonObject.getString("period_start_date");
                            String period = jsonObject.getString("period");
                            final String datetime = jsonObject.getString("datetime");

                            DateTime period_start = DateTime.parse(period_start_date, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
                            final DateTime datetimeNow = DateTime.parse(datetime, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
                            DateTime period_end_date = null;

                            String[] ar = period.split(" ");
                            if (period.toLowerCase().contains("month")) {
                                period_end_date = period_start.plusMonths(Integer.parseInt(ar[0]));
                            } else if (period.toLowerCase().contains("week")) {
                                period_end_date = period_start.plusWeeks(Integer.parseInt(ar[0]));
                            }
                            final DateTime finalPeriod_end_date = period_end_date;

                            SP = getSharedPreferences("dealer_id", MODE_PRIVATE);
                            ed = SP.edit();
                            ed.putString("", dealer_id);
                            ed.commit();

                            SP = getSharedPreferences("user_id", MODE_PRIVATE);
                            ed = SP.edit();
                            ed.putString("", String.valueOf(user_id));
                            ed.commit();

                            SP = getSharedPreferences("enter", MODE_PRIVATE);
                            ed = SP.edit();
                            ed.putString("", "1");
                            ed.commit();

                            jsonObject = new JSONObject();
                            jsonObject.put("CheckTimeCallback", 10); // для CallbackReceiver
                            jsonObject.put("CheckTimeCall", 5);    // для CallReceiver

                            SP = getSharedPreferences("link", MODE_PRIVATE);
                            ed = SP.edit();
                            ed.putString("", domen);
                            ed.commit();

                            String sqlQuewy = "SELECT change_time "
                                    + "FROM history_import_to_server " +
                                    "where user_id = ?";
                            Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(dealer_id)});
                            if (c != null) {
                                if (c.moveToFirst()) {
                                } else {
                                    ContentValues values = new ContentValues();
                                    values.put(DBHelper.KEY_CHANGE_TIME, "0000-00-00 00:00:00");
                                    values.put(DBHelper.KEY_USER_ID, dealer_id);
                                    db.insert(DBHelper.HISTORY_IMPORT_TO_SERVER, null, values);
                                }
                            }

                            sqlQuewy = "SELECT _id "
                                    + "FROM rgzbn_users " +
                                    "where _id = ?";
                            c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(dealer_id)});
                            if (c != null) {
                                if (c.moveToFirst()) {
                                } else {
                                    ContentValues values = new ContentValues();
                                    values.put(DBHelper.KEY_ID, user_id);
                                    values.put(DBHelper.KEY_NAME, name);
                                    values.put(DBHelper.KEY_EMAIL, email);
                                    values.put(DBHelper.KEY_BLOCK, block);
                                    values.put(DBHelper.KEY_SENDEMAIL, sendEmail);
                                    values.put(DBHelper.KEY_REGISTERDATE, registerDate);
                                    values.put(DBHelper.KEY_LASTVISITDATE, lastvisitDate);
                                    values.put(DBHelper.KEY_ACTIVATION, activation);
                                    values.put(DBHelper.KEY_PARAMS, params);
                                    values.put(DBHelper.KEY_ASSOCIATED_CLIENT, associated_client);
                                    values.put(DBHelper.KEY_CHANGE_TIME, change_time);
                                    values.put(DBHelper.KEY_SETTINGS, String.valueOf(jsonObject));
                                    db.insert(DBHelper.TABLE_USERS, null, values);
                                }
                            }

                            importData();

                        } catch (JSONException e) {
                            Log.d(TAG, "JSONException: socialAuth " + e);
                            Toast.makeText(AuthorizationActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mProgressDialog.dismiss();
                    Log.d(TAG, "onErrorResponse:PublicKey " + error);
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Проверьте подключение к интернету, или возможны работы на сервере", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Log.d(TAG, "getParams: " + parametersAuth);
                    return parametersAuth;
                }
            };

            request.setShouldCache(false);
            RequestQueue requestQueue = Volley.newRequestQueue(AuthorizationActivity.this);
            requestQueue.add(request);

            return null;
        }
    }

    public void buttonVhod(View view) {
        if (login.getText().toString().equals("") || password.getText().toString().equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Введите данные", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            typeEnter = 2;
            new GetPublicKey().execute();
        }
    }

    void registrationButton(String key_number, String public_key) {

        org.json.simple.JSONObject jsonObjectAuth = new org.json.simple.JSONObject();
        jsonObjectAuth.put("username", login.getText().toString());
        jsonObjectAuth.put("password", password.getText().toString());

        org.json.simple.JSONObject jsonObjectAuth2 = new org.json.simple.JSONObject();
        jsonObjectAuth2.put("key_number", key_number);
        jsonObjectAuth2.put("data", HelperClass.publicKey(public_key, String.valueOf(jsonObjectAuth)));

        parametersAuth.put("data", String.valueOf(jsonObjectAuth2));

        SharedPreferences SP = getSharedPreferences("link", MODE_PRIVATE);
        SharedPreferences.Editor ed = SP.edit();
        ed.putString("", "calc");
        ed.commit();

        domen = "calc";
        new SendAuthorization().execute();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    class SendAuthorization extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&task=api.Authorization_FromAndroid";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(final Void... params) {

            request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {
                    SharedPreferences SP = getSharedPreferences("login_user", MODE_PRIVATE);
                    SharedPreferences.Editor ed = SP.edit();
                    ed.putString("", login.getText().toString());
                    ed.commit();

                    SP = getSharedPreferences("static_key", MODE_PRIVATE);
                    ed = SP.edit();
                    ed.putString("", "crutch");
                    ed.commit();

                    String newRes = "";
                    final JSONObject[] jsonObject = {null};
                    try {
                        jsonObject[0] = new JSONObject(res);
                        String data = jsonObject[0].getString("data");
                        String hash = jsonObject[0].getString("hash");
                        newRes = HelperClass.decrypt(hash, data, AuthorizationActivity.this);
                    } catch (JSONException e) {
                        Log.d(TAG, "onResponse: " + e);
                    }

                    try {
                        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        final SimpleDateFormat dateFormatDD = new SimpleDateFormat("dd");
                        jsonObject[0] = new JSONObject(newRes);
                        final int user_id = jsonObject[0].getInt("id");
                        String name = jsonObject[0].getString("name");
                        String email = jsonObject[0].getString("email");
                        String block = jsonObject[0].getString("block");
                        String sendEmail = jsonObject[0].getString("sendEmail");
                        String registerDate = jsonObject[0].getString("registerDate");
                        String lastvisitDate = jsonObject[0].getString("lastvisitDate");
                        String activation = jsonObject[0].getString("activation");
                        String params = jsonObject[0].getString("params");
                        String dealer_id = jsonObject[0].getString("dealer_id");
                        String change_time = jsonObject[0].getString("change_time");
                        String associated_client = jsonObject[0].getString("associated_client");
                        final String settings = jsonObject[0].getString("settings");
                        final String period_start_date = jsonObject[0].getString("period_start_date");
                        final String period = jsonObject[0].getString("period");
                        final String datetime = jsonObject[0].getString("datetime");

                        DateTime period_start = DateTime.parse(period_start_date, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
                        final DateTime datetimeNow = DateTime.parse(datetime, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
                        DateTime period_end_date = null;

                        String[] ar = period.split(" ");
                        if (period.toLowerCase().contains("month")) {
                            period_end_date = period_start.plusMonths(Integer.parseInt(ar[0]));
                        } else if (period.toLowerCase().contains("week")) {
                            period_end_date = period_start.plusWeeks(Integer.parseInt(ar[0]));
                        }
                        final DateTime finalPeriod_end_date = period_end_date;

                        String ob = jsonObject[0].getString("groups");

                        SP = getSharedPreferences("dealer_id", MODE_PRIVATE);
                        ed = SP.edit();
                        ed.putString("", String.valueOf(dealer_id));
                        ed.commit();

                        SP = getSharedPreferences("user_id", MODE_PRIVATE);
                        ed = SP.edit();
                        ed.putString("", String.valueOf(user_id));
                        ed.commit();

                        SP = getSharedPreferences("enter", MODE_PRIVATE);
                        ed = SP.edit();
                        ed.putString("", "1");
                        ed.commit();

                        String sqlQuewy = "SELECT change_time "
                                + "FROM history_import_to_server " +
                                "where user_id = ?";
                        Cursor c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(dealer_id)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                            } else {
                                ContentValues values = new ContentValues();
                                values.put(DBHelper.KEY_CHANGE_TIME, "0000-00-00 00:00:00");
                                values.put(DBHelper.KEY_USER_ID, dealer_id);
                                db.insert(DBHelper.HISTORY_IMPORT_TO_SERVER, null, values);
                            }
                        }

                        sqlQuewy = "SELECT _id "
                                + "FROM rgzbn_users " +
                                "where _id = ?";
                        c = db.rawQuery(sqlQuewy, new String[]{String.valueOf(dealer_id)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                            } else {
                                ContentValues values = new ContentValues();
                                values.put(DBHelper.KEY_ID, dealer_id);
                                values.put(DBHelper.KEY_NAME, name);
                                values.put(DBHelper.KEY_EMAIL, email);
                                values.put(DBHelper.KEY_BLOCK, block);
                                values.put(DBHelper.KEY_SENDEMAIL, sendEmail);
                                values.put(DBHelper.KEY_REGISTERDATE, registerDate);
                                values.put(DBHelper.KEY_LASTVISITDATE, lastvisitDate);
                                values.put(DBHelper.KEY_ACTIVATION, activation);
                                values.put(DBHelper.KEY_PARAMS, params);
                                values.put(DBHelper.KEY_ASSOCIATED_CLIENT, associated_client);
                                values.put(DBHelper.KEY_CHANGE_TIME, change_time);
                                values.put(DBHelper.KEY_SETTINGS, settings);
                                db.insert(DBHelper.TABLE_USERS, null, values);
                            }
                        }

                        final int[] i = {0};

                        for (String retval : ob.split(",")) {
                            int indexJava = retval.indexOf(":");
                            if (indexJava == -1) {
                            } else {
                                for (String retval1 : retval.split(":")) {
                                    retval1 = retval1.replaceAll("[^0-9]", "");

                                    if (retval1.equals("13")) {
                                        SP = getSharedPreferences("group_id", MODE_PRIVATE);
                                        ed = SP.edit();
                                        ed.putString("", "13");
                                        ed.commit();
                                    } else if (retval1.equals("14")) {
                                        SP = getSharedPreferences("group_id", MODE_PRIVATE);
                                        ed = SP.edit();
                                        ed.putString("", "14");
                                        ed.commit();
                                    }

                                    if (retval1.equals("25")) {
                                        i[0]++;
                                        String[] array = {"test1", "calc"};
                                        AlertDialog.Builder builder = new AlertDialog.Builder(AuthorizationActivity.this);
                                        builder.setItems(array, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int item) {
                                                switch (item) {
                                                    case 0:
                                                        SharedPreferences SP = getSharedPreferences("link", MODE_PRIVATE);
                                                        SharedPreferences.Editor ed = SP.edit();
                                                        ed.putString("", "test1");
                                                        ed.commit();
                                                        domen = "test1";
                                                        importData();
                                                        break;
                                                    case 1:
                                                        SP = getSharedPreferences("link", MODE_PRIVATE);
                                                        ed = SP.edit();
                                                        ed.putString("", "calc");
                                                        ed.commit();
                                                        domen = "calc";
                                                        importData();
                                                        break;
                                                }
                                            }
                                        });

                                        builder.setCancelable(false);
                                        builder.create();
                                        builder.show();

                                        break;
                                    }
                                }
                            }
                        }

                        if (i[0] == 0) {
                            importData();
                            mProgressDialog.dismiss();
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "onResponse: sendAuthorization " + e);
                        mProgressDialog.dismiss();
                        Toast toast = Toast.makeText(getApplicationContext(),
                                res, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mProgressDialog.dismiss();
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Проверьте подключение к интернету, или возможны работы на сервере", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    return parametersAuth;
                }
            };

            request.setShouldCache(false);
            RequestQueue requestQueue = Volley.newRequestQueue(AuthorizationActivity.this);
            requestQueue.add(request);

            return null;
        }
    }

    private void importData() {

        Log.d(TAG, "importData: ");
        SharedPreferences SP = getSharedPreferences("link", MODE_PRIVATE);
        domen = SP.getString("", "");
        int count = 0;
        dbHelper = new DBHelper(this);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sqlQuewy = "SELECT * "
                + "FROM history_send_to_server";
        Cursor c = db.rawQuery(sqlQuewy, new String[]{});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    count++;
                } while (c.moveToNext());
            }
        }
        c.close();

        if (count == 0) {
            SharedPreferences SP_end = getSharedPreferences("dealer_id", MODE_PRIVATE);
            user_id = SP_end.getString("", "");

            JSONObject jsonObject = new JSONObject();
            sqlQuewy = "SELECT change_time "
                    + "FROM history_import_to_server" +
                    " WHERE user_id = ?";
            c = db.rawQuery(sqlQuewy, new String[]{user_id});
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        change_time_global = c.getString(c.getColumnIndex(c.getColumnName(0)));
                    } while (c.moveToNext());
                }
            }
            c.close();

            try {
                jsonObject.put("change_time", change_time_global);
                jsonObject.put("dealer_id", user_id);
            } catch (Exception e) {
                Log.d(TAG, "onResponse: ImportData() " + e);
            }

            parameters.clear();
            parameters.put("data", HelperClass.encrypt(jsonObject.toString(), this));
            Log.d(TAG, "importData: 2");
            new ImportData().execute();

        } else {
            finish();
            intent = new Intent(AuthorizationActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    class ImportData extends AsyncTask<Void, Void, Void> {

        String insertUrl = "http://" + domen + ".gm-vrn.ru/index.php?option=com_gm_ceiling&task=api.sendInfoToAndroidCallGlider";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(AuthorizationActivity.this);
            pd.setTitle("Загрузка клиентов ... ");
            pd.setMessage("Пожалуйста подождите");
            pd.setIndeterminate(false);
            pd.setCancelable(false);

            pd.show();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // try {

            StringRequest request = new StringRequest(Request.Method.POST, insertUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {
                    SQLiteDatabase db;
                    db = dbHelper.getReadableDatabase();
                    String newRes = "";
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(res);
                        String data = jsonObject.getString("data");
                        String hash = jsonObject.getString("hash");
                        newRes = HelperClass.decrypt(hash, data, AuthorizationActivity.this);
                    } catch (JSONException e) {
                        Log.d(TAG, "onResponse: ImportData " + e);
                        newRes = "null";
                    }

                    //Log.d(TAG, "onResponse: " + newRes);
                    if (newRes != null && !newRes.equals("null")) {
                        try {

                            /*try {
                                jsonObject = new JSONObject(newRes);
                                String b = jsonObject.getString("b");
                                String l = jsonObject.getString("l");
                                String period = jsonObject.getString("period");
                                if (b.equals("0") && l.equals("t")) {
                                    alertDialog();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse:importLog " + e);
                            }*/

                            ContentValues values;
                            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date change_max = ft.parse(change_time_global);

                            jsonObject = new JSONObject(newRes);
                            try {
                                JSONArray rgzbn_gm_ceiling_clients = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients");
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_clients " + rgzbn_gm_ceiling_clients.length());
                                for (int i = 0; i < rgzbn_gm_ceiling_clients.length(); i++) {
                                    values = new ContentValues();
                                    org.json.JSONObject client = rgzbn_gm_ceiling_clients.getJSONObject(i);

                                    String id = client.getString("id");
                                    String client_name = client.getString("client_name");
                                    String client_data_id = client.getString("client_data_id");
                                    String type_id = client.getString("type_id");
                                    String manager_id = client.getString("manager_id");
                                    String dealer_id = client.getString("dealer_id");
                                    String created = client.getString("created");
                                    String sex = client.getString("sex");
                                    String label_id = client.getString("label_id");
                                    String deleted_by_user = client.getString("deleted_by_user");
                                    String change_time = client.getString("change_time");

                                    values.put(DBHelper.KEY_CLIENT_NAME, client_name);
                                    values.put(DBHelper.KEY_CLIENT_DATA_ID, client_data_id);
                                    values.put(DBHelper.KEY_TYPE_ID, type_id);
                                    values.put(DBHelper.KEY_MANAGER_ID, manager_id);
                                    values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                                    values.put(DBHelper.KEY_CREATED, created);
                                    values.put(DBHelper.KEY_SEX, sex);
                                    values.put(DBHelper.KEY_LABEL_ID, label_id);
                                    values.put(DBHelper.KEY_DELETED_BY_USER, deleted_by_user);
                                    values.put(DBHelper.KEY_CHANGE_TIME, change_time);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_clients" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, values,
                                                    "_id = ?", new String[]{id});

                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse:rgzbn_gm_ceiling_clients " + e);
                            }

                            try {
                                JSONArray rgzbn_gm_ceiling_clients_contacts = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients_contacts");
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_clients_contacts " + rgzbn_gm_ceiling_clients_contacts.length());
                                for (int i = 0; i < rgzbn_gm_ceiling_clients_contacts.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject client_contact = rgzbn_gm_ceiling_clients_contacts.getJSONObject(i);

                                    String id = client_contact.getString("id");
                                    String client_id = client_contact.getString("client_id");
                                    String phone = client_contact.getString("phone");
                                    String change_time = client_contact.getString("change_time");

                                    values.put(DBHelper.KEY_ID, id);
                                    values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                    values.put(DBHelper.KEY_PHONE, phone);
                                    values.put(DBHelper.KEY_CHANGE_TIME, change_time);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_clients_contacts" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, values, "_id = ?", new String[]{id});

                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_clients_contacts " + e);
                            }

                            try {
                                JSONArray rgzbn_gm_ceiling_clients_dop_contacts = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients_dop_contacts");
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_clients_dop_contacts " + rgzbn_gm_ceiling_clients_dop_contacts.length());
                                for (int i = 0; i < rgzbn_gm_ceiling_clients_dop_contacts.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject client_dop_contact = rgzbn_gm_ceiling_clients_dop_contacts.getJSONObject(i);


                                    String id = client_dop_contact.getString("id");
                                    String client_id = client_dop_contact.getString("client_id");
                                    String type_id = client_dop_contact.getString("type_id");
                                    String contact = client_dop_contact.getString("contact");
                                    String change_time = client_dop_contact.getString("change_time");

                                    values.put(DBHelper.KEY_ID, id);
                                    values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                    values.put(DBHelper.KEY_TYPE_ID, type_id);
                                    values.put(DBHelper.KEY_CONTACT, contact);
                                    values.put(DBHelper.KEY_CHANGE_TIME, change_time);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_clients_dop_contacts" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS, values, "_id = ?", new String[]{id});

                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_clients_dop_contacts " + e);
                            }

                            try {
                                JSONArray rgzbn_gm_ceiling_callback = jsonObject.getJSONArray("rgzbn_gm_ceiling_callback");
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_callback " + rgzbn_gm_ceiling_callback.length());
                                for (int i = 0; i < rgzbn_gm_ceiling_callback.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject callback = rgzbn_gm_ceiling_callback.getJSONObject(i);


                                    String id = callback.getString("id");
                                    String client_id = callback.getString("client_id");
                                    String date_time = callback.getString("date_time");
                                    String comment = callback.getString("comment");
                                    String manager_id = callback.getString("manager_id");
                                    String notify = callback.getString("notify");
                                    String change_time = callback.getString("change_time");

                                    values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                    values.put(DBHelper.KEY_DATE_TIME, date_time);
                                    values.put(DBHelper.KEY_COMMENT, comment);
                                    values.put(DBHelper.KEY_MANAGER_ID, manager_id);
                                    values.put(DBHelper.KEY_NOTIFY, notify);
                                    values.put(DBHelper.KEY_CHANGE_TIME, change_time);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_callback" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, values,
                                                    "_id = ?", new String[]{id});


                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLBACK, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_callback " + e);
                            }

                            try {
                                JSONArray rgzbn_gm_ceiling_client_history = jsonObject.getJSONArray("rgzbn_gm_ceiling_client_history");
                                for (int i = 0; i < rgzbn_gm_ceiling_client_history.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject client_history = rgzbn_gm_ceiling_client_history.getJSONObject(i);

                                    String id = client_history.getString("id");
                                    String client_id = client_history.getString("client_id");
                                    String date_time = client_history.getString("date_time");
                                    String text = client_history.getString("text");
                                    String type_id = client_history.getString("type_id");
                                    String change_time = client_history.getString("change_time");

                                    values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                    values.put(DBHelper.KEY_DATE_TIME, date_time);
                                    values.put(DBHelper.KEY_TEXT, text);
                                    values.put(DBHelper.KEY_TYPE_ID, type_id);
                                    values.put(DBHelper.KEY_CHANGE_TIME, change_time);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_client_history" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, values,
                                                    "_id = ?", new String[]{id});

                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_client_history " + e);
                            }

                            try {
                                JSONArray rgzbn_gm_ceiling_calls_status_history = jsonObject.getJSONArray("rgzbn_gm_ceiling_calls_status_history");
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_calls_status_history " + rgzbn_gm_ceiling_calls_status_history.length());
                                for (int i = 0; i < rgzbn_gm_ceiling_calls_status_history.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject status_history = rgzbn_gm_ceiling_calls_status_history.getJSONObject(i);

                                    String id = status_history.getString("id");
                                    String manager_id = status_history.getString("manager_id");
                                    String client_id = status_history.getString("client_id");
                                    String status = status_history.getString("status");
                                    String call_length = status_history.getString("call_length");
                                    String change_time = status_history.getString("change_time");

                                    values.put(DBHelper.KEY_MANAGER_ID, manager_id);
                                    values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                    values.put(DBHelper.KEY_STATUS, status);
                                    values.put(DBHelper.KEY_CALL_LENGTH, call_length);
                                    values.put(DBHelper.KEY_CHANGE_TIME, change_time);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_calls_status_history" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, values,
                                                    "_id = ?", new String[]{id});

                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_calls_status_history" + e);
                            }


                            try {
                                JSONArray rgzbn_gm_ceiling_calls_status = jsonObject.getJSONArray("rgzbn_gm_ceiling_calls_status");
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_calls_status " + rgzbn_gm_ceiling_calls_status.length());
                                for (int i = 0; i < rgzbn_gm_ceiling_calls_status.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject status_history = rgzbn_gm_ceiling_calls_status.getJSONObject(i);

                                    String id = status_history.getString("id");
                                    String title = status_history.getString("title");
                                    String change_time = status_history.getString("change_time");

                                    values.put(DBHelper.KEY_TITLE, title);
                                    values.put(DBHelper.KEY_CHANGE_TIME, change_time);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_calls_status" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS, values,
                                                    "_id = ?", new String[]{id});


                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_calls_status " + e);
                            }


                            try {
                                JSONArray rgzbn_gm_ceiling_clients_statuses = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients_statuses");
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_clients_statuses " + rgzbn_gm_ceiling_clients_statuses.length());
                                for (int i = 0; i < rgzbn_gm_ceiling_clients_statuses.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject status = rgzbn_gm_ceiling_clients_statuses.getJSONObject(i);

                                    String id = status.getString("id");
                                    String title = status.getString("title");
                                    String dealer_id = status.getString("dealer_id");
                                    String change_time = status.getString("change_time");

                                    values.put(DBHelper.KEY_TITLE, title);
                                    values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                                    values.put(DBHelper.KEY_CHANGE_TIME, change_time);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_clients_statuses" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, values,
                                                    "_id = ?", new String[]{id});


                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_clients_statuses " + e);
                            }

                            try {
                                JSONArray rgzbn_gm_ceiling_clients_statuses_map = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients_statuses_map");
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_clients_statuses_map " + rgzbn_gm_ceiling_clients_statuses_map.length());
                                for (int i = 0; i < rgzbn_gm_ceiling_clients_statuses_map.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject status = rgzbn_gm_ceiling_clients_statuses_map.getJSONObject(i);

                                    String id = status.getString("id");
                                    String client_id = status.getString("client_id");
                                    String status_id = status.getString("status_id");
                                    String change_time = status.getString("change_time");

                                    values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                    values.put(DBHelper.KEY_STATUS_ID, status_id);
                                    values.put(DBHelper.KEY_CHANGE_TIME, change_time);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_clients_statuses_map" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, values,
                                                    "_id = ?", new String[]{id});


                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse:  rgzbn_gm_ceiling_clients_statuses_map " + e);
                            }

                            try {
                                JSONArray rgzbn_gm_ceiling_api_phones = jsonObject.getJSONArray("rgzbn_gm_ceiling_api_phones");
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_api_phones " + rgzbn_gm_ceiling_api_phones.length());
                                for (int i = 0; i < rgzbn_gm_ceiling_api_phones.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject api_p = rgzbn_gm_ceiling_api_phones.getJSONObject(i);

                                    String id = api_p.getString("id");
                                    String number = api_p.getString("number");
                                    String name = api_p.getString("name");
                                    String description = api_p.getString("description");
                                    String site = api_p.getString("site");
                                    String dealer_id = api_p.getString("dealer_id");
                                    String change_time = api_p.getString("change_time");

                                    values.put(DBHelper.KEY_NUMBER, number);
                                    values.put(DBHelper.KEY_NAME, name);
                                    values.put(DBHelper.KEY_DESCRIPTION, description);
                                    values.put(DBHelper.KEY_SITE, site);
                                    values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                                    values.put(DBHelper.KEY_CHANGE_TIME, change_time);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_api_phones" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_API_PHONES, values,
                                                    "_id = ?", new String[]{id});


                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_API_PHONES, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: rgzbn_gm_ceiling_api_phones " + e);
                            }

                            try {
                                JSONArray rgzbn_users = jsonObject.getJSONArray("rgzbn_users");
                                Log.d(TAG, "onResponse: rgzbn_users " + rgzbn_users.length());
                                for (int i = 0; i < rgzbn_users.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject user_v = rgzbn_users.getJSONObject(i);

                                    String id = user_v.getString("id");
                                    String name = user_v.getString("name");
                                    String username = user_v.getString("username");
                                    String email = user_v.getString("email");
                                    String dealer_id = user_v.getString("dealer_id");
                                    String settings = user_v.getString("settings");
                                    String change_time = user_v.getString("change_time");

                                    values.put(DBHelper.KEY_NAME, name);
                                    values.put(DBHelper.KEY_USERNAME, username);
                                    values.put(DBHelper.KEY_EMAIL, email);
                                    values.put(DBHelper.KEY_DEALER_ID, dealer_id);
                                    values.put(DBHelper.KEY_SETTINGS, settings);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_users" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_USERS, values,
                                                    "_id = ?", new String[]{id});


                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_USERS, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: rgzbn_users " + e);
                            }

                            try {
                                JSONArray messenger_types = jsonObject.getJSONArray("rgzbn_gm_ceiling_messenger_types");
                                Log.d(TAG, "onResponse: messenger_types " + messenger_types.length());
                                for (int i = 0; i < messenger_types.length(); i++) {

                                    values = new ContentValues();
                                    org.json.JSONObject user_v = messenger_types.getJSONObject(i);

                                    String id = user_v.getString("id");
                                    String title = user_v.getString("title");
                                    String change_time = user_v.getString("change_time");

                                    values.put(DBHelper.KEY_ID, id);
                                    values.put(DBHelper.KEY_TITLE, title);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_messenger_types" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_CEILING_MESSENGER_TYPES, values,
                                                    "_id = ?", new String[]{id});


                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_CEILING_MESSENGER_TYPES, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: " + e);
                            }


                            try {
                                JSONArray clients_labels = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients_labels");
                                Log.d(TAG, "onResponse: clients_labels " + clients_labels.length());
                                for (int i = 0; i < clients_labels.length(); i++) {

                                    values = new ContentValues();
                                    JSONObject user_v = clients_labels.getJSONObject(i);

                                    String id = user_v.getString("id");
                                    String title = user_v.getString("title");
                                    String color_code = user_v.getString("color_code");
                                    String dealer_id = user_v.getString("dealer_id");
                                    String change_time = user_v.getString("change_time");

                                    values.put(DBHelper.KEY_ID, id);
                                    values.put(DBHelper.KEY_TITLE, title);
                                    values.put(DBHelper.KEY_COLOR_CODE, color_code);
                                    values.put(DBHelper.KEY_DEALER_ID, dealer_id);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_clients_labels" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_CEILING_CLIENTS_LABELS, values,
                                                    "_id = ?", new String[]{id});


                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_CEILING_CLIENTS_LABELS, null, values);

                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                            try {
                                JSONArray clients_labels_history = jsonObject.getJSONArray("rgzbn_gm_ceiling_clients_labels_history");
                                Log.d(TAG, "onResponse: clients_labels_history " + clients_labels_history.length());
                                for (int i = 0; i < clients_labels_history.length(); i++) {

                                    values = new ContentValues();
                                    JSONObject user_v = clients_labels_history.getJSONObject(i);

                                    String id = user_v.getString("id");
                                    String client_id = user_v.getString("client_id");
                                    String label_id = user_v.getString("label_id");
                                    String change_time = user_v.getString("change_time");

                                    values.put(DBHelper.KEY_ID, id);
                                    values.put(DBHelper.KEY_CLIENT_ID, client_id);
                                    values.put(DBHelper.KEY_LABEL_ID, label_id);

                                    String sqlQuewy = "SELECT * "
                                            + "FROM rgzbn_gm_ceiling_clients_labels_history" +
                                            " WHERE _id = ?";
                                    Cursor c = db.rawQuery(sqlQuewy, new String[]{id});
                                    if (c != null) {
                                        if (c.moveToFirst()) {
                                            db.update(DBHelper.TABLE_RGZBN_CEILING_CLIENTS_LABELS_HISTORY, values,
                                                    "_id = ?", new String[]{id});

                                            change_max = compareDate(change_max, change_time);
                                        } else {
                                            values.put(DBHelper.KEY_ID, id);
                                            db.insert(DBHelper.TABLE_RGZBN_CEILING_CLIENTS_LABELS_HISTORY, null, values);
                                            change_max = compareDate(change_max, change_time);
                                        }
                                    }
                                    c.close();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            SimpleDateFormat out_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                            values = new ContentValues();
                            values.put(DBHelper.KEY_CHANGE_TIME, String.valueOf(out_format.format(change_max)));
                            db.update(DBHelper.HISTORY_IMPORT_TO_SERVER, values, "user_id = ?", new String[]{user_id});

                            Log.d(TAG, "NEW change_time: " + String.valueOf(out_format.format(change_max)));

                        } catch (Exception e) {
                            Log.d(TAG, "onResponse:er " + e);
                        }

                    }

                    pd.dismiss();
                    finish();
                    intent = new Intent(AuthorizationActivity.this, MainActivity.class);
                    startActivity(intent);
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    pd.dismiss();
                    finish();
                    intent = new Intent(AuthorizationActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            }) {

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Log.d(TAG, String.valueOf(parameters));
                    return parameters;
                }
            };

            requestQueue.add(request);

            return null;
        }
    }

    static private Date compareDate(Date change_max, String change_time){
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date change = null;
        try {
            change = ft.parse(change_time);
            if (change_max.getTime() < change.getTime()) {
                change_max = change;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return change_max;
    }

}