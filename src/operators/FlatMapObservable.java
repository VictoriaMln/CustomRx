package operators;

import core.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class FlatMapObservable<T, R> extends Observable<R> {

    private final Observable<T> source;
    private final Function<? super T, ? extends Observable<? extends R>> mapper;

    public FlatMapObservable(Observable<T> source, Function<? super T, ? extends Observable<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public Disposable subscribe(Observer<? super R> downstream) {
        CompositeDisposable set = new CompositeDisposable();
        AtomicInteger wip = new AtomicInteger(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Disposable upstream = source.subscribe(new Observer<T>() {
            @Override
            public void onNext(T t) {
                Observable<? extends R> inner;
                try {
                    inner = mapper.apply(t);
                } catch (Throwable ex) {
                    onError(ex);
                    return;
                }
                if (inner == null) {
                    onError(new NullPointerException("Mapper returned null Observable"));
                    return;
                }
                wip.incrementAndGet();
                Disposable d = inner.subscribe(new Observer<R>() {
                    @Override
                    public void onNext(R r) {
                        if (!set.isDisposed()) {
                            downstream.onNext(r);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (error.compareAndSet(null, e)) {
                            downstream.onError(e);
                            set.dispose();
                        }
                        doneInner();
                    }

                    @Override
                    public void onComplete() {
                        doneInner();
                    }

                    void doneInner() {
                        if (wip.decrementAndGet() == 0 && error.get() == null) {
                            downstream.onComplete();
                        }
                    }
                });
                set.add(d);
            }

            @Override
            public void onError(Throwable e) {
                if (error.compareAndSet(null, e)) {
                    downstream.onError(e);
                    set.dispose();
                }
            }

            @Override
            public void onComplete() {
                if (wip.decrementAndGet() == 0 && error.get() == null) {
                    downstream.onComplete();
                }
            }
        });

        set.add(upstream);
        return set;
    }

    static final class CompositeDisposable implements Disposable {
        private final ConcurrentLinkedQueue<Disposable> resources = new ConcurrentLinkedQueue<>();
        private volatile boolean disposed;

        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                Disposable d;
                while ((d = resources.poll()) != null) {
                    d.dispose();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        void add(Disposable d) {
            if (!disposed) {
                resources.offer(d);
                if (disposed) {
                    d.dispose();
                }
            } else {
                d.dispose();
            }
        }
    }
}