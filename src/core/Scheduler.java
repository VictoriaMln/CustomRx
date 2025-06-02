package core;

public interface Scheduler {
    void execute(Runnable task);

    static Scheduler io() {
        return new schedulers.IOThreadScheduler();
    }
    static Scheduler computation() {
        return new schedulers.ComputationScheduler();
    }
    static Scheduler single() {
        return new schedulers.SingleThreadScheduler();
    }
}