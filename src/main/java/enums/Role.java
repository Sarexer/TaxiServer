package enums;

public enum Role {
    DRIVER("driver"),
    PASSENGER("passenger");

    private String name;

    Role(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
