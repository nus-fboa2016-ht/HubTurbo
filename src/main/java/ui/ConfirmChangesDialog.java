package ui;

import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class ConfirmChangesDialog extends Alert {

    private static final String DIALOG_TITLE = "Save Changes?";
    private static final String DIALOG_HEADER = "There are unsaved changes to your current board.";
    private static final String DIALOG_CONTENT = "Save them?";

    public ConfirmChangesDialog(Stage stage) {
        super(AlertType.CONFIRMATION);
        initOwner(stage);
        setTitle(DIALOG_TITLE);
        setHeaderText(DIALOG_HEADER);
        setContentText(DIALOG_CONTENT);
    }
}
