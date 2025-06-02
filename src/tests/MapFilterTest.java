package tests;

import org.junit.jupiter.api.Test;
import core.Observable;
import core.Observer;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MapFilterTest {
    @Test
    void mapThenFilter() {
        List<Integer> out = new ArrayList<>();

        Observable.<Integer>create(obs -> {
            for (int i = 1; i <= 3; i++) obs.onNext(i);
            obs.onComplete();
        }).map(x -> x * 2).filter(x -> x > 3).subscribe(new Observer<Integer>() {
            @Override
                public void onNext(Integer item) {
                    out.add(item);
                }

                @Override
                public void onError(Throwable t) {
                    fail(t);
                }

                @Override
                public void onComplete() {}
        }).dispose();
        assertEquals(List.of(4, 6), out);
    }
}
