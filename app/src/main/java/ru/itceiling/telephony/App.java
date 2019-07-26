package ru.itceiling.telephony;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKSdk;

import ru.itceiling.telephony.activity.SettingsActivity;

public class App extends Application {

    /*private final Billing mBilling = new Billing(this, new Billing.DefaultConfiguration() {
        @Nonnull
        @Override
        public String getPublicKey() {
            final String s = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk8r2NGH67E3qbNE6nPxZgB09Z4RsbeIS25MwjS0dowuE5XX5Y+xy2sbpFyqC8Ja3VSEr3vbRQqYlSnVhEXY9+WxAnv3VbibSM78o/Ru3eBHwVPIzrTbcPLW84PY/RUYxRm9CIhLr/06WKHmiy8Wd3EpsWb4YFVlZnPe35nho6R9+xHfNbH+8ilqmsLNlyLnerQ7T7mpGXYoWssgOd0wwyKKyN05mopD5jRw1JnmbFa8jomahgg39AJznMyXvaaDGgl9OOGJr2y7v4wU9HGogN+4GovKsQg1NB6fhfiH5c/ci2AfVKLEBOZsA8yTycCIwbAVCqS2cp7u9rATo+WktFQIDAQAB";
            //return Encryption.decrypt(s, "se.solovyev@gmail.com");
            return s;
        }
    });*/

    private static App instance;

    public App() {
        instance = this;
    }

    public static App get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        vkAccessTokenTracker.startTracking();
        VKSdk.initialize(this);
    }

    Context context;

    VKAccessTokenTracker vkAccessTokenTracker = new VKAccessTokenTracker() {
        @Override
        public void onVKAccessTokenChanged(VKAccessToken oldToken, VKAccessToken newToken) {
            Log.d("logd", "onVKAccessTokenChanged: " + newToken);
            if (newToken == null) {
                Toast.makeText(context, "AccessToken invalidated", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(context, SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }
    };

}