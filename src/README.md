# Custom RxJava

## О проекте

Собственная реализация библиотеки RxJava. Система реактивных потоков, включающая:

* абстракцию **Observable / Observer** для доставки данных и сигналов завершения;
* набор операторов (`map`, `filter`, `flatMap`) для декларативного преобразования стримов;
* переключение потоков выполнения через **Scheduler**‑ы (`io`, `computation`, `single`);
* механизм отмены подписки через **Disposable**.

---

## Архитектура

```text
                          +----------------+
          subscribe() --> |  Observable<T> |----------------+
                          +----------------+                |
                                 ^                          v
                                 |                  onNext/onError/onComplete
                          +----------------+                |
          implements -->  |   Observer    | <----------- subscribe()
                          +----------------+                |
                                 ^                          |
                                 |                          v
                          +----------------+        +---------------+
                          |   Operators    |<------ |  Scheduler    |
                          +----------------+        +---------------+
```

* **Observable** — поток событий.
* **Observer** — три метода обратного вызова (`onNext`, `onError`, `onComplete`).
* **Disposable** — переключатель, позволяющий отменить подписку.
* **Scheduler** — абстракция для асинхронного выполнения (`io`, `computation`, `single`).
* Операторы (`map`, `filter`, `flatMap`, `subscribeOn`, `observeOn`) реализуются как обёртки над исходным `Observable`.

### Жизненный цикл

1. Пользователь создаёт поток через `Observable.create()` или `just()`.
2. При вызове `subscribe()` возвращается `Disposable`:

    * до терминального сигнала (`onError`/`onComplete`) можно отменить обработку через `dispose()`;
    * `SimpleDisposable` каскадно отменяет **upstream**.
3. Сигналы проходят через цепочку операторов; каждый оператор может менять поток или переключать **Scheduler**.

### Schedulers

| Scheduler       | Стартовый метод      | Используемый пул                                                           | Назначение                           |
| --------------- | -------------------- | -------------------------------------------------------------------------- | ------------------------------------ |
| `io()`          | CachedThreadPool     | `Executors.newCachedThreadPool()`                                          | Работа с I/O, запросы в сеть, файлы. |
| `computation()` | FixedThreadPool(‑N)  | `Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())` | CPU‑bound задачи.                    |
| `single()`      | SingleThreadExecutor | `Executors.newSingleThreadExecutor()`                                      | Сериализация доступа к ресурсу.      |

**Внутренняя реализация Schedulers**

* **IOThreadScheduler** — оборачивает `Executors.newCachedThreadPool()` без ограничения размера; простаивающие потоки завершаются через 60 с. Потоки именуются `io-%d`, чтобы стек‑трейсы читались.
* **ComputationScheduler** — размер фиксирован и равен числу ядер (`Runtime.availableProcessors()`), что предотвращает чрезмерный контекст‑свитч на CPU‑bound задачах. Теги потоков `cpu-%d`.
* **SingleThreadScheduler** — однопоточный `ExecutorService` с именем `single-0`; гарантирует строгую последовательность и подходит для работы с неконкурентными структурами.

Метод `Scheduler.execute(Runnable)` делегирует задачу во внутренний `ExecutorService`, скрывая детали пула за единым интерфейсом.

*`subscribeOn(s)`* переносит **источник** на указанный пул, *`observeOn(s)`* — каждый входящий сигнал.

---

## Реализованные операторы

| Оператор                    | Сигнатура                                                                                 | Назначение                       |
| --------------------------- | ----------------------------------------------------------------------------------------- |----------------------------------|
| **map**                     | `<R> Observable<R> map(Function<? super T,? extends R> mapper)`                           | Преобразование элементов.        |
| **filter**                  | `Observable<T> filter(Predicate<? super T> predicate)`                                    | Фильтрование.                    |
| **flatMap**                 | `<R> Observable<R> flatMap(Function<? super T,? extends Observable<? extends R>> mapper)` | Конкатенирует внутренние потоки. |
| **subscribeOn / observeOn** | `Observable<T> …On(Scheduler s)`                                                          | Переключает поток выполнения.    |

*Любой оператор гарантирует: после терминального сигнала новые события не будут переданы дальше.*

---

## Тесты

| Категория    | Файл                                               | Покрытие                                |
| ------------ | -------------------------------------------------- |-----------------------------------------|
| Sanity‑flow  | `SanityTest`                                       | базовая передача 1‑2‑3 + onComplete.    |
| Map & Filter | `MapFilterTest`, `MapErrorTest`, `FilterErrorTest` | преобразования + ошибки.                |
| FlatMap      | `FlatMapTest`, `FlatMapErrorTest`                  | дублирование 1→1,1;2→2,2 + ошибки.      |
| Threading    | `SchedulerTest`, `ObserveOnThreadTest`             | корректность `subscribeOn`/`observeOn`. |
| Disposing    | `DisposeTest`                                      | отмена подписки (каскадная).            |

---

## Пример использования библиотеки

```java
import core.Observable;
import core.Scheduler;

public class Demo {
    public static void main(String[] args) {
        Observable.just(1, 2, 3)
                  .map(x -> x * 10)
                  .filter(x -> x > 15)
                  .subscribeOn(Scheduler.computation())
                  .observeOn(Scheduler.io())
                  .subscribe(System.out::println,
                             Throwable::printStackTrace,
                             () -> System.out.println("done"));
    }
}
```
---

## Дополнительные примеры

### 1. `flatMap` + I/O Scheduler

Читаем два текстовых файла параллельно на пуле `io()` и печатаем их длину:

```java
Observable.just("file1.txt", "file2.txt")
        .flatMap(name -> Observable.<String>create(obs -> {
            String content = Files.readString(Path.of(name)); // блокирующее чтение
            obs.onNext(content);
            obs.onComplete();
        }).subscribeOn(Scheduler.io()))
        .map(String::length)
        .subscribe(len -> System.out.println("bytes=" + len),
                   Throwable::printStackTrace);
```

### 2. Отмена подписки (`Disposable`)

```java
Disposable d = Observable.<Integer>create(obs -> {
    try {
        for (int i = 0; i < 100; i++) {
            obs.onNext(i);
            Thread.sleep(50);
        }
        obs.onComplete();
    } catch (InterruptedException e) {
        obs.onError(e);
    }
}).subscribeOn(Scheduler.computation())
  .observeOn(Scheduler.single())
  .subscribe(System.out::println);

Thread.sleep(200);
d.dispose(); // после этого новые элементы не будут доставлены downstream
```
