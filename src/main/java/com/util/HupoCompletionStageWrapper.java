package com.util;

import com.http.FutureConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class HupoCompletionStageWrapper<T> extends CompletableFuture<T> {
    private static final Logger logger = LoggerFactory.getLogger(HupoCompletionStageWrapper.class);

    public CompletableFuture<T> getDelegated() {
        return delegated;
    }

    public void setDelegated(CompletableFuture<T> delegated) {
        this.delegated = delegated;
    }

    private CompletableFuture<T> delegated;
    private ExecutorService executorService = FutureConverter.hupoForkJoinExecutor();

    public HupoCompletionStageWrapper(CompletionStage<T> delegated) {
        this.delegated = delegated.toCompletableFuture();
    }

    private CompletableFuture<T> getWrappedStage(CompletableFuture<T> parent) {
        if (parent instanceof HupoCompletionStageWrapper) {
            return getWrappedStage(((HupoCompletionStageWrapper) parent).getDelegated());
        } else {
            return parent;
        }
    }

    public HupoCompletionStageWrapper(CompletableFuture<T> delegated) {
        this.delegated = getWrappedStage(delegated);
        this.delegated.whenCompleteAsync((response, error) -> {
            if (error != null) {
                this.completeExceptionally(error);
            } else {
                this.complete(response);
            }
        }, executorService);
    }

    public HupoCompletionStageWrapper() {
        super();
    }

    @Override
    public boolean isDone() {
        return delegated.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return delegated.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegated.get(timeout, unit);
    }

    @Override
    public T join() {
        return delegated.join();
    }

    @Override
    public T getNow(T valueIfAbsent) {
        return delegated.getNow(valueIfAbsent);
    }

    @Override
    public boolean complete(T value) {
        super.complete(value);
        return delegated.complete(value);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        super.completeExceptionally(ex);
        return delegated.completeExceptionally(ex);
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return this.thenApplyAsync(fn);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        //避免使用commonPool
        return new HupoCompletionStageWrapper<>(delegated.thenApplyAsync(fn, executorService));
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.thenApplyAsync(fn, executor));
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        //避免使用commonPool
        return this.thenAcceptAsync(action);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        //避免使用commonPool
        return new HupoCompletionStageWrapper<>(delegated.thenAcceptAsync(action, executorService));
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.thenAcceptAsync(action, executor));
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        return this.thenRunAsync(action);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return new HupoCompletionStageWrapper<>(delegated.thenRunAsync(action, executorService));
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.thenRunAsync(action, executor));
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        //避免使用commonPool, 因为有相当的代码用了这个函数，所以直接改成异步的
        return this.thenCombineAsync(other, fn);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return new HupoCompletionStageWrapper<>(delegated.thenCombineAsync(other, fn, executorService));
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.thenCombineAsync(other, fn, executor));
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return this.thenAcceptBothAsync(other, action);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return new HupoCompletionStageWrapper<>(delegated.thenAcceptBothAsync(other, action, executorService));
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.thenAcceptBothAsync(other, action, executor));
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return this.runAfterBothAsync(other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return new HupoCompletionStageWrapper<>(delegated.runAfterBothAsync(other, action, executorService));
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.runAfterBothAsync(other, action, executor));
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return this.applyToEitherAsync(other, fn);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return new HupoCompletionStageWrapper<>(delegated.applyToEitherAsync(other, fn, executorService));
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.applyToEitherAsync(other, fn, executor));
    }

    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return this.acceptEitherAsync(other, action);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return new HupoCompletionStageWrapper<>(delegated.acceptEitherAsync(other, action, executorService));
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.acceptEitherAsync(other, action, executor));
    }

    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return this.runAfterEitherAsync(other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return new HupoCompletionStageWrapper<>(delegated.runAfterEitherAsync(other, action, executorService));
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.runAfterEitherAsync(other, action, executor));
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return this.thenComposeAsync(fn);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return new HupoCompletionStageWrapper<>(delegated.thenComposeAsync(fn, executorService));
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.thenComposeAsync(fn, executor));
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return this.whenCompleteAsync(action);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return new HupoCompletionStageWrapper<>(delegated.whenCompleteAsync(action, executorService));
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.whenCompleteAsync(action, executor));
    }

    @Override
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return this.handleAsync(fn);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return new HupoCompletionStageWrapper<>(delegated.handleAsync(fn, executorService));
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return new HupoCompletionStageWrapper<>(delegated.handleAsync(fn, executor));
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return new HupoCompletionStageWrapper<>(delegated);
    }

    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return new HupoCompletionStageWrapper<>(delegated.exceptionally(fn));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegated.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegated.isCancelled();
    }

    @Override
    public boolean isCompletedExceptionally() {
        return delegated.isCompletedExceptionally();
    }

    @Override
    public void obtrudeValue(T value) {
        delegated.obtrudeValue(value);
    }

    @Override
    public void obtrudeException(Throwable ex) {
        delegated.obtrudeException(ex);
    }

    @Override
    public int getNumberOfDependents() {
        return delegated.getNumberOfDependents();
    }
}
