package ru.itceiling.telephony;

public class Callback {
    public String name;
    public String phone;
    public String comment;
    public String date;
    public Integer id;
    public Integer idCallback;

    public Callback(String name, String phone, String comment, String date, Integer id, Integer idCallback) {
        this.name = name;
        this.phone = phone;
        this.comment = comment;
        this.date = date;
        this.id = id;
        this.idCallback = idCallback;
    }

    public Integer getIdCallback() {
        return idCallback;
    }

    public void setIdCallback(Integer idCallback) {
        this.idCallback = idCallback;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
