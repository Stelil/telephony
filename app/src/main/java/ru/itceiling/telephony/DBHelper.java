package ru.itceiling.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 7;
    public static final String DATABASE_NAME = "dbTelephony";

    private Context mContext;

    public static final String TABLE_RGZBN_GM_CEILING_CLIENTS = "rgzbn_gm_ceiling_clients";
    public static final String KEY_ID = "_id";
    public static final String KEY_CLIENT_NAME = "client_name";
    public static final String KEY_CLIENT_DATA_ID = "client_data_id";
    public static final String KEY_TYPE_ID = "type_id";
    public static final String KEY_DEALER_ID = "dealer_id";
    public static final String KEY_MANAGER_ID = "manager_id";
    public static final String KEY_CREATED = "created";
    public static final String KEY_SEX = "sex";
    public static final String KEY_DELETED_BY_USER = "deleted_by_user";
    public static final String KEY_API_PHONE_ID = "api_phone_id";

    public static final String TABLE_RGZBN_GM_CEILING_CLIENTS_CONTACTS = "rgzbn_gm_ceiling_clients_contacts";
    public static final String KEY_CLIENT_ID = "client_id";
    public static final String KEY_PHONE = "phone";

    public static final String TABLE_RGZBN_GM_CEILING_CLIENTS_DOP_CONTACTS = "rgzbn_gm_ceiling_clients_dop_contacts";
    public static final String KEY_CONTACT = "contact";

    public static final String TABLE_RGZBN_GM_CEILING_CLIENT_HISTORY = "rgzbn_gm_ceiling_client_history";
    public static final String KEY_DATE_TIME = "date_time";
    public static final String KEY_TEXT = "text";

    public static final String TABLE_RGZBN_GM_CEILING_CALLBACK = "rgzbn_gm_ceiling_callback";
    public static final String KEY_COMMENT = "comment";
    public static final String KEY_NOTIFY = "notify";

    public static final String TABLE_RGZBN_GM_CEILING_CALLS_STATUS_HISTORY = "rgzbn_gm_ceiling_calls_status_history";
    public static final String KEY_STATUS = "status";
    public static final String KEY_CALL_LENGTH = "call_length";

    public static final String TABLE_USERS = "rgzbn_users";
    public static final String KEY_NAME = "name";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_ASSOCIATED_CLIENT = "associated_client";
    public static final String KEY_DEALER_TYPE = "dealer_type";
    public static final String KEY_REFUSED_TO_COOPERATE = "refused_to_cooperate";
    public static final String KEY_BLOCK = "block";
    public static final String KEY_SENDEMAIL = "sendEmail";
    public static final String KEY_REGISTERDATE = "registerDate";
    public static final String KEY_LASTVISITDATE = "lastvisitDate";
    public static final String KEY_ACTIVATION = "activation";
    public static final String KEY_PARAMS = "params";
    public static final String KEY_LASTRESETTIME = "lastResetTime";
    public static final String KEY_RESETCOUNT = "resetCount";
    public static final String KEY_OTPKEY = "otpKey";
    public static final String KEY_OTEP = "otep";
    public static final String KEY_REQUIRERESET = "requireReset";
    public static final String KEY_DISCOUNT = "discount";
    public static final String KEY_WAGES = "wages";
    public static final String KEY_CHANGE_TIME = "change_time";
    public static final String KEY_DEALER_MOUNTERS = "dealer_mounters";
    public static final String KEY_DEMO_AND_DATE = "demo_end_date";
    public static final String KEY_SETTINGS = "settings";

    public static final String TABLE_RGZBN_GM_CEILING_CALLS_STATUS = "rgzbn_gm_ceiling_calls_status";
    public static final String KEY_TITLE = "title";

    public static final String TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES = "rgzbn_gm_ceiling_clients_statuses";

    public static final String TABLE_RGZBN_GM_CEILING_API_PHONES = "rgzbn_gm_ceiling_api_phones";
    public static final String KEY_NUMBER = "number";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_SITE = "site";

    public static final String HISTORY_IMPORT_TO_SERVER = "history_import_to_server";
    public static final String KEY_USER_ID = "user_id";

    public static final String TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES_MAP = "rgzbn_gm_ceiling_clients_statuses_map";
    public static final String KEY_STATUS_ID = "status_id";

    public static final String TABLE_RGZBN_USER_USERGROUP_MAP = "rgzbn_gm_user_usergroup_map";
    public static final String KEY_GROUP_ID = "group_id";

    public static final String HISTORY_SEND_TO_SERVER = "history_send_to_server";
    public static final String KEY_ID_NEW = "id_new";
    public static final String KEY_ID_OLD = "id_old";
    public static final String KEY_NAME_TABLE = "name_table";
    public static final String KEY_SYNC = "sync";
    public static final String KEY_TYPE = "type";
    public static final String KEY_DATE = "date";
    public static final String KEY_DATE_SYNC = "date_sync";

    public static final String TABLE_RGZBN_CEILING_MESSENGER_TYPES = "rgzbn_gm_ceiling_messenger_types";

    public static final String TABLE_RGZBN_CEILING_CLIENTS_LABELS = "rgzbn_gm_ceiling_clients_labels";
    public static final String KEY_COLOR_CODE = "color_code";

    public static final String TABLE_RGZBN_CEILING_CLIENTS_LABELS_HISTORY = "rgzbn_gm_ceiling_clients_labels_history";
    public static final String KEY_LABEL_ID = "label_id";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_clients (_id INTEGER, " +
                "client_name TEXT, client_data_id INTEGER, type_id INTEGER, dealer_id INTEGER, manager_id INTEGER, " +
                "created TEXT, sex TEXT, deleted_by_user INTEGER, api_phone_id INTEGER, label_id INTEGER, change_time TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_clients_contacts (_id INTEGER, " +
                "client_id INTEGER, phone TEXT, change_time TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_clients_dop_contacts (_id INTEGER, " +
                "client_id INTEGER, type_id INTEGER, contact TEXT, change_time TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_client_history (_id INTEGER, " +
                "client_id INTEGER, date_time TEXT, text TEXT, change_time TEXT, type_id INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_callback (_id INTEGER, " +
                "client_id INTEGER, date_time TEXT, comment TEXT, manager_id INTEGER, notify INTEGER, change_time TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_clients_statuses_map (_id INTEGER, " +
                " client_id TEXT, status_id TEXT, change_time TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_calls_status_history (_id INTEGER, " +
                "manager_id INTEGER, client_id INTEGER, status INTEGER, date_time TEXT, call_length TEXT, change_time TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_calls_status (_id INTEGER, title TEXT, change_time TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_clients_statuses (_id INTEGER, title TEXT," +
                " change_time TEXT, dealer_id INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_api_phones (_id INTEGER, number TEXT," +
                " name TEXT, description TEXT, site TEXT, dealer_id INTEGER, change_time TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_users (_id INTEGER, " +
                "name TEXT, username TEXT, email TEXT, password TEXT, dealer_id INTEGER, associated_client TEXT," +
                "dealer_type INTEGER, refused_to_cooperate INTEGER, block INTEGER, sendEmail INTEGER, registerDate TEXT, " +
                "lastvisitDate TEXT, activation TEXT, params TEXT, lastResetTime TEXT, resetCount INTEGER, otpKey TEXT, " +
                "otep TEXT, requireReset INTEGER, discount INTEGER,  wages TEXT, change_time TEXT, dealer_mounters INTEGER, " +
                "demo_end_date TEXT, settings TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS history_import_to_server (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, title TEXT, change_time TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS history_send_to_server (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_new INTEGER, id_old INTEGER, name_table TEXT, sync INTEGER, type TEXT, date TEXT, date_sync TEXT, " +
                "status TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_user_usergroup_map (_id INTEGER, user_id TEXT, group_id TEXT, " +
                "change_time TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_messenger_types (_id INTEGER, title TEXT, change_time TEXT)");


        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_clients_labels (_id INTEGER, title TEXT, color_code TEXT, " +
                "dealer_id INTEGER, change_time TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_clients_labels_history (_id INTEGER, client_id INTEGER, " +
                "label_id INTEGER, change_time TEXT)");


        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_ID, 0);
        values.put(DBHelper.KEY_TITLE, "Пропущенный звонок");
        values.put(DBHelper.KEY_CHANGE_TIME, "0000-00-00 00:00:00");
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS, null, values);

        values = new ContentValues();
        values.put(DBHelper.KEY_ID, 1);
        values.put(DBHelper.KEY_TITLE, "Недозвон");
        values.put(DBHelper.KEY_CHANGE_TIME, "0000-00-00 00:00:00");
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS, null, values);

        values = new ContentValues();
        values.put(DBHelper.KEY_ID, 2);
        values.put(DBHelper.KEY_TITLE, "Исходящий дозвон");
        values.put(DBHelper.KEY_CHANGE_TIME, "0000-00-00 00:00:00");
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS, null, values);

        values = new ContentValues();
        values.put(DBHelper.KEY_ID, 3);
        values.put(DBHelper.KEY_TITLE, "Входящий звонок");
        values.put(DBHelper.KEY_CHANGE_TIME, "0000-00-00 00:00:00");
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS, null, values);

        values = new ContentValues();
        values.put(DBHelper.KEY_ID, 1);
        values.put(DBHelper.KEY_TITLE, "Необработанный");
        values.put(DBHelper.KEY_DEALER_ID, "null");
        values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, null, values);

        values = new ContentValues();
        values.put(DBHelper.KEY_ID, 2);
        values.put(DBHelper.KEY_TITLE, "Договор");
        values.put(DBHelper.KEY_DEALER_ID, "null");
        values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, null, values);

        values = new ContentValues();
        values.put(DBHelper.KEY_ID, 3);
        values.put(DBHelper.KEY_TITLE, "В работе");
        values.put(DBHelper.KEY_DEALER_ID, "null");
        values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, null, values);

        values = new ContentValues();
        values.put(DBHelper.KEY_ID, 4);
        values.put(DBHelper.KEY_TITLE, "Отказ от сотрудничества");
        values.put(DBHelper.KEY_DEALER_ID, "null");
        values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, null, values);

        values = new ContentValues();
        values.put(DBHelper.KEY_ID, 5);
        values.put(DBHelper.KEY_TITLE, "Заказ закрыт");
        values.put(DBHelper.KEY_DEALER_ID, "null");
        values.put(DBHelper.KEY_CHANGE_TIME, HelperClass.nowDate());
        db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CLIENTS_STATUSES, null, values);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 2) { // 23.01
            ContentValues values = new ContentValues();
            values.put(DBHelper.KEY_ID, 0);
            values.put(DBHelper.KEY_TITLE, "Пропущенный звонок");
            values.put(DBHelper.KEY_CHANGE_TIME, "0000-00-00 00:00:00");
            db.insert(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS, null, values);
        }

        if (oldVersion < 3) { // 13.02
            db.execSQL("ALTER TABLE rgzbn_users ADD settings TEXT");
        }

        if (oldVersion < 4) { // 27.02
            ContentValues values = new ContentValues();
            values.put(DBHelper.KEY_TITLE, "Недозвон");
            db.update(DBHelper.TABLE_RGZBN_GM_CEILING_CALLS_STATUS, values, "_id = ?",
                    new String[]{"1"});
        }

        if (oldVersion < 6) {
            db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_messenger_types (_id INTEGER, title TEXT, change_time TEXT)");
            db.execSQL("ALTER TABLE rgzbn_gm_ceiling_client_history ADD type_id INTEGER");
        }

        if (oldVersion < 7) {
            db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_clients_labels (_id INTEGER, title TEXT, color_code TEXT, " +
                    "dealer_id INTEGER, change_time TEXT)");

            db.execSQL("CREATE TABLE IF NOT EXISTS rgzbn_gm_ceiling_clients_labels_history (_id INTEGER, client_id INTEGER, " +
                    "label_id INTEGER, change_time TEXT)");

            db.execSQL("ALTER TABLE rgzbn_gm_ceiling_clients ADD label_id INTEGER");
        }

    }
}
