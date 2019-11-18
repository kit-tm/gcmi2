package com.dgeiger.enhanced_framework.benchmarking;

import java.util.concurrent.locks.ReentrantLock;

public class FileWriteLock {

    private static ReentrantLock lock;
    private FileWriteLock () {}

    public static synchronized ReentrantLock get () {
        if (FileWriteLock.lock == null) {
            FileWriteLock.lock = new ReentrantLock ();
        }
        return FileWriteLock.lock;
    }

}
