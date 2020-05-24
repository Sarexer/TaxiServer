package db;

import entity.Car;
import entity.Driver;
import entity.Order;
import entity.Passenger;

import java.util.ArrayList;

public class DbController {
    private static DbController instance;

    private DbController(){

    }

    public Object authenticate(String login, String password){

        if(login.equals("p1")){
            return new Passenger(1, "Светлана", "Шмакова", "+79088743746");
        }
        if(login.equals("p2")){
            return new Passenger(1, "Илья", "Шмаков", "+79088743746");
        }
        if(login.equals("sed")){
            Driver driver = new Driver(2, "Валера", "Рыба", "+79097387721", 1);
            Car car = new Car("Huyndai Solaris", "orange", "T000T00", 4, false);
            driver.setCar(car);

            return driver;
        }
        if(login.equals("pick")){
            Driver driver = new Driver(3, "Андрей", "Каренгин", "+7123452345", 1);
            Car car = new Car("Toyota Hilux", "silver", "T000T00", 4, true);
            driver.setCar(car);

            return driver;
        }
        if(login.equals("bus")){
            Driver driver = new Driver(4, "Илья", "Мальцев", "+709427384324", 1);
            Car car = new Car("ЛИАЗ-5256", "white", "T000T00", 44, false);
            driver.setCar(car);

            return driver;
        }
        if(login.equals("mbus")){
            Driver driver = new Driver(5, "Евгений", "Нохрин", "+7837294234", 1);
            Car car = new Car("Нyundai H-1", "silver", "T000T00", 12, false);
            driver.setCar(car);

            return driver;
        }

        return null;
    }

    public ArrayList<Order> getOrdersHistory(int userId){
        ArrayList<Order> history = new ArrayList<>();

        Order order = new Order(null, null);
        order.setDriver((Driver)authenticate("d1", ""));
        order.setPassenger((Passenger) authenticate("p", ""));

        history.add(order);
        history.add(order);
        history.add(order);
        history.add(order);

        order = new Order(null, null);
        order.setPassenger((Passenger) authenticate("p", ""));
        order.setDriver((Driver) authenticate("d2", ""));

        history.add(order);
        history.add(order);
        history.add(order);
        history.add(order);
        history.add(order);
        history.add(order);
        history.add(order);
        history.add(order);
        history.add(order);
        history.add(order);
        history.add(order);
        history.add(order);
        history.add(order);

        return history;
    }

    public static DbController getInstance() {
        if(instance == null){
            instance = new DbController();
        }

        return instance;
    }
}
