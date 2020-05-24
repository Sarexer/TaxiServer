package entity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Order {
    String id;
    Driver driver;
    Passenger passenger;

    LatLng departure;
    ArrayList<LatLng> destinations;

    List<Integer> waitingListInMinutes = new ArrayList<>();
    int amountOfPassengers;
    String cargo = "";
    String comment = "";

    HashSet<Integer> refusedDrivers = new HashSet<>();

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Order(LatLng departure, ArrayList<LatLng> destinations) {
        this.departure = departure;
        this.destinations = destinations;

        id = UUID.randomUUID().toString();
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("driver", new JSONObject(driver));
        jsonObject.put("passenger", new JSONObject(passenger));
        jsonObject.put("departure", new JSONObject(departure));
        jsonObject.put("destinations", destinations);
        jsonObject.put("amountPassengers", amountOfPassengers);
        jsonObject.put("cargo", cargo);
        jsonObject.put("comment", comment);
        jsonObject.put("waitingList", waitingListInMinutes);

        return jsonObject;
    }

    public void startTimer(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("command", "driverLocation");
                jsonObject.put("lat", driver.getCurrentLocation().getLatitude());
                jsonObject.put("lng", driver.getCurrentLocation().getLongitude());

                String json = jsonObject.toString();

                try {
                    passenger.session.getRemote().sendString(json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        scheduler.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.SECONDS);
    }

    public void stopTimer(){
        scheduler.shutdown();
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

    public List<Integer> getWaitingListInMinutes() {
        return waitingListInMinutes;
    }

    public void setWaitingListInMinutes(List<Integer> waitingListInMinutes) {
        this.waitingListInMinutes = waitingListInMinutes;
    }

    public int getAmountOfPassengers() {
        return amountOfPassengers;
    }

    public void setAmountOfPassengers(int amountOfPassengers) {
        this.amountOfPassengers = amountOfPassengers;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
