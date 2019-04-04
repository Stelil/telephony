package ru.itceiling.telephony.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import ru.itceiling.telephony.data.PhoneBook;
import ru.itceiling.telephony.R;

public class PhoneBookAdapter extends BaseAdapter {

    private List<PhoneBook> mListe;
    private LayoutInflater inflater;

    public PhoneBookAdapter(Context context, List<PhoneBook> mListe) {
        inflater = LayoutInflater.from(context);
        this.mListe = mListe;
    }

    @Override
    public int getCount() {
        return mListe.size();
    }

    @Override
    public Object getItem(int position) {
        return mListe.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private class ViewHolder {
        RelativeLayout item;
        TextView texte1;
        TextView texte2;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        convertView = inflater.inflate(R.layout.item_two_line, null);

        int myColor = Color.parseColor("#ffffff");
        RelativeLayout itemRecup = (RelativeLayout) convertView.findViewById(R.id.item);
        itemRecup.setBackgroundColor(myColor);

        TextView texte1Recup = (TextView) convertView.findViewById(R.id.firstLine);
        texte1Recup.setText(mListe.get(position).getTexte1());
        TextView texte2Recup = (TextView) convertView.findViewById(R.id.secondLine);
        texte2Recup.setText(mListe.get(position).getTexte2());

        final CheckBox cb1Recup =(CheckBox) convertView.findViewById(R.id.cb1);
        cb1Recup.setChecked(mListe.get(position).getCb1());
        cb1Recup.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {

                if(cb1Recup.isChecked()==true)
                {
                    mListe.get(position).setCb1(true);
                }
                else
                {
                    mListe.get(position).setCb1(false);
                }

            }
        });
        if (mListe.get(position).getTexte2().compareTo("") == 0) {
            texte2Recup.setHeight(0);
            texte2Recup.setWidth(0);
        }
        return convertView;

    }

}
