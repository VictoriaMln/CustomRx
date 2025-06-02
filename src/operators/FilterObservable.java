package operators;

import core.Disposable;
import core.Observer;
import core.Observable;
import java.util.function.Predicate;

public final class FilterObservable<T> extends Observable<T> {

    private final Observable<T> source;
    private final Predicate<? super T> predicate;

    public FilterObservable(Observable<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    public Disposable subscribe(Observer<? super T> downstream) {
        return source.subscribe(new Observer<T>() {
            boolean terminated;

            @Override
            public void onNext(T item) {
                if (terminated) return;
                boolean pass;
                try {
                    pass = predicate.test(item);
                } catch (Throwable ex) {
                    terminated = true;
                    downstream.onError(ex);
                    return;
                }
                if (pass) {
                    downstream.onNext(item);
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
