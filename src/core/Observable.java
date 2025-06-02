package core;

import java.util.function.Function;
import java.util.function.Predicate;

public abstract class Observable<T> {

    public abstract Disposable subscribe(Observer<? super T> observer);

    public final void subscribeLegacy(Observer<? super T> observer) {
        subscribe(observer);
    }

    public static <T> Observable<T> create(OnSubscribe<T> source) {
        return new CreateObservable<>(source);
    }

    private static final class CreateObservable<T> extends Observable<T> {
        private final OnSubscribe<T> source;

        CreateObservable(OnSubscribe<T> source) {
            this.source = source;
        }

        @Override
        public Disposable subscribe(Observer<? super T> observer) {
            SimpleDisposable d = new SimpleDisposable();
            try {
                source.subscribe(wrapObserver(observer, d));
            } catch (Throwable t) {
                if (!d.isDisposed()) observer.onError(t);
            }
            return d;
        }

        private <U> Observer<U> wrapObserver(Observer<U> actual, SimpleDisposable d) {
            return new Observer<>() {
                @Override
                public void onNext(U item) {
                    if (!d.isDisposed()) actual.onNext(item);
                }
                @Override
                public void onError(Throwable t) {
                    if (!d.isDisposed()) actual.onError(t);
                }
                @Override
                public void onComplete() {
                    if (!d.isDisposed()) actual.onComplete();
                }
            };
        }
    }
    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        return new operators.MapObservable<>(this, mapper);
    }

    public Observable<T> filter(Predicate<? super T> predicate) {
        return new operators.FilterObservable<>(this, predicate);
    }

    public <R> Observable<R> flatMap(Function<? super T, Observable<? extends R>> mapper) {
        return new operators.FlatMapObservable<>(this, mapper);
    }

    public Observable<T> subscribeOn(core.Scheduler scheduler) {
        Observable<T> source = this;
        return new Observable<>() {
            @Override
            public Disposable subscribe(Observer<? super T> downstream) {
                SimpleDisposable gate = new SimpleDisposable();

                scheduler.execute(() -> {
                    Disposable up = source.subscribe(new Observer<T>() {
                        @Override
                        public void onNext(T item) {
                            if (!gate.isDisposed()) downstream.onNext(item);
                        }

                        @Override
                        public void onError(Throwable t) {
                            downstream.onError(t);
                        }

                        @Override
                        public void onComplete() {
                            downstream.onComplete();
                        }
                    });
                    gate.setInner(up);
                });
                return gate;
            }
        };
    }

    public static <T> Observable<T> just(T item) {
        return create(obs -> {
            obs.onNext(item);
            obs.onComplete();
        });
    }

    @SafeVarargs
    public static <T> Observable<T> just(T... items) {
        return create(obs -> {
            for (T t : items) obs.onNext(t);
            obs.onComplete();
        });
    }

    public Observable<T> observeOn(core.Scheduler scheduler) {
        Observable<T> source = this;
        return new Observable<>() {
            @Override
            public Disposable subscribe(Observer<? super T> downstream) {
                return source.subscribe(new Observer<>() {
                    @Override
                    public void onNext(T item) {
                        scheduler.execute(() -> downstream.onNext(item));
                    }

                    @Override
                    public void onError(Throwable t) {
                        scheduler.execute(() -> downstream.onError(t));
                    }

                    @Override
                    public void onComplete() {
                        scheduler.execute(downstream::onComplete);
                    }
                });
            }
        };
    }
}
