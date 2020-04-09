package entity;

import enums.Role;

public class User {
    private int id;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;

    public User(int id, String firstName, String lastName, String phone, Role role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public Role getRole() {
        return role;
    }

    public int getId() {
        return id;
    }
}
