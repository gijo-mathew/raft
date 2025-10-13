package controller;

public class StopEvent extends TrafficEvent{

    public StopEvent(String light) {
        this.setType(StopEvent.class);
        this.light = light;
    }

    private String light;

    public String getLight() {
        return light;
    }

    public void setLight(String light) {
        this.light = light;
    }
}
