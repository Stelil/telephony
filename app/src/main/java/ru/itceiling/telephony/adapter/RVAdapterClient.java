package ru.itceiling.telephony.adapter;

import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import ru.itceiling.telephony.data.Person;
import ru.itceiling.telephony.R;

public class RVAdapterClient extends RecyclerView.Adapter<RVAdapterClient.PersonViewHolder> {

    public static List<Person> persons;
    private static RecyclerViewClickListener itemListener;
    static String TAG = "logd";

    public static class PersonViewHolder extends RecyclerView.ViewHolder {
        CardView cv;

        PersonViewHolder(View itemView) {
            super(itemView);
            cv = (CardView) itemView.findViewById(R.id.cv);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        itemListener.recyclerViewListClicked(v, pos);
                    }
                }
            });
        }
    }

    public RVAdapterClient(List<Person> persons, RecyclerViewClickListener itemListener) {
        this.persons = persons;
        this.itemListener = itemListener;
    }

    @Override
    public int getItemCount() {
        return persons.size();
    }

    @Override
    public PersonViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.client_card, viewGroup, false);
        PersonViewHolder pvh = new PersonViewHolder(v);
        return pvh;
    }

    @Override
    public void onBindViewHolder(PersonViewHolder personViewHolder, int i) {
        CardView cardView = personViewHolder.cv;
        TextView personName = (TextView) cardView.findViewById(R.id.person_name);
        TextView personPhone = (TextView) cardView.findViewById(R.id.person_phone);
        LinearLayout label = (LinearLayout) cardView.findViewById(R.id.label);
        TextView personStatus = (TextView) cardView.findViewById(R.id.person_status);
        TextView personManager = (TextView) cardView.findViewById(R.id.person_manager);
        ImageView personContact = (ImageView) cardView.findViewById(R.id.person_contact);


        personName.setText(persons.get(i).name);
        personPhone.setText(persons.get(i).phone);
        personStatus.setText(persons.get(i).status);
        personManager.setText(persons.get(i).manager);

        try {
            int parsedColor = Color.parseColor("#" + persons.get(i).label);
            label.setBackgroundColor(parsedColor);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!persons.get(i).getType_contact().equals("0")) {
            personContact.setImageResource(R.drawable.vk);
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}