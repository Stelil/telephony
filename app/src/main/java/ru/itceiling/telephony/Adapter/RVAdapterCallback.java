package ru.itceiling.telephony.Adapter;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import ru.itceiling.telephony.Callback;
import ru.itceiling.telephony.R;

public class RVAdapterCallback extends RecyclerView.Adapter<RVAdapterCallback.CallbackViewHolder>{

    public static List<Callback> callbacks;
    private static RecyclerViewClickListener itemListener;

    public static class CallbackViewHolder extends RecyclerView.ViewHolder{
        CardView cv;
        TextView callbackName;
        TextView callbackPhone;
        TextView callbackDate;
        TextView callbackComment;

        CallbackViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.cv);
            callbackName = (TextView)itemView.findViewById(R.id.callback_name);
            callbackPhone = (TextView)itemView.findViewById(R.id.callback_phone);
            callbackDate = (TextView)itemView.findViewById(R.id.callback_time);
            callbackComment = (TextView)itemView.findViewById(R.id.callback_comment);

            itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if(pos != RecyclerView.NO_POSITION){
                        int clickedDataItem = callbacks.get(pos).getId();
                        //Toast.makeText(v.getContext(), "You clicked " + clickedDataItem, Toast.LENGTH_SHORT).show();
                        itemListener.recyclerViewListClicked(v, clickedDataItem);
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    int pos = getAdapterPosition();
                    if(pos != RecyclerView.NO_POSITION){
                        int clickedDataItem = callbacks.get(pos).getIdCallback();
                        itemListener.recyclerViewListLongClicked(v, clickedDataItem, pos);
                    }
                    return false;
                }
            });
        }
    }

    public RVAdapterCallback(List<Callback> callbacks, RecyclerViewClickListener itemListener){
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
        callbackViewHolder.callbackName.setText(callbacks.get(i).name);
        callbackViewHolder.callbackPhone.setText(callbacks.get(i).phone);
        callbackViewHolder.callbackDate.setText(callbacks.get(i).date);
        callbackViewHolder.callbackComment.setText(callbacks.get(i).comment);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }


}
