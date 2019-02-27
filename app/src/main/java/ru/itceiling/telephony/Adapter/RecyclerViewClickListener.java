package ru.itceiling.telephony.Adapter;

import android.view.View;

public interface RecyclerViewClickListener {
    void run();

    void recyclerViewListClicked(View v, int position);

    void recyclerViewListLongClicked(View v, int id, int pos);
}
