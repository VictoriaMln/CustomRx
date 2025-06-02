package core;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SimpleDisposable implements Disposable {
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private volatile Disposable inner;

    @Override
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            if (inner != null) inner.dispose();
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed.get();
    }

    public void setInner(Disposable d) {
        if (isDisposed()) {
            d.dispose();
            return;
        }
        this.inner = d;
        if (disposed.get()) {
            d.dispose();
        }
    }
}