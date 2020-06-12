package timer_tasks;

import entity.Driver;
import entity.LatLng;
import org.json.JSONObject;
import server.SparkServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class DriversInfoTask extends TimerTask {
    ArrayList<Driver> drivers;
    @Override
    public void run() {
        try {
            initDriverList();
            sendDriverList();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    void initDriverList(){
        drivers = new ArrayList<>(SparkServer.drivers.values());
        /*Driver driver = drivers.get(0);


        for(int i = 0;i<500;i++){
            Driver nDriver = (Driver) driver.clone();
            LatLng latLng = driver.getCurrentLocation();

            nDriver.setCurrentLocation(new LatLng(latLng.getLatitude() + (i % 2 == 0 ? (0.0005 * i) : (-0.001 * i)), latLng.getLongitude()));
            drivers.add(nDriver);
        }*/
    }

    void sendDriverList() throws IOException {

        Map<Integer, Driver> users = SparkServer.drivers;

        for (Driver driver : users.values()) {
            drivers.remove(driver);
            String json = prepareJson();

            try {
                driver.session().getRemote().sendString(json);
            }catch (Exception e){
                e.printStackTrace();
                //this.cancel();
            }

            drivers.add(driver);
        }
    }

    String prepareJson(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "driversInfo");
        jsonObject.put("list", drivers);

        return jsonObject.toString();
    }
}
