package ru.itceiling.telephony.data;

public class Person {
    public String name;
    public String phone;
    public String manager;
    public String color;
    public String label;
    public String status;
    public Integer id;
    public String type_contact;

    public Person(String name, String phone, String manager, String color, String label, String status, Integer id, String type_contact) {
        this.name = name;
        this.phone = phone;
        this.manager = manager;
        this.color = color;
        this.label = label;
        this.status = status;
        this.id = id;
        this.type_contact = type_contact;
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

    public String getType_contact() {
        return type_contact;
    }

    public void setType_contact(String type_contact) {
        this.type_contact = type_contact;
    }
}