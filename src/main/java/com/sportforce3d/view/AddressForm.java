package com.sportforce3d.view;

import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.model.Address;
import com.sportforce3d.util.PeruRegions;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;

/** Formulario de dirección de envío (crear o editar). */
final class AddressForm extends VBox {

    private final Label title = new Label();
    private final SfTextField label = new SfTextField("Nombre de la dirección");
    private final SfTextField recipient = new SfTextField("Quién recibe");
    private final SfTextField phone = new SfTextField("Celular");
    private final ComboBox<String> department = new ComboBox<>();
    private final Label departmentError = new Label();
    private final SfTextField province = new SfTextField("Provincia");
    private final SfTextField district = new SfTextField("Distrito");
    private final SfTextField line1 = new SfTextField("Dirección");
    private final SfTextField reference = new SfTextField("Referencia (opcional)");
    private final CheckBox isDefault = new CheckBox("Usar como dirección predeterminada");
    final SfButton save = new SfButton("Guardar dirección", SfButton.Variant.PRIMARY);
    final SfButton cancel = new SfButton("Cancelar", SfButton.Variant.GHOST);
    private long editingId;

    AddressForm() {
        super(12);
        getStyleClass().add("sf-card");
        setStyle("-fx-padding: 20;");
        title.getStyleClass().addAll("title-s", "ink");
        label.setPromptText("Casa, Oficina…");
        phone.setPromptText("987654321");
        line1.setPromptText("Jr. Mariano Núñez 245, dpto. 3");

        department.getItems().setAll(PeruRegions.DEPARTMENTS);
        department.setPromptText("Elige…");
        department.setMaxWidth(Double.MAX_VALUE);
        department.setAccessibleText("Departamento");
        Label depLabel = new Label("DEPARTAMENTO");
        depLabel.getStyleClass().add("sf-field-label");
        departmentError.getStyleClass().add("sf-field-error");
        departmentError.managedProperty().bind(departmentError.textProperty().isNotEmpty());
        departmentError.visibleProperty().bind(departmentError.managedProperty());
        VBox depBox = new VBox(4, depLabel, department, departmentError);

        HBox row1 = grow(label, recipient, phone);
        HBox row2 = grow(depBox, province, district);
        HBox actions = new HBox(12, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);
        getChildren().addAll(title, row1, row2, line1, reference, isDefault, actions);
    }

    private static HBox grow(javafx.scene.Node... nodes) {
        HBox box = new HBox(12, nodes);
        for (javafx.scene.Node n : nodes) {
            HBox.setHgrow(n, Priority.ALWAYS);
            if (n instanceof javafx.scene.layout.Region r) {
                r.setPrefWidth(200);
                r.setMinWidth(120);
            }
        }
        return box;
    }

    void edit(Address a) {
        clearErrors();
        editingId = a == null ? 0 : a.id();
        title.setText(a == null ? "Nueva dirección" : "Editar dirección");
        label.setText(a == null ? "Casa" : a.label());
        recipient.setText(a == null ? "" : a.recipient());
        phone.setText(a == null ? "" : a.phone());
        department.setValue(a == null ? null : a.department());
        province.setText(a == null ? "" : a.province());
        district.setText(a == null ? "" : a.district());
        line1.setText(a == null ? "" : a.line1());
        reference.setText(a == null || a.reference() == null ? "" : a.reference());
        isDefault.setSelected(a != null && a.isDefault());
    }

    /** Prefill útil para la primera dirección: nombre y celular de la cuenta. */
    void prefill(String fullName, String phoneNumber) {
        if (recipient.getText() == null || recipient.getText().isBlank()) {
            recipient.setText(fullName);
        }
        if ((phone.getText() == null || phone.getText().isBlank()) && phoneNumber != null) {
            phone.setText(phoneNumber);
        }
    }

    Address value() {
        return new Address(editingId, label.getText(), recipient.getText(), phone.getText(), department.getValue(),
                province.getText(), district.getText(), line1.getText(), reference.getText(), isDefault.isSelected());
    }

    void showErrors(Map<String, String> errors) {
        clearErrors();
        label.setError(errors.get("label"));
        recipient.setError(errors.get("recipient"));
        phone.setError(errors.get("phone"));
        departmentError.setText(errors.getOrDefault("department", ""));
        province.setError(errors.get("province"));
        district.setError(errors.get("district"));
        line1.setError(errors.get("line1"));
        reference.setError(errors.get("reference"));
    }

    private void clearErrors() {
        for (SfTextField f : new SfTextField[]{label, recipient, phone, province, district, line1, reference}) {
            f.setError("");
        }
        departmentError.setText("");
    }
}
