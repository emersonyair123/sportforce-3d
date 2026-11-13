package com.sportforce3d.view;

import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.model.PpqhAddress;
import com.sportforce3d.util.PpqhPeruRegions;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;

final class PpqhAddressForm extends VBox {

    private final Label ppqhTitle = new Label();
    private final PpqhSfTextField ppqhLabel = new PpqhSfTextField("Nombre de la dirección");
    private final PpqhSfTextField ppqhRecipient = new PpqhSfTextField("Quién recibe");
    private final PpqhSfTextField ppqhPhone = new PpqhSfTextField("Celular");
    private final ComboBox<String> ppqhDepartment = new ComboBox<>();
    private final Label ppqhDepartmentError = new Label();
    private final PpqhSfTextField ppqhProvince = new PpqhSfTextField("Provincia");
    private final PpqhSfTextField ppqhDistrict = new PpqhSfTextField("Distrito");
    private final PpqhSfTextField ppqhLine1 = new PpqhSfTextField("Dirección");
    private final PpqhSfTextField ppqhReference = new PpqhSfTextField("Referencia (opcional)");
    private final CheckBox ppqhIsDefault = new CheckBox("Usar como dirección predeterminada");
    final PpqhSfButton ppqhSave = new PpqhSfButton("Guardar dirección", PpqhSfButton.PpqhVariant.PRIMARY);
    final PpqhSfButton ppqhCancel = new PpqhSfButton("Cancelar", PpqhSfButton.PpqhVariant.GHOST);
    private long ppqhEditingId;

    PpqhAddressForm() {
        super(12);
        getStyleClass().add("sf-card");
        setStyle("-fx-padding: 20;");
        ppqhTitle.getStyleClass().addAll("title-s", "ink");
        ppqhLabel.setPromptText("Casa, Oficina…");
        ppqhPhone.setPromptText("987654321");
        ppqhLine1.setPromptText("Jr. Mariano Núñez 245, dpto. 3");

        ppqhDepartment.getItems().setAll(PpqhPeruRegions.PPQH_DEPARTMENTS);
        ppqhDepartment.setPromptText("Elige…");
        ppqhDepartment.setMaxWidth(Double.MAX_VALUE);
        ppqhDepartment.setAccessibleText("Departamento");
        Label ppqhDepLabel = new Label("DEPARTAMENTO");
        ppqhDepLabel.getStyleClass().add("sf-field-label");
        ppqhDepartmentError.getStyleClass().add("sf-field-error");
        ppqhDepartmentError.managedProperty().bind(ppqhDepartmentError.textProperty().isNotEmpty());
        ppqhDepartmentError.visibleProperty().bind(ppqhDepartmentError.managedProperty());
        VBox ppqhDepBox = new VBox(4, ppqhDepLabel, ppqhDepartment, ppqhDepartmentError);

        HBox ppqhRow1 = ppqhGrow(ppqhLabel, ppqhRecipient, ppqhPhone);
        HBox ppqhRow2 = ppqhGrow(ppqhDepBox, ppqhProvince, ppqhDistrict);
        HBox ppqhActions = new HBox(12, ppqhCancel, ppqhSave);
        ppqhActions.setAlignment(Pos.CENTER_RIGHT);
        getChildren().addAll(ppqhTitle, ppqhRow1, ppqhRow2, ppqhLine1, ppqhReference, ppqhIsDefault, ppqhActions);
    }

    private static HBox ppqhGrow(javafx.scene.Node... ppqhNodes) {
        HBox ppqhBox = new HBox(12, ppqhNodes);
        for (javafx.scene.Node ppqhN : ppqhNodes) {
            HBox.setHgrow(ppqhN, Priority.ALWAYS);
            if (ppqhN instanceof javafx.scene.layout.Region ppqhR) {
                ppqhR.setPrefWidth(200);
                ppqhR.setMinWidth(120);
            }
        }
        return ppqhBox;
    }

    void ppqhEdit(PpqhAddress ppqhA) {
        ppqhClearErrors();
        ppqhEditingId = ppqhA == null ? 0 : ppqhA.ppqhId();
        ppqhTitle.setText(ppqhA == null ? "Nueva dirección" : "Editar dirección");
        ppqhLabel.setText(ppqhA == null ? "Casa" : ppqhA.ppqhLabel());
        ppqhRecipient.setText(ppqhA == null ? "" : ppqhA.ppqhRecipient());
        ppqhPhone.setText(ppqhA == null ? "" : ppqhA.ppqhPhone());
        ppqhDepartment.setValue(ppqhA == null ? null : ppqhA.ppqhDepartment());
        ppqhProvince.setText(ppqhA == null ? "" : ppqhA.ppqhProvince());
        ppqhDistrict.setText(ppqhA == null ? "" : ppqhA.ppqhDistrict());
        ppqhLine1.setText(ppqhA == null ? "" : ppqhA.ppqhLine1());
        ppqhReference.setText(ppqhA == null || ppqhA.ppqhReference() == null ? "" : ppqhA.ppqhReference());
        ppqhIsDefault.setSelected(ppqhA != null && ppqhA.ppqhIsDefault());
    }

    void ppqhPrefill(String ppqhFullName, String ppqhPhoneNumber) {
        if (ppqhRecipient.getText() == null || ppqhRecipient.getText().isBlank()) {
            ppqhRecipient.setText(ppqhFullName);
        }
        if ((ppqhPhone.getText() == null || ppqhPhone.getText().isBlank()) && ppqhPhoneNumber != null) {
            ppqhPhone.setText(ppqhPhoneNumber);
        }
    }

    PpqhAddress ppqhValue() {
        return new PpqhAddress(ppqhEditingId, ppqhLabel.getText(), ppqhRecipient.getText(), ppqhPhone.getText(), ppqhDepartment.getValue(),
                ppqhProvince.getText(), ppqhDistrict.getText(), ppqhLine1.getText(), ppqhReference.getText(), ppqhIsDefault.isSelected());
    }

    void ppqhShowErrors(Map<String, String> ppqhErrors) {
        ppqhClearErrors();
        ppqhLabel.setError(ppqhErrors.get("label"));
        ppqhRecipient.setError(ppqhErrors.get("recipient"));
        ppqhPhone.setError(ppqhErrors.get("phone"));
        ppqhDepartmentError.setText(ppqhErrors.getOrDefault("department", ""));
        ppqhProvince.setError(ppqhErrors.get("province"));
        ppqhDistrict.setError(ppqhErrors.get("district"));
        ppqhLine1.setError(ppqhErrors.get("line1"));
        ppqhReference.setError(ppqhErrors.get("reference"));
    }

    private void ppqhClearErrors() {
        for (PpqhSfTextField ppqhF : new PpqhSfTextField[]{ppqhLabel, ppqhRecipient, ppqhPhone, ppqhProvince, ppqhDistrict, ppqhLine1, ppqhReference}) {
            ppqhF.setError("");
        }
        ppqhDepartmentError.setText("");
    }
}
