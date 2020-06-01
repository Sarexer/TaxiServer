package server;


import db.DbController;
import entity.Driver;
import entity.LatLng;
import entity.Order;
import entity.Passenger;
import geoutils.GeoUtils;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import routes.RouteBuilder;
import spark.QueryParamsMap;
import spark.Request;
import timer_tasks.DriversInfoTask;

import java.io.IOException;
import java.util.*;
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

        get("/driverHistory", (req, res) ->{
            return driverHistory(req);
        });

        get("/newOrder", ((request, response) -> {
            return newOrder(request);
        }));

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

    private Object driverHistory(Request req) {
        QueryParamsMap map = req.queryMap();
        int userId = Integer.parseInt(map.value("userId"));

        ArrayList<Order> history = dbController.getDriverHistory(userId);

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

    public static Order findPassingOrder(Order order){
        RouteBuilder routeBuilder = new RouteBuilder();
        /*LatLng departure = new LatLng(57.116218108580135,
                65.57478010654451);
        List<LatLng> destinations = new ArrayList<>();
        destinations.add(new LatLng(57.14528549409122,
                65.58050394058229));*/
        List<LatLng> orderRoute = routeBuilder.getRoute(order.getDeparture(),order.getDestinations());

        Order bestPassingOrder = null;
        double maxMatch = -1;

        for (Order value : orders.values()) {
            List<LatLng> passingRoute = routeBuilder.getRoute(value.getDeparture(), value.getDestinations());
            List<LatLng> subRoute = new ArrayList<>();
            List<LatLng> maxSubRoute = new ArrayList<>();
            int lastFindedIndex = -1;
            for(int i =0;i<orderRoute.size();i++){
                LatLng orderRoutePoint = orderRoute.get(i);

                if(lastFindedIndex != -1){
                    if(lastFindedIndex+1 < passingRoute.size()){
                        if(passingRoute.get(lastFindedIndex+1).equals(orderRoutePoint)){
                            subRoute.add(orderRoutePoint);
                            lastFindedIndex++;
                        }else{
                            if(subRoute.size() > maxSubRoute.size()){
                                maxSubRoute = subRoute;
                            }
                            subRoute = new ArrayList<>();
                            lastFindedIndex = -1;
                            i--;
                        }
                    }
                }else {
                    int index = -1;
                    for (int j =0;j<passingRoute.size();j++) {
                        LatLng latLng = passingRoute.get(j);
                        if(latLng.equals(orderRoutePoint)){
                            index = j;
                            break;
                        }
                    }
                    if(index == -1){
                        continue;
                    }else{
                        subRoute.add(orderRoutePoint);
                        lastFindedIndex = index;
                    }
                }
                if(i == orderRoute.size()-1){
                    if(!subRoute.isEmpty()){
                        if(subRoute.size() > maxSubRoute.size()){
                            maxSubRoute = subRoute;
                        }
                    }
                }
            }

            double match = maxSubRoute.size()/orderRoute.size();
            if(match > maxMatch){
                maxMatch = match;
                bestPassingOrder = value;
            }
        }

        return  bestPassingOrder;
    }

    public static Driver findPassingDriverTest(Order order){
        RouteBuilder routeBuilder = new RouteBuilder();
        /*LatLng departure = new LatLng(57.116218108580135,
                65.57478010654451);
        List<LatLng> destinations = new ArrayList<>();
        destinations.add(new LatLng(57.14528549409122,
                65.58050394058229));*/
        List<Integer> orderRoute = Arrays.asList(1,2,3,4,5,6,7,8,9,10);

            List<Integer> passingRoute = Arrays.asList(11,12,13,1,2,8,9,10);
            List<Integer> subRoute = new ArrayList<>();
            List<Integer> maxSubRoute = new ArrayList<>();
            int lastFindedIndex = -1;
            for(int i =0;i<orderRoute.size();i++){
                Integer orderRoutePoint = orderRoute.get(i);

                if(lastFindedIndex != -1){
                    if(lastFindedIndex+1 < passingRoute.size()){
                        if(passingRoute.get(lastFindedIndex+1).equals(orderRoutePoint)){
                            subRoute.add(orderRoutePoint);
                            lastFindedIndex++;
                        }else{
                            if(subRoute.size() > maxSubRoute.size()){
                                maxSubRoute = subRoute;
                            }
                            subRoute = new ArrayList<>();
                            lastFindedIndex = -1;
                            i--;
                        }
                    }
                }else {
                    int index = passingRoute.indexOf(orderRoutePoint);
                    if(index == -1){
                        continue;
                    }else{
                        subRoute.add(orderRoutePoint);
                        lastFindedIndex = index;
                    }
                }
                if(i == orderRoute.size()-1){
                    if(!subRoute.isEmpty()){
                        if(subRoute.size() > maxSubRoute.size()){
                            maxSubRoute = subRoute;
                        }
                    }
                }
            }

        return  null;
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

    public String newOrder(Request req){
        JSONObject jsonObject = new JSONObject(req.body());

        int passengerId = jsonObject.getInt("passengerId");
        JSONArray params = jsonObject.getJSONArray("order");
        JSONObject jsonOrder = params.getJSONObject(0);
        JSONArray jsonWaitingList = jsonOrder.getJSONArray("waitingList");

        JSONObject departureJson = jsonOrder.getJSONObject("departure");
        LatLng departure = new LatLng(departureJson.getDouble("latitude"), departureJson.getDouble("longitude"));

        JSONArray jsonDestinations = jsonOrder.getJSONArray("destinations");
        ArrayList<LatLng> destinations = new ArrayList<>();
        for (Object jsonDestination : jsonDestinations) {
            double lat = ((JSONObject) jsonDestination).getDouble("latitude");
            double lng = ((JSONObject) jsonDestination).getDouble("longitude");
            LatLng destination = new LatLng(lat, lng);

            destinations.add(destination);
        }
        List<Object> waitingList = jsonWaitingList.toList();


        String cargo = jsonOrder.getString("cargo");
        String comment = jsonOrder.getString("comment");
        int amountOfPassengers = jsonOrder.getInt("passengersAmount");


        Order order = new Order(departure, destinations);
        order.setAmountOfPassengers(amountOfPassengers);
        order.setCargo(cargo);
        order.setComment(comment);
        order.setWaitingListInMinutes((List<Integer>) (Object) waitingList);

        Driver driver = SparkServer.findDriver(order);

        if (driver == null) {
            JSONObject failJson = new JSONObject();
            failJson.put("command", "carFinded");
            failJson.put("result", false);

            String json = failJson.toString();
            return json;
        }

        order.setDriver(driver);
        order.setPassenger(SparkServer.passengers.get(passengerId));

        SparkServer.addOrder(order);

        JSONObject driverJson = new JSONObject();
        driverJson.put("command", "newOrder");
        driverJson.put("order", order.toJson());

        try {
            driver.session().getRemote().sendString(driverJson.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }


        return "otvet";
    }



}
