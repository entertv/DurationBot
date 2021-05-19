package model;

import model.exceptions.ManySeparatorsException;
import model.exceptions.PathIsNotDefined;
import model.exceptions.SeparatorException;
import model.exceptions.SimilarPoints;

import java.time.Duration;
import java.util.regex.Pattern;

public class DurationModel {

    private String origin;
    private String destination;

    private String[] separators;

    private Duration driving;
    private Duration transit;
    private Duration bicycling;
    private Duration walking;

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public Duration getDriving() {
        return driving;
    }

    public Duration getWalking() {
        return walking;
    }

    public Duration getBicycling() {
        return bicycling;
    }

    public Duration getTransit() {
        return transit;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setSeparators(String[] separators) {
        this.separators = separators;
    }

    public void createPathPoints(String request) throws SimilarPoints, SeparatorException, ManySeparatorsException {
        String separator = findSeparator(request);
        String[] pathPoints = request.split(Pattern.quote(separator));

        this.origin = pathPoints[0].strip();
        this.destination = pathPoints[1].strip();

        if (this.origin.equals(this.destination)) {
            throw new SimilarPoints();
        }
    }

    public void createDuration(Duration driving, Duration transit,
                               Duration bicycling, Duration walking) throws PathIsNotDefined {
        this.driving = driving;
        this.transit = transit;
        this.bicycling = bicycling;
        this.walking = walking;

        if (this.driving == null &&
                this.transit == null &&
                this.bicycling == null &&
                this.walking == null) {
            throw new PathIsNotDefined();
        }
    }

    public String findSeparator(String request) throws SeparatorException, ManySeparatorsException {
        String separator = null;
        String rqt = request;

        for (String text : separators) {
            if (rqt.contains(text)) {
                separator = text;
                rqt = rqt.replaceFirst(Pattern.quote(separator), "");

                for (String subSeparator : separators) {
                    if (rqt.contains(subSeparator)) {
                        throw new ManySeparatorsException();
                    }
                }
            }
        }

        if (separator == null) {
            throw new SeparatorException();
        }

        return separator;
    }

}
