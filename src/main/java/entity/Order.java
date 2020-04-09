package entity;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Order {
    String id;
    Session driverSession;
    Session passengerSession;
    User driver;
    User passenger;

    LatLng departure;
    ArrayList<LatLng> destinations;

    public Order(LatLng departure, ArrayList<LatLng> destinations) {
        this.departure = departure;
        this.destinations = destinations;

        id = UUID.randomUUID().toString();
    }

    public String toJson() {
        Gson gson = new Gson();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("driver", gson.toJson(driver));
        jsonObject.put("passenger", gson.toJson(passenger));
        jsonObject.put("departure", gson.toJson(departure));
        jsonObject.put("destinations", gson.toJson(destinations));

        return jsonObject.toString();
    }

    public Session getDriverSession() {
        return driverSession;
    }

    public void setDriverSession(Session driverSession) {
        this.driverSession = driverSession;
    }

    public Session getPassengerSession() {
        return passengerSession;
    }

    public void setPassengerSession(Session passengerSession) {
        this.passengerSession = passengerSession;
    }

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
    }

    public User getPassenger() {
        return passenger;
    }

    public void setPassenger(User passenger) {
        this.passenger = passenger;
    }

    public LatLng getDeparture() {
        return departure;
    }

    public void setDeparture(LatLng departure) {
        this.departure = departure;
    }

    public ArrayList<LatLng> getDestinations() {
        return destinations;
    }

    public void setDestinations(ArrayList<LatLng> destinations) {
        this.destinations = destinations;
    }
}
