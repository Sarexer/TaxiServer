package server;


import db.DbController;
import entity.Driver;
import entity.Order;
import entity.Passenger;
import enums.Role;
import javafx.util.Pair;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;
import spark.QueryParamsMap;
import spark.Request;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static spark.Spark.*;

public class SparkServer {
    DbController dbController = DbController.getInstance();
    static Map<Integer, Driver> drivers = new ConcurrentHashMap<>();
    static Map<Integer, Passenger> passengers = new ConcurrentHashMap<>();
    static Map<String, Order> orders = new ConcurrentHashMap<>();

    public SparkServer() {
        staticFileLocation("/public"); //index.html is served at localhost:4567 (default port)
        webSocket("/", WebSocketHandler.class);
        port(1515);

        get("/auth", (req, res) -> {
            return auth(req);
        });

        init();
    }

   /*public static void broadcastMessage(Session session, String message) {
       bots.keySet().stream(

       ).filter(Session::isOpen).forEach(session -> {
           try {
               if(!session.equals(user)){
                   session.getRemote().sendString(message);
               }
           } catch (Exception e) {
               e.printStackTrace();
           }
       });
   }*/

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
        Driver driver = null;

        for (Driver value : drivers.values()) {
            if(order.getRefusedDrivers().contains(value.getId()) || value.isBusy()){
                continue;
            }else{
                driver = value;
                break;
            }
        }

return driver;
        /*Pair<Session,User> p = null;
        for (Session session : drivers.keySet()) {
            p = new Pair<>(session, drivers.get(session));
        }
        return p;*/
    }


}
