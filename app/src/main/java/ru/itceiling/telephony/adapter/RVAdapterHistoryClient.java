package ru.itceiling.telephony.adapter;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.itceiling.telephony.HistoryClient;
import ru.itceiling.telephony.R;

public class RVAdapterHistoryClient extends RecyclerView.Adapter<RVAdapterHistoryClient.HistoryViewHolder> {

    public static List<HistoryClient> historyClients;

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        ImageView typeMessage;
        TextView textDateTime;
        TextView textComment;

        HistoryViewHolder(View itemView) {
            super(itemView);
            cv = (CardView) itemView.findViewById(R.id.cv);
            typeMessage = (ImageView) itemView.findViewById(R.id.typeMessage);
            textDateTime = (TextView) itemView.findViewById(R.id.textDateTime);
            textComment = (TextView) itemView.findViewById(R.id.textComment);
        }
    }

    private Activity activity;
    String TAG = "logd";

    public RVAdapterHistoryClient(List<HistoryClient> historyClients, Activity activity) {
        this.historyClients = historyClients;
        this.activity = activity;
    }

    @Override
    public int getItemCount() {
        return historyClients.size();
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.history_card, viewGroup, false);
        HistoryViewHolder pvh = new HistoryViewHolder(v);

        return pvh;
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder personViewHolder, int i) {

        Drawable bd = null;
        switch (historyClients.get(i).getTypeMessage()) {
            case 1:
                bd = activity.getResources().getDrawable(R.drawable.ic_message);
                personViewHolder.typeMessage.setImageDrawable(bd);
                break;
            default:
                personViewHolder.typeMessage.setImageDrawable(bd);
                break;
        }

        personViewHolder.textDateTime.setText(historyClients.get(i).getDate_time());
        personViewHolder.textComment.setText(historyClients.get(i).getText());
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}