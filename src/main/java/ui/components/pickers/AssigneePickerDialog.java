package ui.components.pickers;

import backend.resource.TurboIssue;
import backend.resource.TurboUser;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class AssigneePickerDialog extends Dialog<String> {
    public static final String DIALOG_TITLE = "Select Assignee";
    private final List<PickerAssignee> assignees = new ArrayList<>();
    private VBox assigneeBox;

    public AssigneePickerDialog(Stage stage, TurboIssue issue, List<TurboUser> assignees) {
        initOwner(stage);
        setTitle(DIALOG_TITLE);
        setupButtons(getDialogPane());
        convertToPickerAssignees(issue, assignees);
        refreshUI();
        setupKeyEvents(getDialogPane());
    }

    private void setupKeyEvents(Node assigneeDialogPane) {
        assigneeDialogPane.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.RIGHT) {
                highlightNextAssignee(this.assignees);
                event.consume();
            }
            if (event.getCode() == KeyCode.LEFT) {
                highlightPreviousAssignee(this.assignees);
                event.consume();
            }
            if (event.getCode() == KeyCode.DOWN) {
                selectAssignee(this.assignees);
            }
            if (event.getCode() == KeyCode.UP) {
                unselectAssignee(this.assignees);
            }
            refreshUI();
        });
    }

    private void selectAssignee(List<PickerAssignee> assignees) {
        if (!hasHighlightedAssignee(assignees)) return;

        PickerAssignee highlightedAssignee = getHighlightedAssignee(assignees);

        assignees.stream()
                .forEach(assignee -> {
                    assignee.setSelected(assignee == highlightedAssignee);
                });
    }

    private boolean hasHighlightedAssignee(List<PickerAssignee> assignees) {
        return assignees.stream()
                .filter(assignee -> assignee.isHighlighted())
                .findAny()
                .isPresent();
    }

    private void unselectAssignee(List<PickerAssignee> assignees) {
        if (!hasHighlightedAssignee(assignees)) return;

        PickerAssignee highlightedAssignee = getHighlightedAssignee(assignees);

        assignees.stream()
                .forEach(assignee -> {
                    assignee.setSelected((assignee == highlightedAssignee) ? false : assignee.isSelected());
                });
    }

    private PickerAssignee getHighlightedAssignee(List<PickerAssignee> assignees) {
        return assignees.stream()
                .filter(assignee -> assignee.isHighlighted())
                .findAny()
                .get();
    }

    private void highlightNextAssignee(List<PickerAssignee> assignees) {
        if (assignees.isEmpty()) return;

        PickerAssignee curAssignee = getHighlightedAssignee(assignees);
        int curAssigneeIndex = assignees.indexOf(curAssignee);
        PickerAssignee nextAssignee = getNextAssignee(assignees, curAssigneeIndex);
        nextAssignee.setHighlighted(true);
        curAssignee.setHighlighted(false);
    }

    private PickerAssignee getNextAssignee(List<PickerAssignee> assignees, int curAssigneeIndex) {
        return (curAssigneeIndex < assignees.size() - 1)
                ? assignees.get(curAssigneeIndex + 1)
                : assignees.get(0);
    }

    private void highlightPreviousAssignee(List<PickerAssignee> assignees) {
        if (assignees.isEmpty()) return;

        PickerAssignee curAssignee = getHighlightedAssignee(assignees);
        int curAssigneeIndex = assignees.indexOf(curAssignee);
        PickerAssignee nextAssignee = getPreviousAssignee(assignees, curAssigneeIndex);
        nextAssignee.setHighlighted(true);
        curAssignee.setHighlighted(false);
    }

    private PickerAssignee getPreviousAssignee(List<PickerAssignee> assignees, int curAssigneeIndex) {
        return (curAssigneeIndex > 0)
                ? assignees.get(curAssigneeIndex - 1)
                : assignees.get(assignees.size() - 1);
    }

    private void convertToPickerAssignees(TurboIssue issue, List<TurboUser> assignees) {
        for (int i = 0; i < assignees.size(); i++) {
            this.assignees.add(new PickerAssignee(assignees.get(i), this));
        }

        //TODO yy implement comparable for sorting
        //Collections.sort(this.assignees);
        selectAssignedAssignee(issue);

        if (hasSelectedAssignee()) {
            highlightSelectedAssignee();
        } else {
            highlightFirstAssignee();
        }
    }

    private void highlightFirstAssignee() {
        if (!this.assignees.isEmpty()) {
            this.assignees.get(0).setHighlighted(true);
        }
    }

    private void highlightSelectedAssignee() {
        this.assignees.stream()
                .filter(assignee -> assignee.isSelected())
                .findAny()
                .get()
                .setHighlighted(true);
    }

    private boolean hasSelectedAssignee() {
        return this.assignees.stream()
                .filter(assignee -> assignee.isSelected())
                .findAny()
                .isPresent();
    }

    private PickerAssignee getSelectedAssignee() {
        return this.assignees.stream()
                .filter(assignee -> assignee.isSelected())
                .findAny()
                .get();
    }

    private void selectAssignedAssignee(TurboIssue issue) {
        this.assignees.stream()
                .filter(assignee -> {
                    if (issue.getAssignee().isPresent()) {
                        return issue.getAssignee().get() == assignee.getLoginName();
                    } else {
                        return false;
                    }
                })
                .forEach(assignee -> assignee.setSelected(true));
    }

    private void setupButtons(DialogPane assigneePickerDialogPane) {
        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        setConfirmResultConverter(confirmButtonType);

        assigneePickerDialogPane.getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
    }

    private void setConfirmResultConverter(ButtonType confirmButtonType) {
        setResultConverter((dialogButton) -> {
            if (dialogButton == confirmButtonType && hasSelectedAssignee()) {

                //TODO yy need a integer value instead of string
                return getSelectedAssignee().getLoginName();
                //return 111;
            } else {
                return null;
            }
        });
    }

    private void refreshUI() {
        assigneeBox = new VBox();

        FlowPane assigneeFlowPlane = createAssigneeGroup();
        populateAssignee(assignees, assigneeFlowPlane);

        assigneeBox.getChildren().add(new Label("Assignees"));
        assigneeBox.getChildren().add(assigneeFlowPlane);

        getDialogPane().setContent(assigneeBox);
    }

    private void populateAssignee(List<PickerAssignee> pickerAssigneesList, FlowPane assigneeFlowPane) {
        pickerAssigneesList.stream()
                .forEach(assignee -> assigneeFlowPane.getChildren().add(assignee.getNode()));
    }

    private FlowPane createAssigneeGroup() {
        FlowPane assigneeGroup = new FlowPane();
        assigneeGroup.setPadding(new Insets(3));
        assigneeGroup.setHgap(3);
        assigneeGroup.setVgap(3);
        assigneeGroup.setStyle("-fx-border-radius: 3;-fx-background-color: white;-fx-border-color: black;");
        return assigneeGroup;
    }

    public void toggleAssignee(String assigneeName) {
        this.assignees.stream()
                .forEach(assignee -> {
                    assignee.setSelected(assignee.getLoginName().equals(assigneeName)
                            && !assignee.isSelected());
                });
        refreshUI();
    }
}
