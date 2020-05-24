package entity;

public class Car {
    private String name;
    private String color;
    private String numberPlate;

    private int amountOfSeats;
    private boolean hasTrunk;

    public Car(String name, String color, String numberPlate, int amountOfSeats, boolean hasTrunk) {
        this.name = name;
        this.color = color;
        this.numberPlate = numberPlate;
        this.amountOfSeats = amountOfSeats;
        this.hasTrunk = hasTrunk;
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

    public int getAmountOfSeats() {
        return amountOfSeats;
    }

    public boolean isHasTrunk() {
        return hasTrunk;
    }
}
