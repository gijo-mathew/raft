package controller;

public class TrafficEvent<T> {
    private T type;

    public void setType(T type) {
        this.type = type;
    }


    public T getType() {
        return type;
    }
}
