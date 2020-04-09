package db;

import entity.User;
import enums.Role;

public class DbController {
    private static DbController instance;

    private DbController(){

    }

    public User authenticate(String login, String password){
        User user = null;
        if(login.equals("p")){
            user = new User(1, "Светлана", "Шмакова", "+79088743746", Role.PASSENGER);
        }
        if(login.equals("d")){
            user = new User(2, "Илья", "Шмаков", "+79097387721", Role.DRIVER);
        }

        return user;
    }

    public static DbController getInstance() {
        if(instance == null){
            instance = new DbController();
        }

        return instance;
    }
}
