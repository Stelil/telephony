package ru.itceiling.telephony;

public class AdapterList {
    String id;
    String one;
    String two;
    String three;
    String four;
    String five;

    public AdapterList(String _id, String _one, String _two, String _three, String _four, String _five){
        this.id = _id;
        this.one = _one;
        this.two = _two;
        this.three = _three;
        this.four = _four;
        this.five = _five;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOne() {
        return one;
    }

    public void setOne(String one) {
        this.one = one;
    }

    public String getTwo() {
        return two;
    }

    public void setTwo(String two) {
        this.two = two;
    }

    public String getThree() {
        return three;
    }

    public void setThree(String three) {
        this.three = three;
    }

    public String getFour() {
        return four;
    }

    public void setFour(String four) {
        this.four = four;
    }

    public String getFive() {
        return five;
    }

    public void setFive(String five) {
        this.five = five;
    }
}
