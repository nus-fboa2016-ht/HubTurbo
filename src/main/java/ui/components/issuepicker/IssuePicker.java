package ui.components.issuepicker;

import java.util.List;
import java.util.Optional;

import backend.resource.MultiModel;
import javafx.application.Platform;
import javafx.stage.Stage;
import ui.UI;
import util.events.ShowIssuePickerEventHandler;

public class IssuePicker {

    private final Stage stage;

    public IssuePicker(UI ui, Stage stage) {
        this.stage = stage;
        ui.registerEvent((ShowIssuePickerEventHandler) e -> 
            Platform.runLater(() -> showIssuePicker(ui.logic.getModels(), e.isStandalone)));
    }

    private Optional<List<String>> showIssuePicker(MultiModel models, boolean isStandalone) {
        IssuePickerDialog issuePickerDialog = new IssuePickerDialog(stage, models, isStandalone);
        // show IssuePickerDialog and wait for result
        return issuePickerDialog.showAndWait();

    }
}
