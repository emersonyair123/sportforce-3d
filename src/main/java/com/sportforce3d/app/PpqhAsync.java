package com.sportforce3d.app;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class PpqhAsync {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhAsync.class);
    private static final AtomicInteger PPQH_COUNTER = new AtomicInteger();
    private static final ExecutorService PPQH_EXECUTOR = Executors.newFixedThreadPool(4, ppqhR -> {
        Thread ppqhT = new Thread(ppqhR, "sf-worker-" + PPQH_COUNTER.incrementAndGet());
        ppqhT.setDaemon(true);
        return ppqhT;
    });

    private PpqhAsync() {
    }

    public static <T> Task<T> ppqhRun(Callable<T> ppqhWork, Consumer<T> ppqhOnSuccess, Consumer<Throwable> ppqhOnError) {
        Task<T> ppqhTask = new Task<>() {
            @Override
            protected T call() throws Exception {
                return ppqhWork.call();
            }
        };
        ppqhTask.setOnSucceeded(ppqhE -> ppqhOnSuccess.accept(ppqhTask.getValue()));
        ppqhTask.setOnFailed(ppqhE -> {
            Throwable ppqhError = ppqhTask.getException();
            ppqhOnError.accept(ppqhError);
        });
        PPQH_EXECUTOR.execute(ppqhTask);
        return ppqhTask;
    }

    public static void ppqhFx(Runnable ppqhAction) {
        if (Platform.isFxApplicationThread()) {
            ppqhAction.run();
        } else {
            Platform.runLater(ppqhAction);
        }
    }

    public static void ppqhShutdown() {
        PPQH_EXECUTOR.shutdownNow();
        ppqhLog.debug("Ejecutor de tareas detenido");
    }
}
