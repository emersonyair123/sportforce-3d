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

/**
 * Ejecuta trabajo de base de datos o BCrypt fuera del hilo de la interfaz con {@link Task} de JavaFX.
 * Los callbacks corren en el hilo de JavaFX.
 */
public final class Async {

    private static final Logger log = LoggerFactory.getLogger(Async.class);
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "sf-worker-" + COUNTER.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    private Async() {
    }

    public static <T> Task<T> run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            onError.accept(error);
        });
        EXECUTOR.execute(task);
        return task;
    }

    /** Para pruebas o casos en que ya se está en el hilo de la interfaz. */
    public static void fx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
        log.debug("Ejecutor de tareas detenido");
    }
}
