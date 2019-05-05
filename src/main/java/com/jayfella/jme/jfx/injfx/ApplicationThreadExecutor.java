package com.jayfella.jme.jfx.injfx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The executor for executing tasks in application thread.
 *
 * @author JavaSaBr
 */
public class ApplicationThreadExecutor {

    private static final ApplicationThreadExecutor INSTANCE = new ApplicationThreadExecutor();

    public static ApplicationThreadExecutor getInstance() {
        return INSTANCE;
    }

    /**
     * The list of waiting tasks.
     */
    // private final ConcurrentArray<Runnable> waitTasks;
    private final CopyOnWriteArrayList<Runnable> waitTasks;

    /**
     * The list of tasks to execute.
     */
    // private final Array<Runnable> execute;
    private final List<Runnable> execute;

    private ApplicationThreadExecutor() {
        // this.waitTasks = ArrayFactory.newConcurrentAtomicARSWLockArray(Runnable.class);
        // this.execute = ArrayFactory.newArray(Runnable.class);
        this.waitTasks = new CopyOnWriteArrayList<>();
        this.execute = new ArrayList<>();
    }

    /**
     * Add the task to execute.
     *
     * @param task the new task.
     */
    public void addToExecute(Runnable task) {
        // ArrayUtils.runInWriteLock(waitTasks, task, Array::add);
        waitTasks.add(task);
    }

    /**
     * Execute the waiting tasks.
     */
    public void execute() {

        if (waitTasks.isEmpty()) {
            return;
        }

        // ArrayUtils.runInWriteLock(waitTasks, execute, ArrayUtils::move);

        //long stamp

        try {
            execute.forEach(Runnable::run);
        } finally {
            execute.clear();
        }

    }
}
