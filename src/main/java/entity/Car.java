package entity;

public class Car {
    private String name;
    private String color;
    private String numberPlate;

    public Car(String name, String color, String numberPlate) {
        this.name = name;
        this.color = color;
        this.numberPlate = numberPlate;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getNumberPlate() {
        return numberPlate;
    }
}
