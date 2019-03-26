package ru.itceiling.telephony;

import android.widget.ImageView;

public class HistoryClient {
    String date_time;
    String text;
    int type;

    public HistoryClient(String date_time, String text, int type) {
        this.date_time = date_time;
        this.text = text;
        this.type = type;
    }

    public String getDate_time() {
        return date_time;
    }

    public void setDate_time(String date_time) {
        this.date_time = date_time;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getTypeMessage() {
        return type;
    }

    public void setTypeMessage(int type) {
        this.type = type;
    }
}
