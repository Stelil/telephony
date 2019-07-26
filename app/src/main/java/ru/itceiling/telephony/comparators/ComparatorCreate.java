package ru.itceiling.telephony.comparators;

import java.util.Comparator;

import ru.itceiling.telephony.data.AdapterList;

public class ComparatorCreate implements Comparator<AdapterList> {

    @Override
    public int compare(AdapterList adapterList, AdapterList t1) {
        return adapterList.getThree().toLowerCase().compareTo(t1.getThree().toLowerCase());
    }
}
