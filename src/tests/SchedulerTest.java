package tests;

import org.junit.jupiter.api.Test;
import core.Observable;
import core.Observer;
import core.Scheduler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class SchedulerTest {

    @Test
    void subscribeOnMoveWorkToOtherThread() throws InterruptedException {
        String mainThread = Thread.currentThread().getName();
        CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder where = new StringBuilder();

        Observable.<Integer>create(obs -> {
            where.append(Thread.currentThread().getName());
            obs.onNext(1);
            obs.onComplete();
        }).subscribeOn(Scheduler.io()).subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) { }
            @Override
            public void onError(Throwable t) {
                fail(t);
            }
            @Override
            public void onComplete() {
                latch.countDown();
            }
        }).dispose();

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotEquals(mainThread, where.toString());
    }
}
