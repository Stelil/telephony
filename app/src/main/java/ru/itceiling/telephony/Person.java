package ru.itceiling.telephony;

public class Person {
    public String name;
    public String phone;
    public String manager;
    public String color;
    public String label;
    public String status;
    public Integer id;

    public Person(String name, String phone, String manager, String color, String label, String status, Integer id) {
        this.name = name;
        this.phone = phone;
        this.manager = manager;
        this.color = color;
        this.label = label;
        this.status = status;
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

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}