package ru.itceiling.telephony;


import android.app.Application;

import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.PlayStoreListener;

import javax.annotation.Nonnull;

public class App extends Application {


    @Nonnull
    private final Billing mBilling = new Billing(this, new Billing.DefaultConfiguration() {
        @Nonnull
        @Override
        public String getPublicKey() {
            final String s = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk8r2NGH67E3qbNE6nPxZgB09Z4RsbeIS25MwjS0dowuE5XX5Y+xy2sbpFyqC8Ja3VSEr3vbRQqYlSnVhEXY9+WxAnv3VbibSM78o/Ru3eBHwVPIzrTbcPLW84PY/RUYxRm9CIhLr/06WKHmiy8Wd3EpsWb4YFVlZnPe35nho6R9+xHfNbH+8ilqmsLNlyLnerQ7T7mpGXYoWssgOd0wwyKKyN05mopD5jRw1JnmbFa8jomahgg39AJznMyXvaaDGgl9OOGJr2y7v4wU9HGogN+4GovKsQg1NB6fhfiH5c/ci2AfVKLEBOZsA8yTycCIwbAVCqS2cp7u9rATo+WktFQIDAQAB";
            //return Encryption.decrypt(s, "se.solovyev@gmail.com");
            return s;
        }
    });

    @Nonnull
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
        mBilling.addPlayStoreListener(new PlayStoreListener() {
            @Override
            public void onPurchasesChanged() {
            }
        });
    }

    @Nonnull
    public Billing getBilling() {
        return mBilling;
    }
}