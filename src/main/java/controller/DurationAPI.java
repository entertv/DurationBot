package controller;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Scanner;

public class DurationAPI {

    public static final String DRIVING_MODE = "driving";
    public static final String BICYCLING_MODE = "bicycling";
    public static final String WALKING_MODE = "walking";
    public static final String TRANSIT_MODE = "transit";

    private static final String DAY = "day";
    private static final String HOUR = "hour";
    private static final String MIN = "min";

    private int partParse(String str, String part) {
        String buffer = null;
        int value = 0;

        if (str.contains(part)) {
            buffer = str.substring(0, str.indexOf(part));
            value = Integer.parseInt(buffer.strip());
        }

        return value;
    }

    private String partErase(int value, String part) {
        String erase = value + " " + part;

        if (value != 1) {
            erase += "s";
        }

        erase += " ";

        return erase;
    }

    public Duration timeParse(String str) {
        int day = 0;
        int hour = 0;
        int minute = 0;

        day = partParse(str, DAY);
        str = str.replace(partErase(day, DAY), "");

        hour += partParse(str, HOUR);
        str = str.replace(partErase(hour, HOUR), "");

        minute += partParse(str, MIN);

        Duration time = Duration.ofDays(day);
        time = time.plusHours(hour);
        time = time.plusMinutes(minute);

        return time;
    }

    public Duration googleJSONRead(String content){
        String result = null;

        JSONObject object = new JSONObject(content);
        JSONArray routes = object.getJSONArray("routes");
        JSONObject routesObj = routes.getJSONObject(0);
        JSONArray legs = routesObj.getJSONArray("legs");
        JSONObject legsObj = legs.getJSONObject(0);
        JSONObject required = legsObj.getJSONObject("duration");

        result = required.getString("text");
        return timeParse(result);
    }

    public Duration getContent(String origin, String destination, String travelMode) {
        StringBuilder content = new StringBuilder();
        Duration duration;
        try {
            URL url = new URL(
                    "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                            origin.replace(" ", "") +
                            "&destination=" +
                            destination.replace(" ", "") +
                            "&mode=" +
                            travelMode +
                            "&key=AIzaSyAJk7qZaJ2hhvHSkaXdBiBJJDPq__2PZkQ"
            );

            Scanner in = new Scanner((InputStream) url.getContent());
            while (in.hasNext()) {
                content.append(in.nextLine());
            }

            duration = googleJSONRead(content.toString());
        } catch (IOException | JSONException exception) {
            duration = null;
        }

        return duration;
    }
}
