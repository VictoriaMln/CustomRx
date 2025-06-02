package tests;

import core.Observable;
import core.Observer;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class MapErrorTest {

    @Test
    void mapperFailurePropagatesOnce() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable.just(1).<Integer>map(x -> { throw new IllegalStateException("crash"); })
                .subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) {}
                    @Override public void onError(Throwable t) { latch.countDown(); }
                    @Override public void onComplete() { completed.set(true); }
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS), "onError не вызван");
        assertFalse(completed.get(), "onComplete не должен вызываться");
    }
}
