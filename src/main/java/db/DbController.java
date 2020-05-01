package db;

import entity.Car;
import entity.Driver;
import entity.Passenger;
import enums.Role;

public class DbController {
    private static DbController instance;

    private DbController(){

    }

    public Object authenticate(String login, String password){

        if(login.equals("p")){
            return new Passenger(1, "Светлана", "Шмакова", "+79088743746");
        }
        if(login.equals("d")){
            Driver driver = new Driver(2, "Валера", "Рыба", "+79097387721", 1);
            Car car = new Car("Huyndai Solaris", "orange", "T000T00");
            driver.setCar(car);

            return driver;
        }

        return null;
    }

    public static DbController getInstance() {
        if(instance == null){
            instance = new DbController();
        }

        return instance;
    }
}
