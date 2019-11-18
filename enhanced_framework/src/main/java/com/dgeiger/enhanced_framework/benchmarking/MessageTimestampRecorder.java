package com.dgeiger.enhanced_framework.benchmarking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class MessageTimestampRecorder {

    private static final Logger log = LoggerFactory.getLogger(MessageTimestampRecorder.class);

    private ArrayList<Long> xidsWithoutOutTimesToDownstream;
    private ArrayList<Long> xidsWithoutOutTimesToUpstream;
    private ConcurrentHashMap<Long, ArrayList<Long>> upstreamMeasurements;
    private ConcurrentHashMap<Long, ArrayList<Long>> downstreamMeasurements;
    private ReentrantLock lock;
    private long lastChange;
    private final Clock clock = Clock.systemDefaultZone();

    public MessageTimestampRecorder(){
        this.lock = FileWriteLock.get();
        lastChange = System.currentTimeMillis();
        xidsWithoutOutTimesToDownstream = new ArrayList<>();
        xidsWithoutOutTimesToUpstream = new ArrayList<>();
        upstreamMeasurements = new ConcurrentHashMap<>();
        downstreamMeasurements = new ConcurrentHashMap<>();

        startFileWriteChecker();
    }

    private void startFileWriteChecker(){
        Timer t = new Timer();

        t.scheduleAtFixedRate(new TimerTask() {
            public void run()
            {
                if(System.currentTimeMillis() - lastChange > 10000
                        && upstreamMeasurements.size() + downstreamMeasurements.size() > 1000){
                    writeToFile();
                }
            }
        },10000,10000);
    }

    private void writeToFile(){
        final HashMap<Long, ArrayList<Long>> upstreamMeasurements = new HashMap<>(this.upstreamMeasurements);
        final HashMap<Long, ArrayList<Long>> downstreamMeasurements = new HashMap<>(this.downstreamMeasurements);

        this.upstreamMeasurements.clear();
        this.downstreamMeasurements.clear();

        int counter = 0;

        ArrayList<String> fileContent = new ArrayList<>();

        for(long xid : upstreamMeasurements.keySet()){
            for(long time : upstreamMeasurements.get(xid)){
                fileContent.add(xid + " fromDownstream " + time);
                counter++;
            }
        }

        for(long xid : downstreamMeasurements.keySet()){
            for(long time : downstreamMeasurements.get(xid)){
                fileContent.add(xid + " fromUpstream " + time);
                counter++;
            }
        }

        log.debug("waiting for lock...");
        lock.lock();
        log.debug("writing " + counter + " measurements to file");
        try {
            Files.write(Paths.get("proxy_times.txt"), fileContent, UTF_8, APPEND, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private long getCurrentTimeMicros(){
        Instant instant = clock.instant();
        long seconds = instant.getEpochSecond();
        long nano = instant.getNano();
        final long nanoseconds = seconds * 1000000000 + nano;
        return nanoseconds / 1000;
    }

    private void saveUpstreamMeasurement(long xid, long time){
        lastChange = System.currentTimeMillis();
        if(!upstreamMeasurements.containsKey(xid)){
            upstreamMeasurements.put(xid, new ArrayList<>());
        }
        upstreamMeasurements.get(xid).add(time);

        if(upstreamMeasurements.get(xid).size() > 2)
            log.warn("saved " + upstreamMeasurements.get(xid).size() + " upstreammeasurements for xid " + xid);
    }

    private void saveDownstreamMeasurement(long xid, long time){
        lastChange = System.currentTimeMillis();
        if(!downstreamMeasurements.containsKey(xid)){
            downstreamMeasurements.put(xid, new ArrayList<>());
        }
        downstreamMeasurements.get(xid).add(time);

        if(downstreamMeasurements.get(xid).size() > 2)
            log.warn("saved " + downstreamMeasurements.get(xid).size() + " downstreammeasurements for xid " + xid);
    }

    public void saveInXidWithCurrentTime(boolean upDownstream, long xid){
        if(upDownstream) saveUpstreamMeasurement(xid, getCurrentTimeMicros());
        else saveDownstreamMeasurement(xid, getCurrentTimeMicros());
    }

    public void saveOutXid(long xid, boolean upDownstream){
        if(upDownstream) xidsWithoutOutTimesToUpstream.add(xid);
        else xidsWithoutOutTimesToDownstream.add(xid);
    }

    public void saveOutTimeForExistingXids(boolean upDownstream){
        long time = getCurrentTimeMicros();

        if(upDownstream){
            if(xidsWithoutOutTimesToUpstream.size() == 0)
                log.warn("xidsWithoutOutTimesToUpstream was empty");

            while(xidsWithoutOutTimesToUpstream.size() > 0){
                saveUpstreamMeasurement(xidsWithoutOutTimesToUpstream.remove(0), time);
            }
        }else{
            if(xidsWithoutOutTimesToDownstream.size() == 0)
                log.warn("xidsWithoutOutTimesToDownstream was empty");

            while(xidsWithoutOutTimesToDownstream.size() > 0){
                saveDownstreamMeasurement(xidsWithoutOutTimesToDownstream.remove(0), time);
            }
        }
    }
}
