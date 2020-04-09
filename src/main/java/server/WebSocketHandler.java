package server;
import com.google.gson.Gson;
import db.DbController;
import entity.LatLng;
import entity.Order;
import entity.User;
import javafx.util.Pair;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

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
                Order order = createOrder(user, jsonObject);
                sendInfoAboutDriverToPassenger(order);
                //sendOrderToDriver(order);
                break;

        }
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

        User user = dbController.authenticate(login,password);

        jsonObject = new JSONObject();
        if(user != null){
            SparkServer.addUser(session, user);
            jsonObject.put("command", "auth_response");
            jsonObject.put("result", "true");
            jsonObject.put("user", new JSONObject(user));
        }else{
            jsonObject.put("command", "auth_response");
            jsonObject.put("result", "false");
        }

        sendMessage(session, jsonObject.toString());
    }

    private Order createOrder(Session session, JSONObject jsonObject){
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
        Pair<Session, User> pair = SparkServer.findDriver(order);
        order.setDriverSession(pair.getKey());
        order.setDriver(pair.getValue());

        order.setPassengerSession(session);
        order.setPassenger(SparkServer.passengers.get(session));
        SparkServer.addOrder(order);

        return order;
    }

    private void sendInfoAboutDriverToPassenger(Order order){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "carFinded");
        ArrayList<Object> params = new ArrayList<>();
        params.add(order.toJson());

        jsonObject.put("params", params);

        String json = jsonObject.toString();

        sendMessage(order.getPassengerSession(), json);
    }

    private void sendOrderToDriver(Order order){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "newOrder");
        ArrayList<Object> params = new ArrayList<>();
        params.add(order);

        jsonObject.put("params", params);

        sendMessage(order.getDriverSession(), jsonObject.toString());
    }

}
