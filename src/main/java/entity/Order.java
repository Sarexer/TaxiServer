package entity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;

import java.util.*;

public class Order {
    String id;
    Driver driver;
    Passenger passenger;

    LatLng departure;
    ArrayList<LatLng> destinations;

    HashSet<Integer> refusedDrivers = new HashSet<>();

    public Order(LatLng departure, ArrayList<LatLng> destinations) {
        this.departure = departure;
        this.destinations = destinations;

        id = UUID.randomUUID().toString();
    }

    public JSONObject toJson() {
        Gson gson = new Gson();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("driver", new JSONObject(driver));
        jsonObject.put("passenger", new JSONObject(passenger));
        jsonObject.put("departure", new JSONObject(departure));
        jsonObject.put("destinations", destinations);

        return jsonObject;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public void setPassenger(Passenger passenger) {
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

    public String getId() {
        return id;
    }

    public HashSet<Integer> getRefusedDrivers() {
        return refusedDrivers;
    }
}
