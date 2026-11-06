package com.sportforce3d.viewmodel;

import com.sportforce3d.app.Async;
import com.sportforce3d.model.Address;
import com.sportforce3d.model.CartSummary;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.service.AddressService;
import com.sportforce3d.service.CartService;
import com.sportforce3d.service.OrderService;
import com.sportforce3d.service.ValidationException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CheckoutViewModel {

    private record Loaded(List<Address> addresses, CartSummary summary) {
    }

    private final AddressService addressService;
    private final CartService cartService;
    private final OrderService orderService;

    private final ObservableList<Address> addresses = FXCollections.observableArrayList();
    private final ObjectProperty<Long> addressId = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentMethod> method = new SimpleObjectProperty<>(PaymentMethod.YAPE);
    private final StringProperty notes = new SimpleStringProperty("");
    private final ObjectProperty<CartSummary> summary = new SimpleObjectProperty<>();
    private final BooleanProperty placing = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");

    public CheckoutViewModel(AddressService addressService, CartService cartService, OrderService orderService) {
        this.addressService = addressService;
        this.cartService = cartService;
        this.orderService = orderService;
    }

    public void load() {
        Async.run(() -> new Loaded(addressService.list(), cartService.summary()),
                loaded -> {
                    setAddresses(loaded.addresses(), null);
                    summary.set(loaded.summary());
                },
                e -> error.set(ErrorMessages.of(e)));
    }

    private void setAddresses(List<Address> list, Long prefer) {
        addresses.setAll(list);
        Long keep = prefer != null ? prefer : addressId.get();
        boolean stillThere = keep != null && list.stream().anyMatch(a -> a.id() == keep);
        addressId.set(stillThere ? keep
                : list.stream().filter(Address::isDefault).map(Address::id).findFirst()
                .orElse(list.isEmpty() ? null : list.get(0).id()));
    }

    /** Guarda (crea o edita) y la deja elegida. Los errores por campo van a {@code onFieldErrors}. */
    public void saveAddress(Address form, Runnable onSuccess, Consumer<Map<String, String>> onFieldErrors,
                            Consumer<String> onError) {
        Async.run(() -> {
                    Address saved = addressService.save(form);
                    return Map.entry(saved, addressService.list());
                },
                result -> {
                    setAddresses(result.getValue(), result.getKey().id());
                    onSuccess.run();
                },
                e -> {
                    if (e instanceof ValidationException v) {
                        onFieldErrors.accept(v.fieldErrors());
                    } else {
                        onError.accept(ErrorMessages.of(e));
                    }
                });
    }

    public void deleteAddress(long id, Consumer<String> onError) {
        Async.run(() -> {
                    addressService.delete(id);
                    return addressService.list();
                },
                list -> setAddresses(list, null),
                e -> onError.accept(ErrorMessages.of(e)));
    }

    public void placeOrder(Consumer<OrderDetail> onSuccess, Consumer<String> onError) {
        if (addressId.get() == null) {
            onError.accept("Agrega o elige una dirección de envío.");
            return;
        }
        placing.set(true);
        long address = addressId.get();
        PaymentMethod m = method.get();
        String n = notes.get();
        Async.run(() -> orderService.placeOrder(address, m, n),
                order -> {
                    placing.set(false);
                    onSuccess.accept(order);
                },
                e -> {
                    placing.set(false);
                    onError.accept(ErrorMessages.of(e));
                    // El carrito pudo cambiar (stock): se recarga el resumen.
                    Async.run(cartService::summary, summary::set, ignored -> { });
                });
    }

    public ObservableList<Address> addresses() { return addresses; }
    public ObjectProperty<Long> addressIdProperty() { return addressId; }
    public ObjectProperty<PaymentMethod> methodProperty() { return method; }
    public StringProperty notesProperty() { return notes; }
    public ObjectProperty<CartSummary> summaryProperty() { return summary; }
    public BooleanProperty placingProperty() { return placing; }
    public StringProperty errorProperty() { return error; }
}
