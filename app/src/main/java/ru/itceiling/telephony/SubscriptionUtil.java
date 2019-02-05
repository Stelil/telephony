package ru.itceiling.telephony;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

import ru.itceiling.telephony.util.IabHelper;
import ru.itceiling.telephony.util.IabResult;
import ru.itceiling.telephony.util.Inventory;
import ru.itceiling.telephony.util.Purchase;
import ru.itceiling.telephony.util.SkuDetails;

/**
 * Created by hrskrs on 5/5/2016.
 */
public class SubscriptionUtil extends Application {
    private static final int REQUEST_CODE = 10001;
    private static final String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk8r2NGH67E3qbNE6nPxZgB09Z4RsbeIS25MwjS0dowuE5XX5Y+xy2sbpFyqC8Ja3VSEr3vbRQqYlSnVhEXY9+WxAnv3VbibSM78o/Ru3eBHwVPIzrTbcPLW84PY/RUYxRm9CIhLr/06WKHmiy8Wd3EpsWb4YFVlZnPe35nho6R9+xHfNbH+8ilqmsLNlyLnerQ7T7mpGXYoWssgOd0wwyKKyN05mopD5jRw1JnmbFa8jomahgg39AJznMyXvaaDGgl9OOGJr2y7v4wU9HGogN+4GovKsQg1NB6fhfiH5c/ci2AfVKLEBOZsA8yTycCIwbAVCqS2cp7u9rATo+WktFQIDAQAB";

    private IabHelper iabHelper;
    private Context context;

    String TAG = "logdSub";

    public SubscriptionUtil() {
        //No instance
    }

    public SubscriptionUtil(Context context) {
        this.context = context;
        iabHelper = new IabHelper(context, base64EncodedPublicKey);
        iabHelper.enableDebugLogging(true, "logdHelper");
        setup();
    }

    private void setup() {
        Log.d(TAG, "setup: ");
        if (iabHelper != null) {
            iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (result.isFailure()) {
                        Log.d(TAG, "Problem setting up In-app Billing: " + result);
                        dispose();
                    }
                }
            });
        }
    }

    public void initSubscription(final String subscriptionType,
                                 SubscriptionFinishedListener subscriptionFinishedListener) {
        initSubscriptionWithExtras(subscriptionType, subscriptionFinishedListener, "");
    }

    public void initSubscriptionWithExtras(final String subscriptionType,
                                           final SubscriptionFinishedListener subscriptionFinishedListener,
                                           String payload) {
        if (iabHelper != null) {
            try {
                iabHelper.launchSubscriptionPurchaseFlow((Activity) context,
                        subscriptionType,
                        REQUEST_CODE,
                        new IabHelper.OnIabPurchaseFinishedListener() {
                            @Override
                            public void onIabPurchaseFinished(IabResult result, Purchase info) {
                                if (result.isFailure()) {
                                    Log.e(TAG, "Error purchasing: " + result);
                                    return;
                                }
                                if (info.getSku().equals(subscriptionType)) {
                                    if(subscriptionFinishedListener != null){
                                        subscriptionFinishedListener.onSuccess();
                                    }
                                    Log.e(TAG, "Thank you for upgrading to premium!");
                                }
                            }
                        },
                        payload
                );
            } catch (IabHelper.IabAsyncInProgressException e) {
                e.printStackTrace();
            }
            //In case you get below error:
            //`Can't start async operation (refresh inventory) because another async operation (launchPurchaseFlow) is in progress.`
            //Include this line of code to end proccess after purchase
            //iabHelper.flagEndAsync();
        }
    }

    public void getSkuDetailsList(
            final ArrayList<String> skuIdsList,
            final SubscriptionInventoryListener subscriptionInventoryListener
    ) {
        if (iabHelper != null) {
            try {
                iabHelper.queryInventoryAsync(true, null, skuIdsList,
                        new IabHelper.QueryInventoryFinishedListener() {
                    @Override
                    public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                        if (result.isFailure()) {
                            Log.d(TAG, "Problem querying inventory: " + result);
                            dispose();
                            return;
                        }
                        ArrayList<SkuDetails> skuDetailsList = new ArrayList<>();
                        for (String skuId : skuIdsList) {
                            SkuDetails sku = inventory.getSkuDetails(skuId);
                            if (sku.getSku().equals(skuId)) {
                                skuDetailsList.add(sku);
                                sku.getPrice();
                            }
                        }

                        if (subscriptionInventoryListener != null) {
                            subscriptionInventoryListener.onQueryInventoryFinished(skuDetailsList);
                        }
                    }
                });
            } catch (IabHelper.IabAsyncInProgressException e) {
                Log.e(TAG, "EXCEPTION:" + e.getMessage());
            }
        }

    }

    public void dispose() {
        if (iabHelper != null) {
            try {
                iabHelper.dispose();
            } catch (IabHelper.IabAsyncInProgressException e) {
                e.printStackTrace();
            }
            iabHelper = null;
        }
    }

    public IabHelper getIabHelper() {
        if (iabHelper == null) {
            iabHelper = new IabHelper(context, base64EncodedPublicKey);
        }
        return iabHelper;
    }

    public interface SubscriptionInventoryListener {
        void onQueryInventoryFinished(ArrayList<SkuDetails> skuList);
    }

    public interface SubscriptionFinishedListener{
        void onSuccess();
    }
}
