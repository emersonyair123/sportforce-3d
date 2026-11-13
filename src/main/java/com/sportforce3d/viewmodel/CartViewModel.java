package com.sportforce3d.viewmodel;

import com.sportforce3d.app.Async;
import com.sportforce3d.model.CartSummary;
import com.sportforce3d.service.CartService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class CartViewModel {

    private final CartService cart;
    private final ObjectProperty<CartSummary> summary = new SimpleObjectProperty<>();
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");

    public CartViewModel(CartService cart) {
        this.cart = cart;
    }

    public void load() {
        run(cart::summary, s -> { }, message -> error.set(message));
    }

    public void updateQuantity(long itemId, int quantity, Consumer<String> onError) {
        run(() -> cart.updateQuantity(itemId, quantity), s -> { }, onError);
    }

    public void remove(long itemId, Runnable onSuccess, Consumer<String> onError) {
        run(() -> cart.remove(itemId), s -> onSuccess.run(), onError);
    }

    private void run(Callable<CartSummary> work, Consumer<CartSummary> onSuccess, Consumer<String> onError) {
        busy.set(true);
        error.set("");
        Async.run(work,
                s -> {
                    busy.set(false);
                    summary.set(s);
                    onSuccess.accept(s);
                },
                e -> {
                    busy.set(false);
                    onError.accept(ErrorMessages.of(e));
                    Async.run(cart::summary, summary::set, ignored -> { });
                });
    }

    public ObjectProperty<CartSummary> summaryProperty() { return summary; }
    public BooleanProperty busyProperty() { return busy; }
    public StringProperty errorProperty() { return error; }
}
