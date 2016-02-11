package ui.components.pickers;

import backend.resource.TurboIssue;
import backend.resource.TurboUser;
import javafx.application.Platform;
import javafx.stage.Stage;
import ui.UI;
import undo.actions.ChangeAssigneeAction;
import util.events.ShowAssigneePickerEventHandler;

import java.util.List;
import java.util.Optional;

public class AssigneePicker {

    private final UI ui;
    private final Stage stage;

    public AssigneePicker(UI ui, Stage stage) {
        this.ui = ui;
        this.stage = stage;
        ui.registerEvent((ShowAssigneePickerEventHandler) e -> Platform.runLater(() -> showAssigneePicker(e.issue)));
    }

    private void showAssigneePicker(TurboIssue issue) {

        List<TurboUser> assigneeList = ui.logic.getRepo(issue.getRepoId()).getUsers();
        AssigneePickerDialog assigneePickerDialog = new AssigneePickerDialog(stage, issue, assigneeList);
        Optional<String> assignedAssignee = assigneePickerDialog.showAndWait();


        if (!issue.getAssignee().equals(assignedAssignee)) {

            //TODO yy expects an integer assignee id
            ui.undoController.addAction(issue, new ChangeAssigneeAction(ui.logic, issue.getAssignee(), assignedAssignee));
            //ui.undoController.addAction(issue, new ChangeAssigneeAction(ui.logic, Optional.of(111), Optional.of(222)));
        }
    }
}
