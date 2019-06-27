package com.util;

import com.http.FutureConverter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class HupoCompletableFuture<T> extends HupoCompletionStageWrapper<T> {
    public HupoCompletableFuture(CompletionStage<T> future) {
        super(future.toCompletableFuture());
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, FutureConverter.hupoForkJoinExecutor());
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return new HupoCompletableFuture(CompletableFuture.supplyAsync(supplier, FutureConverter.hupoForkJoinExecutor()));
    }
}
