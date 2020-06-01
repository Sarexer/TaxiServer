package routes;

import entity.LatLng;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RouteBuilder {
    OkHttpClient httpClient = new OkHttpClient();

    public List<LatLng> getRoute(LatLng departure, List<LatLng> destinations){
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(buildHttpUrl(departure, destinations))
                .build();
        List<LatLng> points = null;
        try (Response response = httpClient.newCall(request).execute()) {

            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            JSONObject jsonObject = new JSONObject(response.body().string());

             points = decode(jsonObject.getString("route_geometry"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return points;
    }

    private String buildHttpUrl(LatLng departure, List<LatLng> destinations){
        String url = String.format("http://routes.maps.sputnik.ru/osrm/router/viaroute?loc=%s", departure.toString());

        for (LatLng destination : destinations) {
            url += String.format("&loc=%s", destination.toString());
        }

        url += "&alt=false";

        return url;
    };

    public List<LatLng> decode(final String encodedPath) {
        int len = encodedPath.length();

        final List<LatLng> path = new ArrayList<LatLng>();
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63-1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            path.add(new LatLng(lat * 1e-6, lng * 1e-6));
        }

        return path;
    }
}
