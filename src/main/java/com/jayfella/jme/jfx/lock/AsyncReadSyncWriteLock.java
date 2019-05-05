package com.jayfella.jme.jfx.lock;

/**
 * The interface for implementing async reading and sync writing lock.
 *
 * @author JavaSaBr
 */
public interface AsyncReadSyncWriteLock {

    /**
     * Lock any writing for reading.
     */
    void asyncLock();

    /**
     * Finish this reading and unlock any writing if it is last reading.
     */
    void asyncUnlock();

    /**
     * Lock any reading for writing.
     */
    void syncLock();

    /**
     * Finish this writing and unlock any readings.
     */
    void syncUnlock();
}