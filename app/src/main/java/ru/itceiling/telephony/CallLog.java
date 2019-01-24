package ru.itceiling.telephony;

public class CallLog {
    public Integer id;
    public String name;
    public String phone;
    public String date_time;
    public String type;

    public CallLog(Integer id, String name, String phone, String date_time, String type) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.date_time = date_time;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getDate_time() {
        return date_time;
    }

    public void setDate_time(String date_time) {
        this.date_time = date_time;
    }

    public String getStatus() {
        return type;
    }

    public void setStatus(String type) {
        this.type = type;
    }
}
