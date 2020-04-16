package server;
import db.DbController;
import entity.Driver;
import entity.LatLng;
import entity.Order;
import entity.Passenger;
import javafx.util.Pair;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
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
        SparkServer.removeUser(user);
        System.out.println("user disconnect");
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        //SparkServer.broadcastMessage(user, message);
        JSONObject jsonObject = new JSONObject(message);

        switch (jsonObject.getString("command")){
            case "auth":
                auth(user, jsonObject);
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
                updateDriverLocation(jsonObject, user);
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

        sendMessage(order.getPassengerSession(), jsonObject.toString());
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
                SparkServer.passengers.put(session, passenger);

                jsonObject.put("command", "auth_response");
                jsonObject.put("result", "true");
                jsonObject.put("user", new JSONObject(passenger));
            }else{
                Driver driver = (Driver) user;
                SparkServer.drivers.put(session, driver);

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

    private void createOrder(Session session, JSONObject jsonObject){
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
        Pair<Session, Driver> pair = SparkServer.findDriver(order);

        if(pair == null){
            sendFaileMessageToPassenger(session);
            return;
        }

        order.setDriverSession(pair.getKey());
        order.setDriver(pair.getValue());

        order.setPassengerSession(session);
        order.setPassenger(SparkServer.passengers.get(session));
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

        sendMessage(order.getPassengerSession(), json);
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

        sendMessage(order.getDriverSession(), jsonObject.toString());
    }

    private void orderResponse(JSONObject jsonObject){
        String orderId = jsonObject.getString("orderId");
        boolean result = jsonObject.getBoolean("result");

        Order order = SparkServer.orders.get(orderId);

        if(result){
            sendInfoAboutDriverToPassenger(order);
        }else{
            order.getRefusedDrivers().add(order.getDriver().getId());
            Pair<Session, Driver> driverSession = SparkServer.findDriver(order);

            if(driverSession == null){
                sendFaileMessageToPassenger(order.getPassengerSession());
                SparkServer.orders.remove(order.getId());
                return;
            }

            order.setDriverSession(driverSession.getKey());
            order.setDriver(driverSession.getValue());

            sendOrderToDriver(order);
        }
    }

    private void updateDriverLocation(JSONObject jsonObject, Session user){
        double latitude = jsonObject.getDouble("latitude");
        double longitude = jsonObject.getDouble("longitude");

        LatLng driverLocation = new LatLng(latitude, longitude);

        System.out.println(latitude + ", " + longitude);

        Driver driver = SparkServer.drivers.get(user);
        driver.setCurrentLocation(driverLocation);
    }

}
