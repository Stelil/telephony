package ru.itceiling.telephony.data;

public class Callback {
    public String name;
    public String phone;
    public String comment;
    public String date;
    public Integer id;
    public Integer idCallback;
    public String manager;
    public String nameDay;

    public Callback(String name, String phone, String comment, String date, Integer id, Integer idCallback, String manager, String nameDay) {
        this.name = name;
        this.phone = phone;
        this.comment = comment;
        this.date = date;
        this.id = id;
        this.idCallback = idCallback;
        this.manager = manager;
        this.nameDay = nameDay;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
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

    public String getNameDay() {
        return nameDay;
    }

    public void setNameDay(String nameDay) {
        this.nameDay = nameDay;
    }
}
