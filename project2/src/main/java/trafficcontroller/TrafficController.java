package trafficcontroller;

import trafficcontroller.controller.TimerThread;
import trafficcontroller.controller.TrafficEvent;
import trafficcontroller.controller.TrafficEventProcessor;
import trafficcontroller.io.ButtonListener;
import trafficcontroller.io.LightResolver;

import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

public class TrafficController {

    static LinkedBlockingQueue<TrafficEvent> concurrentLinkedQueue = new LinkedBlockingQueue<>();

    public TrafficController() throws SocketException {

    }

    public static void main(String[] args) throws InterruptedException, SocketException {

        int ewPort = 10000;
        int nsPort = 11000;
        LightResolver lightResolver = new LightResolver(ewPort, nsPort);
        lightResolver.updateLight("EW", "G");
        lightResolver.updateLight("NS", "R");

        ButtonListener ewbuttonListener = new ButtonListener(20000, concurrentLinkedQueue);
        Thread buttonThread = new Thread(ewbuttonListener);
        buttonThread.start();

        ButtonListener nsListener = new ButtonListener(21000, concurrentLinkedQueue);
        Thread nsThread = new Thread(nsListener);
        nsThread.start();

        TrafficEventProcessor trafficEventProcessor = new TrafficEventProcessor(concurrentLinkedQueue, lightResolver);

        Thread controllerThread = new Thread(() -> {
            try {
                trafficEventProcessor.execute();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        controllerThread.start();

        TimerThread timer = new TimerThread(concurrentLinkedQueue);
        Thread timerThread = new Thread(timer);
        timerThread.start();

        Thread.currentThread().join();  // main thread waits forever

    }
}

