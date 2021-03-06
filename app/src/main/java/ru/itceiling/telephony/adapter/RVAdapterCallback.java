package ru.itceiling.telephony.adapter;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ru.itceiling.telephony.data.Callback;
import ru.itceiling.telephony.R;

public class RVAdapterCallback extends RecyclerView.Adapter<RVAdapterCallback.CallbackViewHolder> {

    public static List<Callback> callbacks;
    private static RecyclerViewClickListener itemListener;

    public static class CallbackViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView callbackName;
        TextView callbackPhone;
        TextView callbackDate;
        TextView callbackComment;
        TextView callbackManager;
        TextView nameDay;

        CallbackViewHolder(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.cv);
            callbackName = itemView.findViewById(R.id.callback_name);
            callbackPhone = itemView.findViewById(R.id.callback_phone);
            callbackDate = itemView.findViewById(R.id.callback_time);
            callbackComment = itemView.findViewById(R.id.callback_comment);
            callbackManager = itemView.findViewById(R.id.callback_manager);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        itemListener.recyclerViewListClicked(v, pos);
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        int clickedDataItem = callbacks.get(pos).getIdCallback();
                        itemListener.recyclerViewListLongClicked(v, clickedDataItem, pos);
                    }
                    return false;
                }
            });
        }
    }

    public RVAdapterCallback(List<Callback> callbacks, RecyclerViewClickListener itemListener) {
        Log.d("logd", "RVAdapterCallback: " + callbacks.size());
        this.callbacks = callbacks;
        this.itemListener = itemListener;
    }

    @Override
    public int getItemCount() {
        return callbacks.size();
    }

    @Override
    public CallbackViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.callback_card, viewGroup, false);
        CallbackViewHolder pvh = new CallbackViewHolder(v);
        return pvh;
    }

    @Override
    public void onBindViewHolder(CallbackViewHolder callbackViewHolder, int i) {
        callbackViewHolder.callbackName.setText(callbacks.get(i).getName());
        callbackViewHolder.callbackPhone.setText(callbacks.get(i).getPhone());
        callbackViewHolder.callbackDate.setText(callbacks.get(i).getDate());
        callbackViewHolder.callbackComment.setText(callbacks.get(i).getComment());
        callbackViewHolder.callbackManager.setText(callbacks.get(i).getManager());
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }


}