package ui.components.pickers;

import backend.resource.TurboUser;
import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class PickerAssignee extends TurboUser {

    boolean isSelected;
    boolean isHighlighted;
    AssigneePickerDialog assigneePickerDialog;

    PickerAssignee(TurboUser user, AssigneePickerDialog assigneePickerDialog) {
        super(user);
        this.assigneePickerDialog = assigneePickerDialog;
    }

    public Node getNode() {
        Label assignee = new Label(getLoginName());
        FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();
        double width = fontLoader.computeStringWidth(assignee.getText(), assignee.getFont());
        assignee.setPrefWidth(width + 35);
        assignee.getStyleClass().add("labels");
        assignee.setStyle("-fx-background-color: yellow;");

        if (isSelected) {
            assignee.setText(assignee.getText() + " ✓");
        }

        if (isHighlighted) {
            assignee.setStyle("-fx-border-color: black;");
        }

        assignee.setOnMouseClicked(e -> assigneePickerDialog.toggleAssignee(getLoginName()));

        return assignee;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public void setHighlighted(boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
    }

    public boolean isHighlighted() {
        return this.isHighlighted;
    }

}
