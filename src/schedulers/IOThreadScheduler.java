package schedulers;

import core.Scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class IOThreadScheduler implements Scheduler {
    private final ExecutorService pool = Executors.newCachedThreadPool();

    @Override public void execute(Runnable task) {
        pool.submit(task);
    }
}