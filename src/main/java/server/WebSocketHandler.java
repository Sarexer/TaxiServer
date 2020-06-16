package server;

import db.DbController;
import entity.*;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebSocket
public class WebSocketHandler {
    DbController dbController = DbController.getInstance();
    UdpClient udpClient = new UdpClient();
    OkHttpClient httpClient = new OkHttpClient();

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        System.out.println("user connected");
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
        //SparkServer.removeUser(user);
        for (Passenger passenger : SparkServer.passengers.values()) {
            if (passenger.session().equals(user)) {
                SparkServer.passengers.remove(passenger.getId());
                System.out.println("passenger disconnect");
                return;
            }
        }

        for (Driver driver : SparkServer.drivers.values()) {
            if (driver.session().equals(user)) {
                //SparkServer.drivers.remove(driver.getId());
                System.out.println("driver disconnect");
                return;
            }
        }

    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        //SparkServer.broadcastMessage(user, message);
        JSONObject jsonObject = new JSONObject(message);

        switch (jsonObject.getString("command")) {
            case "auth":
                auth(user, jsonObject);
                break;

            case "reconnect":
                reconnect(user, jsonObject);
                break;

            case "newOrder":
                createOrder(user, jsonObject);
                break;
            case "passingOrder":
                passingOrder(user,jsonObject);
                break;
            case "passingOrderResp":
                passingOrderResp(jsonObject);
                break;
            case "editOrder":
                editOrder(jsonObject);
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

            case "driverArrived":
                driverArrived(jsonObject);
                break;
            case "startTrip":
                startTrip(jsonObject);
                break;
            case "continueTrip":
                continueTrip(jsonObject);
                break;
            case "endTrip":
                cancelTrip(jsonObject);
                break;
            case "cancelTrip":
                cancelTrip(jsonObject);
                break;
            case "ordersHistory":
                ordersHistory(jsonObject);
                break;

            case "driverRating":
                driverRating(jsonObject);
                break;

            case "changeBusy":
                changeBusy(jsonObject);
                break;

        }
    }

    private void changeBusy(JSONObject jsonObject) {
        int driverId = jsonObject.getInt("driverId");
        boolean onLine = jsonObject.getBoolean("onLine");

        Driver driver = SparkServer.drivers.get(driverId);
        driver.setBusy(!onLine);
    }

    private void passingOrderResp(JSONObject jsonObject) {
        String orderId = jsonObject.getString("orderId");
        String passingOrderId = jsonObject.getString("passingOrderId");
        boolean result = jsonObject.getBoolean("result");

        Order order = SparkServer.orders.get(orderId);
        Order passingOrder = SparkServer.orders.get(passingOrderId);

        if(result){
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("command", "passingOrder");
            jsonObject1.put("order", order.toJson());
            String json = jsonObject1.toString();

            sendMessage(passingOrder.getDriver().session(), json);
            sendInfoAboutDriverToPassenger(order);

            order.startTimer();
            //sendMessage(passingOrder.getPassenger().session(), json);
        }else{
            sendFaileMessageToPassenger(order.getPassenger().session());
        }

    }

    private void ordersHistory(JSONObject jsonObject) {
        int userId = jsonObject.getInt("userId");

        ArrayList<Order> history = dbController.getOrdersHistory(userId);

        jsonObject = new JSONObject();
        jsonObject.put("command" , "ordersHistory");
        jsonObject.put("history", history);
    }

    private void editOrder(JSONObject jsonObject) {
        JSONArray params = jsonObject.getJSONArray("params");
        JSONObject jsonOrder = params.getJSONObject(0);
        String orderId = jsonOrder.getString("id");
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
        order.setId(orderId);
        order.setAmountOfPassengers(amountOfPassengers);
        order.setCargo(cargo);
        order.setComment(comment);
        order.setWaitingListInMinutes((List<Integer>) (Object) waitingList);

        Order oldOrder = SparkServer.orders.get(orderId);
        order.setDriver(oldOrder.getDriver());
        order.setPassenger(oldOrder.getPassenger());

        SparkServer.orders.replace(orderId, order);

        sendEditedOrderToClients(order);
    }

    private void sendEditedOrderToClients(Order order) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "editOrder");
        jsonObject.put("order", order.toJson());

        sendMessage(order.getDriver().session(), jsonObject.toString());
        //sendMessage(order.getPassenger().session(), jsonObject.toString());
    }

    private void cancelTrip(JSONObject jsonObject) {
        System.out.println("cancel");
        String orderId = jsonObject.getString("orderId");
        Order order = SparkServer.orders.get(orderId);

        jsonObject = new JSONObject();
        jsonObject.put("command", "cancelTrip");

        try {
            sendMessage(order.getPassenger().session(), jsonObject.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            sendMessage(order.getDriver().session(), jsonObject.toString());
        }catch (Exception e){
            e.printStackTrace();
        }



        SparkServer.drivers.get(order.getDriver().getId()).setBusy(false);
        order.stopTimer();

        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("result", "complete");
        jsonObject1.put("driverId", order.getDriver().getId());
        jsonObject1.put("order", order.toJson());

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://178.46.154.134/driver_ans")
                .post(RequestBody.create(jsonObject1.toString(), MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            System.out.println(response.message());
        } catch (IOException e) {
            e.printStackTrace();
        }

        SparkServer.orders.remove(orderId);
    }

    private void sendDriverLocationToPassenger(JSONObject jsonObject) {
        int driverId = jsonObject.getInt("driverId");
        String orderId = jsonObject.getString("orderId");
        Order order = SparkServer.orders.get(orderId);
        LatLng driverLocation = SparkServer.drivers.get(driverId).getCurrentLocation();

        jsonObject = new JSONObject();
        jsonObject.put("command", "driverLocation");
        jsonObject.put("latitude", driverLocation.getLatitude());
        jsonObject.put("longitude", driverLocation.getLongitude());

        sendMessage(order.getPassenger().session(), jsonObject.toString());
    }

    private void sendMessage(Session user, String message) {
        try {
            user.getRemote().sendString(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void auth(Session session, JSONObject jsonObject) {
        String login = jsonObject.getString("login");
        String password = jsonObject.getString("password");

        Object user = dbController.authenticate(login, password);


        jsonObject = new JSONObject();

        if (user != null) {
            if (user instanceof Passenger) {
                Passenger passenger = (Passenger) user;
                passenger.setSession(session);
                SparkServer.passengers.put(passenger.getId(), passenger);

                jsonObject.put("command", "auth_response");
                jsonObject.put("result", "true");
                jsonObject.put("user", new JSONObject(passenger));
            } else {
                Driver driver = (Driver) user;
                Car car = dbController.routeList.check(driver.getId());
                driver.setCar(car);

                if(car == null){
                    driver.setBusy(true);
                }

                driver.setSession(session);
                SparkServer.drivers.put(driver.getId(), driver);

                jsonObject.put("command", "auth_response");
                jsonObject.put("result", "true");
                jsonObject.put("user", new JSONObject(driver));
            }
        } else {
            jsonObject.put("command", "auth_response");
            jsonObject.put("result", "false");
        }

        sendMessage(session, jsonObject.toString());
    }

    private void reconnect(Session user, JSONObject jsonObject) {
        String role = jsonObject.getString("role");
        int userId = jsonObject.getInt("userId");

        if (role.equals("passenger")) {
            if (SparkServer.passengers.containsKey(userId)) {
                Passenger passenger = SparkServer.passengers.get(userId);
                //passenger.session().close();
                passenger.setSession(user);
            } else {
                auth(user, jsonObject);
            }
        } else {
            if (SparkServer.drivers.containsKey(userId)) {
                Driver driver = SparkServer.drivers.get(userId);
                driver.session().close();
                driver.setSession(user);
            } else {
                auth(user, jsonObject);
            }
        }
    }

    private void createOrder(Session session, JSONObject jsonObject) {
        int passengerId = jsonObject.getInt("passengerId");
        JSONArray params = jsonObject.getJSONArray("params");
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
            sendFaileMessageToPassenger(session);
            return;
        }

        order.setDriver(driver);
        order.setPassenger(SparkServer.passengers.get(passengerId));

        SparkServer.addOrder(order);

        sendOrderToDriver(order);
    }

    private void passingOrder(Session session, JSONObject jsonObject) {
        int passengerId = jsonObject.getInt("passengerId");
        JSONArray params = jsonObject.getJSONArray("params");
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
        Order passingOrder = SparkServer.findPassingOrder(order);

        if (passingOrder == null) {
            sendFaileMessageToPassenger(session);
            return;
        }

        order.setDriver(passingOrder.getDriver());
        order.setPassenger(SparkServer.passengers.get(passengerId));

        SparkServer.addOrder(order);
        sendPassingOrderToAdmin(order, passingOrder.getId());
        sendPassingOrderToPassenger(passingOrder.getPassenger().session(), order);
    }

    private void sendPassingOrderToAdmin(Order order, String passingOrderId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("main_order", passingOrderId);
        jsonObject.put("order", order.toJson());

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://178.46.154.134/associated_order")
                .post(RequestBody.create(jsonObject.toString(), MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            System.out.println(response.message());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendInfoAboutDriverToPassenger(Order order) {
        if(order.getPassenger() == null){
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "carFinded");
        jsonObject.put("result", true);
        jsonObject.put("driver", new JSONObject(order.getDriver()));
        jsonObject.put("orderId", order.getId());

        String json = jsonObject.toString();

        sendMessage(order.getPassenger().session(), json);
    }

    private void sendFaileMessageToPassenger(Session session) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "carFinded");
        jsonObject.put("result", false);

        String json = jsonObject.toString();

        sendMessage(session, json);
    }

    private void sendOrderToDriver(Order order) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "newOrder");
        jsonObject.put("order", order.toJson());

        sendMessage(order.getDriver().session(), jsonObject.toString());
    }

    private void sendPassingOrderToPassenger(Session session, Order order) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "passingOrderReq");
        jsonObject.put("order", order.toJson());

        sendMessage(session, jsonObject.toString());
    }

    private void orderResponse(JSONObject jsonObject) {
        String orderId = jsonObject.getString("orderId");
        boolean result = jsonObject.getBoolean("result");


        Order order = SparkServer.orders.get(orderId);

        if (result) {
            SparkServer.drivers.get(order.getDriver().getId()).setBusy(true);
            sendInfoAboutDriverToPassenger(order);
            sendInfoAboutDriverToAdmin(order);

            order.startTimer();
        } else {
            String declineReason = jsonObject.getString("reason");
            order.getRefusedDrivers().add(order.getDriver().getId());
            if(!order.isPassengerMakeOrder()){
                sendFailMessageToAdmin(order);
            }
            Driver driver = SparkServer.findDriver(order);

            if (driver == null) {


                if(order.getPassenger() != null){
                    sendFaileMessageToPassenger(order.getPassenger().session());
                }

                SparkServer.orders.remove(order.getId());
                return;
            }

            order.setDriver(driver);

            sendOrderToDriver(order);
        }
    }

    private void sendInfoAboutDriverToAdmin(Order order) {
        System.out.println("send to admin");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", true);
        jsonObject.put("driverId", order.getDriver().getId());
        jsonObject.put("orderId", order.getId());


        String json = jsonObject.toString();
        System.out.println(json);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://178.46.154.134/driver_ans")
                .post(RequestBody.create(json, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            System.out.println(response.message());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendFailMessageToAdmin(Order order) {
        OkHttpClient httpClient = new OkHttpClient();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", false);
        jsonObject.put("orderId", order.getId());

        String json = jsonObject.toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://178.46.154.134/driver_ans")
                .put(RequestBody.create(json, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    private void updateDriverLocation(JSONObject jsonObject) {
        int driverId = jsonObject.getInt("driverId");
        double latitude = jsonObject.getDouble("latitude");
        double longitude = jsonObject.getDouble("longitude");

        LatLng driverLocation = new LatLng(latitude, longitude);

        //System.out.println(latitude + ", " + longitude);

        Driver driver = SparkServer.drivers.get(driverId);
        driver.setCurrentLocation(driverLocation);

        udpClient.sendMessage(jsonObject.toString());
    }

    private void driverArrived(JSONObject jsonObject) {
        String orderId = jsonObject.getString("orderId");
        Order order = SparkServer.orders.get(orderId);

        jsonObject = new JSONObject();
        jsonObject.put("command", "driverArrived");

        sendMessage(order.getPassenger().session(), jsonObject.toString());
    }

    private void startTrip(JSONObject jsonObject) {
        String orderId = jsonObject.getString("orderId");
        Order order = SparkServer.orders.get(orderId);

        jsonObject = new JSONObject();
        jsonObject.put("command", "startTrip");

        try{
            sendMessage(order.getPassenger().session(), jsonObject.toString());
        }catch (Exception e){

        }
    }

    private void continueTrip(JSONObject jsonObject) {
        String orderId = jsonObject.getString("orderId");
        Order order = SparkServer.orders.get(orderId);

        jsonObject = new JSONObject();
        jsonObject.put("command", "continueTrip");

        try{
            sendMessage(order.getPassenger().session(), jsonObject.toString());
        }catch (Exception e){

        }

    }

    private void endTrip(JSONObject jsonObject) {
        String orderId = jsonObject.getString("orderId");
        Order order = SparkServer.orders.get(orderId);

        jsonObject = new JSONObject();
        jsonObject.put("command", "endTrip");

        sendMessage(order.getPassenger().session(), jsonObject.toString());

        order.getDriver().setBusy(false);
        order.stopTimer();
        SparkServer.orders.remove(orderId);
    }

    private void driverRating(JSONObject jsonObject){
        /*String orderId = jsonObject.getString("orderId");
        int rating = jsonObject.getInt("rating");
        int driverId = jsonObject.getInt("driverId");*/

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://178.46.154.134/rate")
                .post(RequestBody.create(jsonObject.toString(), MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            System.out.println(response.message());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
