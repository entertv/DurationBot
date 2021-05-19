package controller;

import model.DurationModel;
import org.joda.time.Days;

import java.time.Duration;

public class ShowContent {

    private static final String HELP_COMMAND = "/help";
    private static final String DURATION_COMMAND = "/duration";

    private static final String HELP_MESSAGE =
            "I can help you calculate the time " + "\n" +
                    "it takes to get from point A to point B." + "\n\n" +
                    "You can control me by sending" + "\n" +
                    DURATION_COMMAND + "\n\n" +
                    "After that you will have to enter " + "\n" +
                    "your *origin* and *destination* " + "\n" +
                    "according to the given rule:" + "\n\n" +
                    "<street>,<house>,<city>,<region>,<country>" + "\n\n" +
                    "For example:" + "\n" +
                    "_1) Paris > London_" + "\n" +
                    "_2) Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine > Kyrpychova St, 2, Kharkiv, Kharkivs'ka Oblast, Ukraine_";

    private static final String DURATION_MESSAGE = "Please set origin and destination: ";

    public String getHelpCommand() {
        return HELP_COMMAND;
    }

    public String getDurationCommand() {
        return DURATION_COMMAND;
    }

    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    public String getDurationMessage() {
        return DURATION_MESSAGE;
    }

    private static final String DRIVING_MODE = "driving";
    private static final String BICYCLING_MODE = "bicycling";
    private static final String WALKING_MODE = "walking";
    private static final String TRANSIT_MODE = "transit";

    private static final String DAY = "day";
    private static final String HOUR = "hour";
    private static final String MIN = "min";

    private static final String DRIVING_ICON = "\uD83D\uDE97";
    private static final String TRANSIT_ICON = "\uD83D\uDE8C";
    private static final String BICYCLING_ICON = "\uD83D\uDEB2";
    private static final String WALKING_ICON = "\uD83D\uDEB6";

    private static final String SHOW_DURATION_MESSAGE = "The duration time:";
    private static final String SPACE = " ";
    private static final String DASH = " - ";

    public String editPart(long duration, String part) {
        String str = "";

        if (duration == 1) {
            str = duration + " " + part + " ";
        } else if (duration != 0) {
            str = duration + " " + part + "s ";
        }

        return str;
    }

    public String showPart(Duration duration) {
        String durationStr = "";

        if (duration == null || duration.toDaysPart() > 366) {
            durationStr = "is not defined";
        } else {
            durationStr += editPart(duration.toDaysPart(), DAY);
            durationStr += editPart(duration.toHoursPart(), HOUR);
            durationStr += editPart(duration.toMinutesPart(), MIN);
        }
        return durationStr;
    }

    public String showDuration(DurationModel model) {
        return SHOW_DURATION_MESSAGE + "\n\n" +
                "from " + model.getOrigin() + SPACE + "to" + SPACE + model.getDestination() + "\n\n" +
                DRIVING_ICON + SPACE + DRIVING_MODE + DASH + showPart(model.getDriving()) + "\n" +
                TRANSIT_ICON + SPACE + TRANSIT_MODE + DASH + showPart(model.getTransit()) + "\n" +
                BICYCLING_ICON + SPACE + BICYCLING_MODE + DASH + showPart(model.getBicycling()) + "\n" +
                WALKING_ICON + SPACE + WALKING_MODE + DASH + showPart(model.getWalking());
    }
}
