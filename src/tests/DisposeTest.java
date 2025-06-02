package tests;

import core.Disposable;
import org.junit.jupiter.api.Test;
import core.Observable;
import core.Observer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;

class DisposeTest {

    @Test
    void disposeStopsFurtherEvents() throws InterruptedException {
        List<Integer> out = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable<Integer> infinite = Observable.create(obs -> {
            int i = 0;
            while (true) {
                obs.onNext(i++);
                Thread.sleep(10);
            }
        });

        final Disposable[] ref = new Disposable[1];

        ref[0] = infinite.subscribeOn(core.Scheduler.io()).subscribe(new Observer<Integer>() {
            @Override public void onNext(Integer item) {
                out.add(item);
                if (item == 5) {
                    ref[0].dispose();
                    latch.countDown();
                }
            }
            @Override
            public void onError(Throwable t) {
                fail(t);
            }
            @Override
            public void onComplete() { }
        });

        assertTrue(latch.await(1, java.util.concurrent.TimeUnit.SECONDS));
        int sizeAfterDispose = out.size();
        Thread.sleep(100);
        assertEquals(sizeAfterDispose, out.size(), "после dispose размер не должен расти");
    }
}
