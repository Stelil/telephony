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
}