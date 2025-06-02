package tests;

import org.junit.jupiter.api.Test;
import core.Observable;
import core.Observer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SanityTest {
    @Test
    void simpleEmission() {
        List<Integer> received = new ArrayList<>();

        Observable.<Integer>create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onComplete();
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                received.add(item);
            }
            @Override
            public void onError(Throwable t) {
                fail(t);
            }

            @Override
            public void onComplete() {

            }
        }).dispose();

        assertEquals(Arrays.asList(1, 2), received);
    }
}
