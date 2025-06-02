package operators;

import core.Disposable;
import core.Observable;
import core.Observer;
import java.util.function.Function;

public final class MapObservable<T, R> extends Observable<R> {
    private final Observable<T> source;
    private final Function<? super T, ? extends R> mapper;

    public MapObservable(Observable<T> source, Function<? super T, ? extends R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public Disposable subscribe(Observer<? super R> downstream) {
        return source.subscribe(new Observer<T>() {
            boolean terminated;

            @Override
            public void onNext(T item) {
                if (terminated) return;
                try {
                    R mapped = mapper.apply(item);
                    downstream.onNext(mapped);
                } catch (Throwable ex) {
                    terminated = true;
                    downstream.onError(ex);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (terminated) return;
                terminated = true;
                downstream.onError(t);
            }

            @Override
            public void onComplete() {
                if (terminated) return;
                terminated = true;
                downstream.onComplete();
            }
        });
    }
}
