package net.floodlightcontroller.core.internal;

import net.floodlightcontroller.learningswitch.NativeClock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class MessageTimestampRecorder {

    private ArrayList<Long> unassignedIncomingTimes;
    private HashMap<Long, Long> upstreamMeasurements;
    private HashMap<Long, Long> downstreamMeasurements;
    private ReentrantLock lock;
    private long lastChange;
    private NativeClock nativeClock;

    public MessageTimestampRecorder(){
        this.lock = FileWriteLock.get();
        lastChange = System.currentTimeMillis();
        unassignedIncomingTimes = new ArrayList<>();
        upstreamMeasurements = new HashMap<>();
        downstreamMeasurements = new HashMap<>();
        nativeClock = new NativeClock();

        startFileWriteChecker();
    }

    private void startFileWriteChecker(){
        Timer t = new Timer();

        t.scheduleAtFixedRate(new TimerTask() {
            public void run()
            {
                if(System.currentTimeMillis() - lastChange > 3000
                        && upstreamMeasurements.size() + downstreamMeasurements.size() > 0){
                    writeToFile();
                }
            }
        },10000,3000);
    }

    private void writeToFile(){
        final HashMap<Long, Long> upstreamMeasurements = new HashMap<>(this.upstreamMeasurements);
        final HashMap<Long, Long> downstreamMeasurements = new HashMap<>(this.downstreamMeasurements);

        this.upstreamMeasurements.clear();
        this.downstreamMeasurements.clear();

        ArrayList<String> fileContent = new ArrayList<>();

        for(long time : upstreamMeasurements.keySet()){
            fileContent.add(upstreamMeasurements.get(time) + " in " + time);
        }

        for(long time : downstreamMeasurements.keySet()){
            fileContent.add(downstreamMeasurements.get(time) + " out " + time);
        }

        lock.lock();
        try {
            Files.write(Paths.get("floodlight_times.txt"), fileContent, UTF_8, APPEND, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void saveIncomingTimestamp(){
        lastChange = System.currentTimeMillis();
        unassignedIncomingTimes.add(nativeClock.getCurrentTimeMicros());
    }

    private void saveUpstreamMeasurement(long timestamp, long xid){
        lastChange = System.currentTimeMillis();
        upstreamMeasurements.put(timestamp, xid);
    }

    private void saveDownstreamMeasurement(long timestamp, long xid){
        lastChange = System.currentTimeMillis();
        downstreamMeasurements.put(timestamp, xid);
    }

    public void saveDownstreamMeasurements(List<Long> xids){
        long timestamp = nativeClock.getCurrentTimeMicros();
        for(long xid : xids) saveDownstreamMeasurement(timestamp, xid);
    }

    public void saveUpstreamXidsWithLastTimestamp(List<Long> xids){
        lastChange = System.currentTimeMillis();
        long lastTimestamp = unassignedIncomingTimes
                .remove(unassignedIncomingTimes.size() - 1);

        for(long xid : xids) saveUpstreamMeasurement(lastTimestamp, xid);
    }
}
