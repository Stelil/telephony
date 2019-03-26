package ru.itceiling.telephony.comparators;

import java.util.Comparator;

import ru.itceiling.telephony.AdapterList;

public class ComparatorComment implements Comparator<AdapterList> {
    @Override
    public int compare(AdapterList adapterList, AdapterList t1) {
        return adapterList.getThree().toLowerCase().compareTo(t1.getThree());
    }
}
