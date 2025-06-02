package tests;

import core.Observable;
import core.Observer;
import core.Scheduler;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ObserveOnThreadTest {

    @Test
    void observeOnSwitchesThread() throws InterruptedException {
        String mainThread = Thread.currentThread().getName();
        AtomicReference<String> signalThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Scheduler io = Scheduler.io();

        Observable.just(1).observeOn(io).subscribe(new Observer<Integer>() {
            @Override public void onNext(Integer item) {
                signalThread.set(Thread.currentThread().getName());
                latch.countDown();
            }
            @Override public void onError(Throwable t) {}
            @Override public void onComplete() {}
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS), "onNext не вызван");
        assertNotEquals(mainThread, signalThread.get(), "observeOn должен перенести обработку " +
                "на другой поток");
    }
}
