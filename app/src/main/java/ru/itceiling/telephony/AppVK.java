package ru.itceiling.telephony;

import com.vk.sdk.VKSdk;

public class AppVK extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(this);
    }

}