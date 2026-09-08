package com.guessmarket.ui.common;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.NewEventDTO;
import com.guessmarket.dto.TradingMethodType;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;

/**
 * Modal "Add Event" form. Collects a market maker, a trading method and the
 * event's details (the method-specific fields swap when the method changes),
 * does light client-side validation, and returns a {@link NewEventDTO} when the
 * user clicks Create - or an empty {@link Optional} on Cancel.
 *
 * <p>Business rules the client can't check here (does the market maker have
 * enough for the subsidy?) are left to the engine.
 */
public final class NewEventDialog {

    private NewEventDialog() {}

    public static Optional<NewEventDTO> show(Window owner, List<String> participantNames) {
        Dialog<NewEventDTO> dialog = new Dialog<>();
        dialog.setTitle("Add Event");
        dialog.setHeaderText("Create a new event");
        dialog.initOwner(owner);
        dialog.setResizable(true);

        ButtonType createType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(createType, ButtonType.CANCEL);

        // --- Controls --------------------------------------------------------
        ComboBox<String> participantCombo = new ComboBox<>();
        participantCombo.getItems().setAll(participantNames);
        participantCombo.setMaxWidth(Double.MAX_VALUE);
        if (!participantNames.isEmpty()) {
            participantCombo.getSelectionModel().selectFirst();
        }

        ComboBox<TradingMethodType> methodCombo = new ComboBox<>();
        methodCombo.getItems().setAll(TradingMethodType.LMSR, TradingMethodType.ORDERBOOK);
        methodCombo.setConverter(new StringConverter<>() {
            @Override public String toString(TradingMethodType t) {
                return t == TradingMethodType.ORDERBOOK ? "Order Book" : t == null ? "" : "LMSR";
            }
            @Override public TradingMethodType fromString(String s) { return null; }
        });
        methodCombo.getSelectionModel().select(TradingMethodType.LMSR);
        methodCombo.setMaxWidth(Double.MAX_VALUE);

        TextField nameField = new TextField();
        TextField descriptionField = new TextField();
        TextField option1Field = new TextField();
        TextField option2Field = new TextField();
        TextField commissionField = new TextField("0");

        ComboBox<CommissionType> commissionTypeCombo = new ComboBox<>();
        commissionTypeCombo.getItems().setAll(CommissionType.ON_PURCHASE, CommissionType.ON_CLOSE);
        commissionTypeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(CommissionType t) { return t == null ? "" : t.toUIDisplay(); }
            @Override public CommissionType fromString(String s) { return null; }
        });
        commissionTypeCombo.getSelectionModel().select(CommissionType.ON_PURCHASE);
        commissionTypeCombo.setMaxWidth(Double.MAX_VALUE);

        // Method-specific fields
        Label bLabel = new Label("Liquidity parameter (b)");
        TextField bField = new TextField();

        Label dLabel = new Label("Base value (d)");
        TextField dField = new TextField();
        Label initialLabel = new Label("Initial shares per option");
        TextField initialField = new TextField("0");
        CheckBox allowMintCheck = new CheckBox("Allow minting");

        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill: #e74c3c;");
        errorLabel.setMaxWidth(360);

        // --- Layout ---------------------------------------------------------
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 4, 4, 4));
        int r = 0;
        addRow(grid, r++, "Participant (Market Maker)", participantCombo);
        addRow(grid, r++, "Trading method", methodCombo);
        addRow(grid, r++, "Event name", nameField);
        addRow(grid, r++, "Description", descriptionField);
        addRow(grid, r++, "Option 1", option1Field);
        addRow(grid, r++, "Option 2", option2Field);
        addRow(grid, r++, "Commission (%)", commissionField);
        addRow(grid, r++, "Commission charged", commissionTypeCombo);

        grid.add(bLabel, 0, r);
        grid.add(bField, 1, r++);
        grid.add(dLabel, 0, r);
        grid.add(dField, 1, r++);
        grid.add(initialLabel, 0, r);
        grid.add(initialField, 1, r++);
        grid.add(allowMintCheck, 1, r++);
        grid.add(errorLabel, 0, r, 2, 1);

        List<Node> lmsrNodes = List.of(bLabel, bField);
        List<Node> orderBookNodes = List.of(dLabel, dField, initialLabel, initialField, allowMintCheck);
        Runnable applyMethodVisibility = () -> {
            boolean lmsr = methodCombo.getValue() == TradingMethodType.LMSR;
            setShown(lmsrNodes, lmsr);
            setShown(orderBookNodes, !lmsr);
            errorLabel.setText("");
            Window w = grid.getScene() == null ? null : grid.getScene().getWindow();
            if (w != null) {
                w.sizeToScene();
            }
        };
        methodCombo.valueProperty().addListener((obs, old, val) -> applyMethodVisibility.run());
        applyMethodVisibility.run();

        dialog.getDialogPane().setContent(grid);

        // --- Validation + result -------------------------------------------
        Button createButton = (Button) dialog.getDialogPane().lookupButton(createType);
        createButton.addEventFilter(ActionEvent.ACTION, evt -> {
            String problem = firstProblem(participantCombo, methodCombo, nameField, descriptionField,
                    option1Field, option2Field, commissionField, bField, dField, initialField, allowMintCheck);
            if (problem != null) {
                errorLabel.setText(problem);
                evt.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button != createType) {
                return null;
            }
            boolean lmsr = methodCombo.getValue() == TradingMethodType.LMSR;
            return new NewEventDTO(
                    participantCombo.getValue(),
                    nameField.getText().trim(),
                    descriptionField.getText().trim(),
                    Integer.parseInt(commissionField.getText().trim()),
                    commissionTypeCombo.getValue(),
                    List.of(option1Field.getText().trim(), option2Field.getText().trim()),
                    methodCombo.getValue(),
                    lmsr ? Integer.parseInt(bField.getText().trim()) : null,
                    lmsr ? null : Integer.parseInt(dField.getText().trim()),
                    lmsr ? null : Integer.parseInt(initialField.getText().trim()),
                    !lmsr && allowMintCheck.isSelected());
        });

        return dialog.showAndWait();
    }

    private static void addRow(GridPane grid, int row, String label, Node control) {
        grid.add(new Label(label), 0, row);
        grid.add(control, 1, row);
        if (control instanceof javafx.scene.control.Control c) {
            c.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private static void setShown(List<Node> nodes, boolean shown) {
        for (Node n : nodes) {
            n.setVisible(shown);
            n.setManaged(shown);
        }
    }

    private static Integer parseInt(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstProblem(ComboBox<String> participant, ComboBox<TradingMethodType> method,
                                       TextField name, TextField description, TextField option1, TextField option2,
                                       TextField commission, TextField b, TextField d, TextField initial,
                                       CheckBox allowMint) {
        if (participant.getValue() == null) {
            return "Choose a participant to be the market maker.";
        }
        if (method.getValue() == null) {
            return "Choose a trading method.";
        }
        if (name.getText().isBlank()) {
            return "Enter an event name.";
        }
        if (description.getText().isBlank()) {
            return "Enter a description.";
        }
        String o1 = option1.getText().trim();
        String o2 = option2.getText().trim();
        if (o1.isEmpty() || o2.isEmpty()) {
            return "Enter both option names.";
        }
        if (o1.equalsIgnoreCase(o2)) {
            return "The two options must have different names.";
        }
        Integer commissionPct = parseInt(commission.getText());
        if (commissionPct == null) {
            return "Commission must be a whole number.";
        }
        if (commissionPct < 0 || commissionPct > 90) {
            return "Commission must be between 0 and 90.";
        }
        if (method.getValue() == TradingMethodType.LMSR) {
            Integer bValue = parseInt(b.getText());
            if (bValue == null) {
                return "The liquidity parameter b must be a whole number.";
            }
            if (bValue <= 0) {
                return "The liquidity parameter b must be greater than 0.";
            }
        } else {
            Integer dValue = parseInt(d.getText());
            Integer initialValue = parseInt(initial.getText());
            if (dValue == null || initialValue == null) {
                return "Base value and initial shares must be whole numbers.";
            }
            if (dValue <= 0) {
                return "The base value d must be greater than 0.";
            }
            if (initialValue < 0) {
                return "Initial shares per option cannot be negative.";
            }
            if (initialValue == 0 && !allowMint.isSelected()) {
                return "With no initial shares, minting must be enabled - otherwise no shares can ever exist.";
            }
        }
        return null;
    }
}
