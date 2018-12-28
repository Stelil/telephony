package ru.itceiling.telephony.Comparators;

import java.util.Comparator;

import ru.itceiling.telephony.AdapterList;

public class ComparatorStatus implements Comparator<AdapterList> {
    @Override
    public int compare(AdapterList adapterList, AdapterList t1) {
        return adapterList.getTwo().toLowerCase().compareTo(t1.getTwo().toLowerCase());
    }
}
