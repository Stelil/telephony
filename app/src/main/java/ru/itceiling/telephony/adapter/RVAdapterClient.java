package ru.itceiling.telephony.adapter;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ru.itceiling.telephony.Person;
import ru.itceiling.telephony.R;

public class RVAdapterClient extends RecyclerView.Adapter<RVAdapterClient.PersonViewHolder>{

    public static List<Person> persons;
    private static RecyclerViewClickListener itemListener;

    public static class PersonViewHolder extends RecyclerView.ViewHolder{
        CardView cv;
        TextView personName;
        TextView personPhone;
        TextView personLabel;
        TextView personStatus;
        TextView personManager;

        PersonViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.cv);
            personName = (TextView)itemView.findViewById(R.id.person_name);
            personPhone = (TextView)itemView.findViewById(R.id.person_phone);
            personLabel = (TextView)itemView.findViewById(R.id.person_label);
            personStatus = (TextView)itemView.findViewById(R.id.person_status);
            personManager = (TextView)itemView.findViewById(R.id.person_manager);

            itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if(pos != RecyclerView.NO_POSITION){
                        itemListener.recyclerViewListClicked(v, pos);
                    }
                }
            });
        }
    }

    public RVAdapterClient(List<Person> persons, RecyclerViewClickListener itemListener){
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
        personViewHolder.personName.setText(persons.get(i).name);
        personViewHolder.personPhone.setText(persons.get(i).phone);
        personViewHolder.personLabel.setText(persons.get(i).label);
        personViewHolder.personStatus.setText(persons.get(i).status);
        personViewHolder.personManager.setText(persons.get(i).manager);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}