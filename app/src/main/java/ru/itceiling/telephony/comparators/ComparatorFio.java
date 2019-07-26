package ru.itceiling.telephony.comparators;

import java.util.Comparator;

import ru.itceiling.telephony.data.AdapterList;

public class ComparatorFio implements Comparator<AdapterList> {
    @Override
    public int compare(AdapterList adapterList, AdapterList t1) {
        return adapterList.getOne().toLowerCase().compareTo(t1.getOne());
    }
}
