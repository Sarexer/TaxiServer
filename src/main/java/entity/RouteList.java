package entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RouteList {
    Map<Integer, Car> routeList = new ConcurrentHashMap<>();

    public RouteList() {
        init();
    }

    void init(){
        Car sed = new Car("Huyndai Solaris", "orange", "T000T00", 4, false);
        Car pick = new Car("Toyota Hilux", "silver", "T000T00", 4, true);
        Car bus = new Car("ЛИАЗ-5256", "white", "T000T00", 44, false);
        Car mbus = new Car("Нyundai H-1", "silver", "T000T00", 12, false);

        routeList.put(2,sed);
        routeList.put(3,pick);
        routeList.put(4,bus);
        routeList.put(5,mbus);
    }

    public Car check(int driverId){
        return routeList.getOrDefault(driverId, null);
    }
}
