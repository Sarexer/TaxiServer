package entity;

import org.eclipse.jetty.websocket.api.Session;

public class Driver implements Cloneable{

    private Session session;

    private int id;
    private String firstName;
    private String lastName;
    private String phone;
    private double rating;

    private Car car;
    private LatLng currentLocation;

    private boolean isBusy = false;

    public Driver(int id, String firstName, String lastName, String phone, double rating) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.rating = rating;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
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

    public double getRating() {
        return rating;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public void setBusy(boolean busy) {
        isBusy = busy;
    }

    public LatLng getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(LatLng currentLocation) {
        this.currentLocation = currentLocation;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Session session() {
        return session;
    }

    @Override
    public Object clone()  {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return null;
    }
}
