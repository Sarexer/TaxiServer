package entity;

import enums.Role;
import org.eclipse.jetty.websocket.api.Session;

public class Passenger {
    Session session;

    private int id;
    private String firstName;
    private String lastName;
    private String phone;

    public Passenger(int id, String firstName, String lastName, String phone) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }

    public int getId() {
        return id;
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

    public Session session() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }
}
