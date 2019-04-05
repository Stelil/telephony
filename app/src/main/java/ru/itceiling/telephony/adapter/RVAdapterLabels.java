package ru.itceiling.telephony.adapter;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import ru.itceiling.telephony.R;
import ru.itceiling.telephony.data.Labels;

public class RVAdapterLabels extends RecyclerView.Adapter<RVAdapterLabels.LabelViewHolder> {

    public static List<Labels> labels;
    private static RecyclerViewClickListener itemListener;
    static String TAG = "logd";

    public static class LabelViewHolder extends RecyclerView.ViewHolder {
        CardView cv;

        LabelViewHolder(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.cv);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        itemListener.recyclerViewListClicked(null, pos);
                    }
                }
            });
        }
    }

    public RVAdapterLabels(List<Labels> labels, RecyclerViewClickListener itemListener) {
        this.labels = labels;
        this.itemListener = itemListener;
    }

    @Override
    public int getItemCount() {
        return labels.size();
    }

    @Override
    public LabelViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_label, viewGroup, false);
        LabelViewHolder pvh = new LabelViewHolder(v);
        return pvh;
    }

    @Override
    public void onBindViewHolder(LabelViewHolder labelViewHolder, int i) {

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xffffff, labels.get(i).colorCode});

        CardView cardView = labelViewHolder.cv;
        cardView.setBackgroundDrawable(gd);
        TextView tv = cardView.findViewById(R.id.tv);
        tv.setText(labels.get(i).title);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}