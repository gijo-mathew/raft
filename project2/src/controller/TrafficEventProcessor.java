package controller;

import io.LightResolver;

import java.util.concurrent.LinkedBlockingQueue;

public class TrafficEventProcessor {

    private LinkedBlockingQueue<TrafficEvent> concurrentLinkedQueue;
    private long elapsed;
    private boolean EW_STOP_PENDING = false;
    private boolean NS_STOP_PENDING = false;
    private State currentState;
    private LightResolver lightResolver;

    enum State {
        EW_GREEN,
        EW_YELLOW,
        NS_GREEN,
        NS_YELLOW

    }

    public TrafficEventProcessor(LinkedBlockingQueue<TrafficEvent> concurrentLinkedQueue, LightResolver lightResolver) {
        this.concurrentLinkedQueue = concurrentLinkedQueue;
        this.elapsed = 0;
        this.lightResolver = lightResolver;
        this.currentState = State.EW_GREEN;
    }

    public void execute() throws InterruptedException {
        while(true) {
            TrafficEvent event = concurrentLinkedQueue.take();

            if(event.getType().equals(TimerEvent.class)) {
                elapsed ++;
            }
            else if(event.getType().equals(StopEvent.class)) {
                StopEvent stopEvent = (StopEvent) event;
                if(stopEvent.getLight().equals("EW")) {
                    NS_STOP_PENDING = true;
                }
                else {
                    EW_STOP_PENDING = true;
                }
            }

            switch(currentState) {
                case EW_GREEN : if(elapsed >= 30 || (elapsed >=15 && EW_STOP_PENDING)) {
                    lightResolver.updateLight("EW", "Y");
                    logState(State.EW_YELLOW);
                    currentState = State.EW_YELLOW;
                    elapsed = 0;
                    EW_STOP_PENDING = false;
                }
                break;
                case EW_YELLOW: if(elapsed >= 5) {
                    lightResolver.updateLight("EW", "R");
                    lightResolver.updateLight("NS", "G");
                    logState(State.NS_GREEN);
                    currentState = State.NS_GREEN;
                    elapsed = 0;
                }
                break;
                case NS_GREEN: if(elapsed >=60 || (elapsed >=15 && NS_STOP_PENDING)) {
                    lightResolver.updateLight("NS", "Y");
                    logState(State.NS_YELLOW);
                    currentState = State.NS_YELLOW;
                    elapsed = 0;
                    NS_STOP_PENDING = false;
                }
                break;
                case NS_YELLOW: if(elapsed >=5) {
                    lightResolver.updateLight("EW", "G");
                    lightResolver.updateLight("NS", "R");
                    logState(State.EW_GREEN);
                    currentState = State.EW_GREEN;
                    elapsed = 0;
                }
                break;
            }

        }
    }

    private void logState(State nextState){
        System.out.println("Transitioning " + this.currentState + " -> " + nextState + "at elapsed=" + elapsed);

    }


}
