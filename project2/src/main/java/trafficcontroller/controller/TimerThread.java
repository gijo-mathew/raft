package trafficcontroller.controller;

import java.util.concurrent.LinkedBlockingQueue;


public class TimerThread implements Runnable{


    private LinkedBlockingQueue<TrafficEvent> concurrentLinkedQueue;
    public TimerThread(LinkedBlockingQueue<TrafficEvent> concurrentLinkedQueue) {
        this.concurrentLinkedQueue = concurrentLinkedQueue;
    }

    @Override
    public void run() {
        long nextTick = System.currentTimeMillis() + 1000;

        while (true) {
            long now = System.currentTimeMillis();

            if (now >= nextTick) {
                try {
                    concurrentLinkedQueue.put(new TimerEvent());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // exit if interrupted
                }
                nextTick += 1000; // schedule next tick
            }

            // sleep a little to avoid busy waiting
            long sleepTime = nextTick - System.currentTimeMillis();
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
