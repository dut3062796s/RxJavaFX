/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.javafx.sources;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.JavaFxScheduler;
import rx.subscriptions.JavaFxSubscriptions;

import java.util.HashMap;

public final class ObservableListSource {
    private ObservableListSource() {}

    public static <T> Observable<ObservableList<T>> fromObservableList(final ObservableList<T> source) {

        return Observable.create((Observable.OnSubscribe<ObservableList<T>>) subscriber -> {
            ListChangeListener<T> listener = c -> subscriber.onNext(source);
            source.addListener(listener);
            subscriber.add(JavaFxSubscriptions.unsubscribeInEventDispatchThread(() -> source.removeListener(listener)));
        }).subscribeOn(JavaFxScheduler.getInstance());
    }

    public static <T> Observable<T> fromObservableListAdds(final ObservableList<T> source) {

        return Observable.create((Observable.OnSubscribe<T>) subscriber -> {

            ListChangeListener<T> listener = c -> {
                while (c.next()) {
                    if (c.wasAdded()) {
                        c.getAddedSubList().forEach(subscriber::onNext);
                    }
                }
            };
            source.addListener(listener);
            subscriber.add(JavaFxSubscriptions.unsubscribeInEventDispatchThread(() -> source.removeListener(listener)));

        }).subscribeOn(JavaFxScheduler.getInstance());
    }
    public static <T> Observable<T> fromObservableListRemovals(final ObservableList<T> source) {

        return Observable.create((Observable.OnSubscribe<T>) subscriber -> {

            ListChangeListener<T> listener = c -> {
                while (c.next()) {
                    if (c.wasRemoved()) {
                        c.getRemoved().forEach(subscriber::onNext);
                    }
                }
            };

            source.addListener(listener);
            subscriber.add(JavaFxSubscriptions.unsubscribeInEventDispatchThread(() -> source.removeListener(listener)));

        }).subscribeOn(JavaFxScheduler.getInstance());
    }
    public static <T> Observable<T> fromObservableListUpdates(final ObservableList<T> source) {

        return Observable.create((Observable.OnSubscribe<T>) subscriber -> {

            ListChangeListener<T> listener = c -> {
                while (c.next()) {
                    if (c.wasUpdated()) {
                        for (int i = c.getFrom(); i < c.getTo(); i++) {
                            subscriber.onNext(c.getList().get(i));
                        }
                    }
                }
            };
            source.addListener(listener);
            subscriber.add(JavaFxSubscriptions.unsubscribeInEventDispatchThread(() -> source.removeListener(listener)));

        }).subscribeOn(JavaFxScheduler.getInstance());
    }
    public static <T> Observable<ListChange<T>> fromObservableListChanges(final ObservableList<T> source) {
        return Observable.create((Observable.OnSubscribe<ListChange<T>>) subscriber -> {

            ListChangeListener<T> listener = c -> {
                while (c.next()) {
                    if (c.wasAdded()) {
                        c.getAddedSubList().forEach(v -> subscriber.onNext(ListChange.of(v,Flag.ADDED)));
                    }
                    if (c.wasRemoved()) {
                        c.getRemoved().forEach(v -> subscriber.onNext(ListChange.of(v,Flag.REMOVED)));
                    }
                    if (c.wasUpdated()) {
                        for (int i = c.getFrom(); i < c.getTo(); i++) {
                            subscriber.onNext(ListChange.of(c.getList().get(i),Flag.UPDATED));
                        }
                    }
                }
            };
            source.addListener(listener);

            subscriber.add(JavaFxSubscriptions.unsubscribeInEventDispatchThread(() -> source.removeListener(listener)));
        }).subscribeOn(JavaFxScheduler.getInstance());
    }

    public static <T> Observable<ListChange<T>> fromObservableListDistinctChanges(final ObservableList<T> source) {

        return Observable.create((Observable.OnSubscribe<ListChange<T>>) subscriber -> {

            final DupeCounter<T> dupeCounter = new DupeCounter<>();
            source.stream().forEach(dupeCounter::add);

            ListChangeListener<T> listener = c -> {

                while (c.next()) {
                    if (c.wasAdded()) {
                        c.getAddedSubList().stream().filter(v -> dupeCounter.add(v) == 1)
                                .forEach(v -> subscriber.onNext(ListChange.of(v,Flag.ADDED)));
                    }
                    if (c.wasRemoved()) {
                        c.getRemoved().stream().filter(v -> dupeCounter.remove(v) == 0)
                                .forEach(v -> subscriber.onNext(ListChange.of(v,Flag.REMOVED)));
                    }
                }
            };
            source.addListener(listener);

            subscriber.add(JavaFxSubscriptions.unsubscribeInEventDispatchThread(() -> source.removeListener(listener)));
        }).subscribeOn(JavaFxScheduler.getInstance());
    }
    public static <T,R> Observable<ListChange<T>> fromObservableListDistinctChanges(final ObservableList<T> source, Func1<T,R> mapper) {

        return Observable.create((Observable.OnSubscribe<ListChange<T>>) subscriber -> {

            final DupeCounter<R> dupeCounter = new DupeCounter<>();
            source.stream().map(mapper::call).forEach(dupeCounter::add);

            ListChangeListener<T> listener = c -> {

                while (c.next()) {
                    if (c.wasAdded()) {
                        c.getAddedSubList().stream().filter(v -> dupeCounter.add(mapper.call(v)) == 1)
                                .forEach(v -> subscriber.onNext(ListChange.of(v,Flag.ADDED)));
                    }
                    if (c.wasRemoved()) {
                        c.getRemoved().stream().filter(v -> dupeCounter.remove(mapper.call(v)) == 0)
                                .forEach(v -> subscriber.onNext(ListChange.of(v,Flag.REMOVED)));
                    }
                }
            };
            source.addListener(listener);
            subscriber.add(JavaFxSubscriptions.unsubscribeInEventDispatchThread(() -> source.removeListener(listener)));
        }).subscribeOn(JavaFxScheduler.getInstance());
    }
    public static <T,R> Observable<ListChange<R>> fromObservableListDistinctMappings(final ObservableList<T> source, Func1<T,R> mapper) {

        return Observable.create((Observable.OnSubscribe<ListChange<R>>) subscriber -> {

            final DupeCounter<R> dupeCounter = new DupeCounter<>();
            source.stream().map(mapper::call).forEach(dupeCounter::add);

            ListChangeListener<T> listener = c -> {

                while (c.next()) {
                    if (c.wasAdded()) {
                        c.getAddedSubList().stream().map(mapper::call)
                                .filter(v -> dupeCounter.add(v) == 1)
                                .forEach(v -> subscriber.onNext(ListChange.of(v,Flag.ADDED)));
                    }
                    if (c.wasRemoved()) {
                        c.getRemoved().stream().map(mapper::call)
                                .filter(v -> dupeCounter.remove(v) == 0)
                                .forEach(v -> subscriber.onNext(ListChange.of(v,Flag.REMOVED)));
                    }
                }
            };
            source.addListener(listener);
            subscriber.add(JavaFxSubscriptions.unsubscribeInEventDispatchThread(() -> source.removeListener(listener)));

        }).subscribeOn(JavaFxScheduler.getInstance());
    }

    private static final class DupeCounter<T> {
        private final HashMap<T,Integer> counts = new HashMap<>();

        public int add(T value) {
            Integer prev = counts.get(value);
            int newVal = 0;
            if (prev == null) {
                newVal = 1;
                counts.put(value, newVal);
            }  else {
                newVal = prev + 1;
                counts.put(value, newVal);
            }
            return newVal;
        }
        public int remove(T value) {
            Integer prev = counts.get(value);
            if (prev != null && prev > 0) {
                int newVal = prev - 1;
                if (newVal == 0) {
                    counts.remove(value);
                } else {
                    counts.put(value, newVal);
                }
                return newVal;
            }
            else {
                throw new IllegalStateException();
            }
        }
    }

}
