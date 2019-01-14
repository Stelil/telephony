package ru.itceiling.telephony.Adapter;

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

import static com.android.volley.VolleyLog.TAG;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.PersonViewHolder>{

    List<Person> persons;

    public static class PersonViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CardView cv;
        TextView personName;
        TextView personPhone;
        TextView personLabel;
        TextView personStatus;
        TextView personManager;

        String TAG = "logd";

        PersonViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.cv);
            personName = (TextView)itemView.findViewById(R.id.person_name);
            personPhone = (TextView)itemView.findViewById(R.id.person_phone);
            personLabel = (TextView)itemView.findViewById(R.id.person_label);
            personStatus = (TextView)itemView.findViewById(R.id.person_status);
            personManager = (TextView)itemView.findViewById(R.id.person_manager);

        }

    }

    public RVAdapter(List<Person> persons){
        this.persons = persons;
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
