package server;
import db.DbController;
import entity.Driver;
import entity.LatLng;
import entity.Order;
import entity.Passenger;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

@WebSocket
public class WebSocketHandler {
    DbController dbController = DbController.getInstance();

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        System.out.println("user connected");
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
        //SparkServer.removeUser(user);
        for (Passenger passenger : SparkServer.passengers.values()) {
            if(passenger.session().equals(user)){
                SparkServer.passengers.remove(passenger.getId());
                System.out.println("user disconnect");
                return;
            }
        }

        for (Driver driver : SparkServer.drivers.values()) {
            if(driver.session().equals(user)){
                SparkServer.passengers.remove(driver.getId());
                System.out.println("user disconnect");
                return;
            }
        }

    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        //SparkServer.broadcastMessage(user, message);
        JSONObject jsonObject = new JSONObject(message);

        switch (jsonObject.getString("command")){
            case "auth":
                auth(user, jsonObject);
                break;

            case "reconnect":
                reconnect(user, jsonObject);
                break;

            case "newOrder":
                createOrder(user, jsonObject);
                break;
            case "whereDriver":
                sendDriverLocationToPassenger(jsonObject);
                break;
                ////////////////////////////////////////////////////////////
            case "orderResponse":
                orderResponse(jsonObject);
                break;
            case "driverLocation":
                updateDriverLocation(jsonObject);
                break;
        }
    }

    private void sendDriverLocationToPassenger(JSONObject jsonObject) {
        String orderId = jsonObject.getString("orderId");
        Order order = SparkServer.orders.get(orderId);
        LatLng driverLocation = order.getDriver().getCurrentLocation();

        jsonObject = new JSONObject();
        jsonObject.put("command", "driverLocation");
        jsonObject.put("latitude", driverLocation.getLatitude());
        jsonObject.put("longitude", driverLocation.getLongitude());

        sendMessage(order.getPassenger().session(), jsonObject.toString());
    }

    private void sendMessage(Session user, String message){
        try {
            user.getRemote().sendString(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void auth(Session session,JSONObject jsonObject){
        String login = jsonObject.getString("login");
        String password = jsonObject.getString("password");

        Object user = dbController.authenticate(login,password);


        jsonObject = new JSONObject();

        if(user != null){
            if(user instanceof Passenger){
                Passenger passenger = (Passenger) user;
                passenger.setSession(session);
                SparkServer.passengers.put(passenger.getId(), passenger);

                jsonObject.put("command", "auth_response");
                jsonObject.put("result", "true");
                jsonObject.put("user", new JSONObject(passenger));
            }else{
                Driver driver = (Driver) user;
                driver.setSession(session);
                SparkServer.drivers.put(driver.getId(), driver);

                jsonObject.put("command", "auth_response");
                jsonObject.put("result", "true");
                jsonObject.put("user", new JSONObject(driver));
            }
        }else{
            jsonObject.put("command", "auth_response");
            jsonObject.put("result", "false");
        }

        sendMessage(session, jsonObject.toString());
    }

    private void reconnect(Session user, JSONObject jsonObject){
        String role = jsonObject.getString("role");
        int userId = jsonObject.getInt("userId");

        if(role.equals("passenger")){
            if(SparkServer.passengers.containsKey(userId)){
                Passenger passenger = SparkServer.passengers.get(userId);
                passenger.session().close();
                passenger.setSession(user);
            }
        }else{
            if(SparkServer.drivers.containsKey(userId)){
                Driver driver = SparkServer.drivers.get(userId);
                driver.session().close();
                driver.setSession(user);
            }
        }
    }

    private void createOrder(Session session, JSONObject jsonObject){
        int passengerId = jsonObject.getInt("passengerId");
        JSONArray params = jsonObject.getJSONArray("params");
        JSONObject jsonOrder = params.getJSONObject(0);

        JSONObject departureJson = jsonOrder.getJSONObject("departure");
        LatLng departure = new LatLng(departureJson.getDouble("latitude"), departureJson.getDouble("longitude"));

        JSONArray jsonDestinations = jsonOrder.getJSONArray("destinations");
        ArrayList<LatLng> destinations = new ArrayList<>();
        for (Object jsonDestination : jsonDestinations) {
            double lat = ((JSONObject) jsonDestination).getDouble("latitude");
            double lng = ((JSONObject) jsonDestination).getDouble("longitude");
            LatLng destination = new LatLng(lat,lng);

            destinations.add(destination);
        }

        Order order = new Order(departure, destinations);
        Driver driver = SparkServer.findDriver(order);

        if(driver == null){
            sendFaileMessageToPassenger(session);
            return;
        }

        order.setDriver(driver);
        order.setPassenger(SparkServer.passengers.get(passengerId));

        SparkServer.addOrder(order);

        sendOrderToDriver(order);
    }

    private void sendInfoAboutDriverToPassenger(Order order){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "carFinded");
        jsonObject.put("result", true);
        jsonObject.put("driver", new JSONObject(order.getDriver()));
        jsonObject.put("orderId", order.getId());

        String json = jsonObject.toString();

        sendMessage(order.getPassenger().session(), json);
    }

    private void sendFaileMessageToPassenger(Session session)
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "carFinded");
        jsonObject.put("result", false);

        String json = jsonObject.toString();

        sendMessage(session, json);
    }

    private void sendOrderToDriver(Order order){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "newOrder");
        jsonObject.put("order", order.toJson());

        sendMessage(order.getDriver().session(), jsonObject.toString());
    }

    private void orderResponse(JSONObject jsonObject){
        String orderId = jsonObject.getString("orderId");
        boolean result = jsonObject.getBoolean("result");

        Order order = SparkServer.orders.get(orderId);

        if(result){
            sendInfoAboutDriverToPassenger(order);
        }else{
            order.getRefusedDrivers().add(order.getDriver().getId());
            Driver driver = SparkServer.findDriver(order);

            if(driver == null){
                sendFaileMessageToPassenger(order.getPassenger().session());
                SparkServer.orders.remove(order.getId());
                return;
            }

            order.setDriver(driver);

            sendOrderToDriver(order);
        }
    }

    private void updateDriverLocation(JSONObject jsonObject){
        int driverId = jsonObject.getInt("driverId");
        double latitude = jsonObject.getDouble("latitude");
        double longitude = jsonObject.getDouble("longitude");

        LatLng driverLocation = new LatLng(latitude, longitude);

        System.out.println(latitude + ", " + longitude);

        Driver driver = SparkServer.drivers.get(driverId);
        driver.setCurrentLocation(driverLocation);
    }

}
