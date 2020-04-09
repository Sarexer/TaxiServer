package server;


import db.DbController;
import entity.Order;
import entity.User;
import enums.Role;
import javafx.util.Pair;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;
import spark.QueryParamsMap;
import spark.Request;


import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static spark.Spark.*;

public class SparkServer {
    DbController dbController = DbController.getInstance();
    static Map<Session, User> drivers = new ConcurrentHashMap<>();
    static Map<Session, User> passengers = new ConcurrentHashMap<>();
    static Map<Integer, Order> orders = new ConcurrentHashMap<>();

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

        User user = dbController.authenticate(login, password);

        JSONObject jsonObject = new JSONObject();
        if (user != null) {
            jsonObject.put("result", "true");
            jsonObject.put("user", new JSONObject(user));
        } else {
            jsonObject.put("result", "false");
        }

        return jsonObject.toString();
    }

    public static void addUser(Session session, User user) {
        if (user.getRole() == Role.DRIVER) {
            drivers.put(session, user);
        } else {
            passengers.put(session, user);
        }
    }

    public static void removeUser(Session session) {
        drivers.remove(session);
        passengers.remove(session);
    }

    public static void addOrder(Order order) {
        Random random = new Random();

        orders.put(random.nextInt(), order);
    }

    public static Pair<Session, User> findDriver(Order order) {
        Pair<Session, User> p = null;
        for (Session session : passengers.keySet()) {
            p = new Pair<>(session, passengers.get(session));
            break;
        }
        return p;


        /*Pair<Session,User> p = null;
        for (Session session : drivers.keySet()) {
            p = new Pair<>(session, drivers.get(session));
        }
        return p;*/
    }


}
