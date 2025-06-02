package tests;

import org.junit.jupiter.api.Test;
import core.Observable;
import core.Observer;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FlatMapTest {

    @Test
    void flatMapDuplicatesElements() {
        List<Integer> out = new ArrayList<>();

        Observable.<Integer>create(obs -> {
                    obs.onNext(1);
                    obs.onNext(2);
                    obs.onComplete();
                }).flatMap(x -> Observable.<Integer>create(o -> {
                    o.onNext(x);
                    o.onNext(x);
                    o.onComplete();
                })).subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) {
                        out.add(item);
                    }
                    @Override public void onError(Throwable t) {
                        fail(t);
                    }
                    @Override public void onComplete() {}
                }).dispose();

        assertEquals(List.of(1, 1, 2, 2), out);
    }
}
