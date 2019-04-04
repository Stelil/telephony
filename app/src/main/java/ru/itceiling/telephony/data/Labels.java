package ru.itceiling.telephony.data;

public class Labels {

    public int id;
    public String title;
    public Integer colorCode;

    public Labels(int id, String title, Integer colorCode){
        this.id = id;
        this.title = title;
        this.colorCode = colorCode;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getColorCode() {
        return colorCode;
    }

    public void setColorCode(Integer colorCode) {
        this.colorCode = colorCode;
    }
}
