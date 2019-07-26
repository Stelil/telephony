package ru.itceiling.telephony.broadcaster;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;

import ru.itceiling.telephony.AsyncTaskVK;

public class VKReceiver {

    private String TAG = "vkRec";
    private VKAccessToken token = null;
    private AsyncTaskVK asyncRequest;
    private Context context;
    private boolean bool = false;

    public VKReceiver(Context context, boolean bool) {
        AsyncTaskVK asyncTaskVK = new AsyncTaskVK();
        if (!asyncTaskVK.isBool()) {
            this.context = context;
            token = VKAccessToken.currentToken();
            new GetGroups().execute();
            this.bool = bool;
        }
    }

    private class GetGroups extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            final VKRequest request = new VKRequest("groups.get", VKParameters.from(
                    VKApiConst.ACCESS_TOKEN, token.accessToken,
                    VKApiConst.USER_ID, token.userId,
                    "filter", "admin",
                    VKApiConst.EXTENDED, 1));

            request.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);
                    JSONArray groups = null;
                    try {
                        groups = response.json.getJSONObject("response").getJSONArray("items");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; groups.length() > i; i++) {
                        try {
                            org.json.JSONObject group = groups.getJSONObject(i);
                            Log.d(TAG, "onComplete: " + group.get("name"));
                            if (true) {
                                dialogCheckConversations((int) group.get("id"));
                            } else {
                                //new StartServer().execute((int) group.get("id"));
                            }
                        } catch (JSONException e) {
                            Log.d(TAG, "onComplete: " + e);
                        }
                    }
                }
            });
            return null;
        }
    }

    private void dialogCheckConversations(final int group_id) {
        String title = "Загрузить диалоги из сообщества?";
        String message = "Диалоги добавятся в виде клиентов";
        String button1String = "Да";
        String button2String = "Нет";
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(button1String, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                new GetConversations().execute(group_id);
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

    private class GetConversations extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... integers) {

            VKRequest request = null;
            for (Integer integ : integers) {
                request = new VKRequest("messages.getConversations",
                        VKParameters.from(
                                VKApiConst.GROUP_ID, integ,
                                "filter", "all"));
            }

            request.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);
                    Log.d(TAG, "onComplete: " + response.json.toString());
                }
            });
            return null;
        }
    }

    private class StartServer extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... integers) {
            VKRequest request = null;
            for (Integer integ : integers) {
                request = new VKRequest("groups.getLongPollServer", VKParameters.from(
                        VKApiConst.GROUP_ID, integ));
            }

            request.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);

                    Log.d(TAG, "onComplete: " + response.json.toString());

                    asyncRequest = new AsyncTaskVK(response, context);
                }
            });
            return null;
        }
    }
}