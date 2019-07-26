package ru.itceiling.telephony.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.itceiling.telephony.App;
import ru.itceiling.telephony.R;

public class SubscriptionsActivity extends AppCompatActivity {

    /*static String TAG = "ImportLog";

    private static final List<String> SKUS = Arrays.asList("telephony.subscription.1month", "telephony.subscription.6month");

    private final List<Inventory.Callback> mInventoryCallbacks = new ArrayList<>();

    @BindView(R.id.buy)
    View mBuy;
    @BindView(R.id.available_skus)
    Spinner mAvailableSkus;

    @BindView(R.id.purchased_skus)
    RecyclerView mPurchasedSkus;
    @BindView(R.id.change)
    View mChange;
    @BindView(R.id.target_skus)
    Spinner mTargetSkus;
    private PurchasedSkusAdapter mPurchasedSkusAdapter;
    private TargetSkusAdapter mTargetSkusAdapter;

    private ActivityCheckout mCheckout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_subscriptions);
        ButterKnife.bind(this);

        initAvailableSkus();
        initPurchasedSkus();
        initTargetSkus();

        final Billing billing = App.get().getBilling();
        mCheckout = Checkout.forActivity(this, billing);
        mCheckout.start();
        reloadInventory();

    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        mPurchasedSkusAdapter.saveSate(out);
        super.onSaveInstanceState(out);
    }

    @Override
    protected void onRestoreInstanceState(Bundle in) {
        super.onRestoreInstanceState(in);
        mPurchasedSkusAdapter.restoreState(in);
    }

    private void reloadInventory() {
        final Inventory.Request request = Inventory.Request.create();
        request.loadPurchases(SUBSCRIPTION);
        request.loadSkus(SUBSCRIPTION, SKUS);
        mCheckout.loadInventory(request, new Inventory.Callback() {
            @Override
            public void onLoaded(@Nonnull Inventory.Products products) {
                for (Inventory.Callback callback : mInventoryCallbacks) {
                    callback.onLoaded(products);
                }
            }
        });
    }

    private void initTargetSkus() {
        mTargetSkusAdapter = new TargetSkusAdapter(this);
        mTargetSkus.setAdapter(mTargetSkusAdapter);
        mTargetSkusAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                updateTargetSkusVisibility();
            }
        });
        mChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Set<Sku> checked = mPurchasedSkusAdapter.getChecked();
                if (checked.isEmpty()) {
                    return;
                }
                final int position = mTargetSkus.getSelectedItemPosition();
                if (position >= 0 && position < mTargetSkusAdapter.getCount()) {
                    final SkuItem item = mTargetSkusAdapter.getItem(position);
                    if (item != null) {
                        change(checked, item.mSku);
                    }
                }
            }
        });
        mInventoryCallbacks.add(mTargetSkusAdapter);
    }

    private void updateTargetSkusVisibility() {
        final boolean enabled = mTargetSkusAdapter.getCount() > 0 && mPurchasedSkusAdapter.getChecked().size() > 0;
        mChange.setEnabled(enabled);
        mTargetSkus.setEnabled(enabled);
    }

    private void initPurchasedSkus() {
        mPurchasedSkusAdapter = new PurchasedSkusAdapter(SubscriptionsActivity.this, new PurchasedSkusAdapter.Listener() {
            @Override
            public void onCheckedChanged() {
                updateTargetSkusVisibility();
            }
        });
        mPurchasedSkus.setAdapter(mPurchasedSkusAdapter);
        mPurchasedSkus.setLayoutManager(new LinearLayoutManager(this));
        mInventoryCallbacks.add(mPurchasedSkusAdapter);
    }

    private void initAvailableSkus() {
        final AvailableSkusAdapter adapter = new AvailableSkusAdapter(this);
        mAvailableSkus.setAdapter(adapter);
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                Log.d(TAG, "adapter.getCount: " + adapter.getCount());
                mBuy.setEnabled(adapter.getCount() > 0);
            }
        });
        mBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = mAvailableSkus.getSelectedItemPosition();
                if (position >= 0 && position < adapter.getCount()) {
                    final SkuItem item = adapter.getItem(position);
                    if (item != null) {
                        purchase(item.mSku);
                    }
                }
            }
        });
        mInventoryCallbacks.add(adapter);
    }

    private void purchase(Sku sku) {
        mCheckout.startPurchaseFlow(sku, null, new PurchaseListener());
    }

    private void change(final Set<Sku> old, final Sku sku) {
        mCheckout.whenReady(new Checkout.EmptyListener() {
            @Override
            public void onReady(@Nonnull BillingRequests requests) {
                final PurchaseFlow flow = mCheckout.createOneShotPurchaseFlow(new PurchaseListener());
                requests.changeSubscription(new ArrayList<>(old), sku, null, flow);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, "onActivityResult: " + requestCode + " " + resultCode);

        mCheckout.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        mCheckout.stop();
        super.onDestroy();
    }

    private static class SkuItem {
        private final Sku mSku;

        private SkuItem(Sku sku) {
            mSku = sku;
        }

        @Override
        public String toString() {
            return mSku.getDisplayTitle();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener {

        private final PurchasedSkusAdapter mAdapter;
        @BindView(R.id.subscription)
        CheckBox mSubscription;
        @Nullable
        private Sku mSku;

        ViewHolder(View view, PurchasedSkusAdapter adapter) {
            super(view);
            mAdapter = adapter;

            ButterKnife.bind(this, view);
            mSubscription.setOnCheckedChangeListener(this);
        }

        public void onBind(Sku sku) {
            mSku = sku;
            mSubscription.setText(sku.getDisplayTitle());
            mSubscription.setChecked(mAdapter.isChecked(sku));
        }

        @Override
        public void onCheckedChanged(CompoundButton v, boolean checked) {
            if (mSku == null) {
                return;
            }
            mAdapter.onChecked(mSku, checked);
        }
    }

    // то, что можно купить
    private static class AvailableSkusAdapter extends ArrayAdapter<SkuItem> implements Inventory.Callback {
        public AvailableSkusAdapter(Context context) {
            super(context, R.layout.support_simple_spinner_dropdown_item);
        }

        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            final Inventory.Product product = products.get(SUBSCRIPTION);

            setNotifyOnChange(false);
            clear();
            for (Sku sku : product.getSkus()) {
                if (!product.isPurchased(sku)) {
                    add(new SkuItem(sku));
                    Log.d("logd", "AvailableSkusAdapter: " + sku.title);
                }
            }
            notifyDataSetChanged();
        }
    }

    // изменить подписку
    private static class TargetSkusAdapter extends ArrayAdapter<SkuItem> implements Inventory.Callback {
        public TargetSkusAdapter(Context context) {
            super(context, R.layout.support_simple_spinner_dropdown_item);
        }
        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            final Inventory.Product product = products.get(SUBSCRIPTION);

            setNotifyOnChange(false);
            clear();
            for (Sku sku : product.getSkus()) {
                if (!product.isPurchased(sku)) {
                    add(new SkuItem(sku));
                    Log.d("logd", "TargetSkusAdapter: " + sku.title);
                }
            }
            notifyDataSetChanged();
        }
    }

    //которые уже есть
    private static class PurchasedSkusAdapter extends RecyclerView.Adapter<ViewHolder> implements Inventory.Callback {
        private final LayoutInflater mInflater;
        private final Listener mListener;
        private final List<Sku> mSkus = new ArrayList<>();
        private final Set<Sku> mChecked = new HashSet<>();
        private final List<String> mRestoredChecked = new ArrayList<>();
        private boolean mLoaded;

        private PurchasedSkusAdapter(Activity activity, Listener listener) {
            Log.d(TAG, "PurchasedSkusAdapter: ");
            mInflater = LayoutInflater.from(activity);
            mListener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(R.layout.subscription, parent, false);
            return new ViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Sku sku = mSkus.get(position);
            holder.onBind(sku);
        }

        @Override
        public int getItemCount() {
            return mSkus.size();
        }

        private boolean updateCheckedSkus() {
            if (mLoaded && !mRestoredChecked.isEmpty()) {
                for (String restored : mRestoredChecked) {
                    final Sku sku = getSku(restored);
                    if (sku != null) {
                        Log.d("logd", "updateCheckedSkus: " + sku.title);
                        mChecked.add(sku);
                    }
                }
                mRestoredChecked.clear();
                return !mChecked.isEmpty();
            }

            boolean changed = false;
            final Iterator<Sku> it = mChecked.iterator();
            while (it.hasNext()) {
                final Sku checked = it.next();
                if (!mSkus.contains(checked)) {
                    it.remove();
                    changed = true;
                }
            }
            return changed;
        }

        @Nullable
        private Sku getSku(String code) {
            for (Sku sku : mSkus) {
                if (sku.id.code.equals(code)) {
                    return sku;
                }
            }
            return null;
        }

        public void onChecked(Sku sku, boolean checked) {
            final int before = mChecked.size();
            if (checked) {
                mChecked.add(sku);
            } else {
                mChecked.remove(sku);
            }
            final int after = mChecked.size();
            if (before != after) {
                mListener.onCheckedChanged();
            }
        }

        public boolean isChecked(Sku sku) {
            return mChecked.contains(sku);
        }

        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            mLoaded = true;

            // update the list
            final Inventory.Product product = products.get(SUBSCRIPTION);
            mSkus.clear();
            for (Purchase purchase : product.getPurchases()) {
                if (purchase.state != Purchase.State.PURCHASED) {
                    continue;
                }
                final Sku sku = product.getSku(purchase.sku);

                if (sku != null) {
                    mSkus.add(sku);
                    Log.d(TAG, "onLoaded: " + sku.title);
                }
            }
            // keep checked status if possible & do initial restore if needed
            final boolean checkedChanged = updateCheckedSkus();

            notifyDataSetChanged();
            if (checkedChanged) {
                mListener.onCheckedChanged();
            }
        }

        public Set<Sku> getChecked() {
            return mChecked;
        }

        public void saveSate(Bundle out) {
            if (mChecked.isEmpty()) {
                return;
            }
            final ArrayList<String> checked = new ArrayList<>();
            for (Sku sku : mChecked) {
                checked.add(sku.id.code);
            }
            out.putStringArrayList("checked", checked);
        }

        public void restoreState(Bundle in) {
            mRestoredChecked.clear();
            final ArrayList<String> checked = in.getStringArrayList("checked");
            if (checked != null) {
                mRestoredChecked.addAll(checked);
                updateCheckedSkus();
            }
        }

        interface Listener {
            void onCheckedChanged();
        }
    }

    private class PurchaseListener implements RequestListener<Purchase> {
        @Override
        public void onSuccess(@Nonnull Purchase result) {
            Log.d(TAG, "onSuccess: ");
            reloadInventory();
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
            Log.d(TAG, "onError: " + e);
            reloadInventory();
        }
    }*/

}