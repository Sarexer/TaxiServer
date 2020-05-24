package server;


import db.DbController;
import entity.Driver;
import entity.LatLng;
import entity.Order;
import entity.Passenger;
import geoutils.GeoUtils;
import org.json.JSONObject;
import spark.QueryParamsMap;
import spark.Request;
import timer_tasks.DriversInfoTask;

import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class SparkServer {
    DbController dbController = DbController.getInstance();
    public static Map<Integer, Driver> drivers = new ConcurrentHashMap<>();
    static Map<Integer, Passenger> passengers = new ConcurrentHashMap<>();
    static Map<String, Order> orders = new ConcurrentHashMap<>();

    public SparkServer() {
        staticFileLocation("/public"); //index.html is served at localhost:4567 (default port)
        webSocket("/", WebSocketHandler.class);
        port(1515);

        get("/auth", (req, res) -> {
            return auth(req);
        });

        get("/history", (req, res) ->{
            return ordersHistory(req);
        });

        init();

        startTimerTask();
    }

    void startTimerTask(){
        DriversInfoTask driversInfoTask = new DriversInfoTask();
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(driversInfoTask, 0, 5*1000);
    }

    private Object ordersHistory(Request req) {
        QueryParamsMap map = req.queryMap();
        int userId = Integer.parseInt(map.value("userId"));

        ArrayList<Order> history = dbController.getOrdersHistory(userId);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("history", history);

        return jsonObject.toString();
    }

    private String auth(Request req) {
        QueryParamsMap map = req.queryMap();
        String login = map.value("login");
        String password = map.value("password");

        Object user = dbController.authenticate(login, password);



        JSONObject jsonObject = new JSONObject();
        if (user != null) {
            jsonObject.put("result", "true");
            if(user instanceof Passenger){
                Passenger passenger = (Passenger) user;
                jsonObject.put("user", new JSONObject(passenger));
                jsonObject.put("role", "passenger");
            }else{
                Driver driver = (Driver) user;
                jsonObject.put("user", new JSONObject(driver));
                jsonObject.put("role", "driver");

            }
        } else {
            jsonObject.put("result", "false");
        }

        return jsonObject.toString();
    }

    public static void addOrder(Order order) {
        orders.put(order.getId(), order);
    }

    public static Driver findDriver(Order order) {
        double minDistance = Double.MAX_VALUE;
        Driver driver = null;

        Map<Integer, Driver> filteredDrivers = drivers.values().stream()
                .filter(e -> !e.isBusy())
                .filter(e -> order.getAmountOfPassengers() <= e.getCar().getAmountOfSeats())
                .collect(Collectors.toMap(Driver::getId, Function.identity()));

        for (Driver value : filteredDrivers.values()) {
            if(order.getCargo().equals("Да") && !value.getCar().isHasTrunk()){
                continue;
            }
            if (order.getRefusedDrivers().contains(value.getId())) {
                continue;
            } else {
                LatLng departure = order.getDeparture();
                LatLng driverLocation = value.getCurrentLocation();

                double distanceBetween = GeoUtils.distance(departure.getLatitude(), driverLocation.getLatitude(),
                        departure.getLongitude(), driverLocation.getLongitude(), 0, 0);

                if (distanceBetween < minDistance) {
                    minDistance = distanceBetween;
                    driver = value;
                }
            }
        }

        return driver;
    }


}
