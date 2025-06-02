package tests;

import core.Observable;
import core.Observer;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class FlatMapErrorTest {

    @Test
    void flatMapPropagatesFirstErrorOnly() throws InterruptedException {
        List<Integer> received = new ArrayList<>();
        AtomicInteger errorCount = new AtomicInteger();
        AtomicBoolean completed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        Observable.<Integer>create(obs -> {
                    obs.onNext(1);
                    obs.onNext(2);
                    obs.onNext(3);
                    obs.onComplete();
                }).flatMap(x -> {
                    if (x == 2) {
                        return Observable.<Integer>create(o -> {
                            o.onError(new IllegalStateException("fail"));
                        });
                    }
                    return Observable.<Integer>create(o -> {
                        o.onNext(x);
                        o.onComplete();
                    });
                }).subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) {
                        received.add(item);
                    }
                    @Override public void onError(Throwable t) {
                        errorCount.incrementAndGet();
                        latch.countDown();
                    }
                    @Override public void onComplete() {
                        completed.set(true);
                    }
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS), "onError не пришёл вовремя");

        assertEquals(1, errorCount.get(), "onError должен вызываться ровно один раз");
        assertFalse(completed.get(), "onComplete не должен вызываться после ошибки");
        assertEquals(List.of(1), received, "после ошибки элементы не должны поступать");
    }
}
