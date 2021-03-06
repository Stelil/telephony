package ru.itceiling.telephony.adapter;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import ru.itceiling.telephony.data.CallLog;
import ru.itceiling.telephony.R;

public class RVAdapterCallLog extends RecyclerView.Adapter<RVAdapterCallLog.CallLogViewHolder> {

    public static List<CallLog> callLogList;
    private static RecyclerViewClickListener itemListener;

    static LinearLayout relCard;

    public static class CallLogViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView callbackName;
        TextView callbackPhone;
        TextView callbackDate;
        TextView callbackType;

        CallLogViewHolder(View itemView) {
            super(itemView);
            relCard = (LinearLayout) itemView.findViewById(R.id.relCard);
            cv = (CardView) itemView.findViewById(R.id.cv);
            callbackName = (TextView) itemView.findViewById(R.id.call_log_name);
            callbackPhone = (TextView) itemView.findViewById(R.id.call_log_phone);
            callbackDate = (TextView) itemView.findViewById(R.id.call_log_time);
            callbackType = (TextView) itemView.findViewById(R.id.call_log_type);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        int clickedDataItem = callLogList.get(pos).getId();
                        itemListener.recyclerViewListClicked(v, pos);
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        int clickedDataItem = callLogList.get(pos).getId();
                        itemListener.recyclerViewListLongClicked(v, clickedDataItem, pos);
                    }
                    return false;
                }
            });
        }
    }

    public RVAdapterCallLog(List<CallLog> callLogList, RecyclerViewClickListener itemListener) {
        this.callLogList = callLogList;
        this.itemListener = itemListener;
    }

    @Override
    public int getItemCount() {
        return callLogList.size();
    }

    @Override
    public CallLogViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.call_log_card, viewGroup, false);
        CallLogViewHolder pvh = new CallLogViewHolder(v);

        return pvh;
    }

    String TAG = "callLog";

    @Override
    public void onBindViewHolder(CallLogViewHolder callbackViewHolder, int i) {
        if (callLogList.get(i).getCallStatus() == 1) {
            relCard.setBackgroundResource(R.drawable.card_red_corner);
        } else {
            relCard.setBackgroundResource(R.drawable.card_green_corner);
        }
        callbackViewHolder.callbackName.setText(callLogList.get(i).getName());
        callbackViewHolder.callbackPhone.setText(callLogList.get(i).getPhone());
        callbackViewHolder.callbackDate.setText(callLogList.get(i).getDate_time());
        callbackViewHolder.callbackType.setText(callLogList.get(i).getType());
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}