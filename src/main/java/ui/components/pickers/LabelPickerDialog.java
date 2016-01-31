package ui.components.pickers;

import backend.resource.TurboIssue;
import backend.resource.TurboLabel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

import ui.UI;

public class LabelPickerDialog extends Dialog<List<String>> {

    private static final int ELEMENT_MAX_WIDTH = 108;
    private final LabelPickerUILogic uiLogic;

    @FXML
    private VBox mainLayout;
    @FXML
    private Label title;
    @FXML
    private FlowPane assignedLabels;
    @FXML
    private TextField queryField;
    @FXML
    private VBox feedbackLabels;

    LabelPickerDialog(TurboIssue issue, List<TurboLabel> repoLabels, Stage stage) {
        LabelPickerState initialState = new LabelPickerState(new HashSet<String>(issue.getLabels()));
        uiLogic = new LabelPickerUILogic(initialState, 
            repoLabels.stream().map(TurboLabel::getActualName)
            .collect(Collectors.toSet()));

        // UI creation
        initUI(stage, issue);
        setupEvents(stage);
        Platform.runLater(queryField::requestFocus);
    }

    // Initilisation of UI

    private void initialiseDialog(Stage stage, TurboIssue issue) {
        initOwner(stage);
        initModality(Modality.APPLICATION_MODAL); // TODO change to NONE for multiple dialogs
        setTitle("Edit Labels for " + (issue.isPullRequest() ? "PR #" : "Issue #") +
                issue.getId() + " in " + issue.getRepoId());
    }

    private void positionDialog(Stage stage) {
        if (!Double.isNaN(getHeight())) {
            setX(stage.getX() + stage.getScene().getX());
            setY(stage.getY() +
                 stage.getScene().getY() +
                 (stage.getScene().getHeight() - getHeight()) / 2);
        }
    }


    private void initUI(Stage stage, TurboIssue issue) {
        initialiseDialog(stage, issue);
        setDialogPaneContent();
        title.setTooltip(createTitleTooltip(issue));
        createButtons();
    }

    private void setDialogPaneContent() {
        try {
            createMainLayout();
            getDialogPane().setContent(mainLayout);
        } catch (IOException e) {
            // TODO use a HTLogger instead when failed to load fxml
            e.printStackTrace();
        }
    }

    private void createMainLayout() throws IOException {
        FXMLLoader loader = new FXMLLoader(UI.class.getResource("fxml/LabelPickerView.fxml"));
        loader.setController(this);
        mainLayout = (VBox) loader.load();
    }

    // TODO returns result via showAndWait
    private void createButtons() {
        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // defines what happens when user confirms/presses enter
        setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                // TODO return assignedLabels from current state
                return null;
            }
            return null;
        });
    }

    private Tooltip createTitleTooltip(TurboIssue issue) {
        Tooltip titleTooltip = new Tooltip(
                (issue.isPullRequest() ? "PR #" : "Issue #") + issue.getId() + ": " + issue.getTitle());
        titleTooltip.setWrapText(true);
        titleTooltip.setMaxWidth(500);
        return titleTooltip;
    }

    private void setupEvents(Stage stage) {
        setupKeyEvents();

        showingProperty().addListener(e -> {
            positionDialog(stage);
        });
    }

    private void setupKeyEvents() {
        queryField.setOnKeyPressed(e -> {
            if (!e.isAltDown() && !e.isMetaDown() && !e.isControlDown()) {
                uiLogic.getNewState(e, queryField.getText().toLowerCase());
            }
        });
    }

    // Populate UI elements with LabelPickerState

    /**
     * Updates ui elements based on current state
     * @param state
     */
    public void updateUI(LabelPickerState state) {
        List<String> initialLabels = state.getInitialLabels();
        List<String> addedLabels = state.getAddedLabels();
        List<String> removedLabels = state.getRemovedLabels();
        Optional<String> suggestion = state.getCurrentSuggestion();
        
        // Population of UI elements
        populateAssignedLabels(initialLabels, addedLabels, removedLabels, suggestion);
    }

    private boolean hasNoLabels(List<String> initialLabels, 
                                List<String> addedLabels) {
        return initialLabels.isEmpty() && addedLabels.isEmpty();
    }

    private Label createTextLabel(String input) {
        Label label = new Label(input);
        label.setPadding(new Insets(2, 5, 2, 5));
        return label;
    }

    private List<Label> populateInitialLabels(List<String> initialLabels, 
                                               List<String> removedLabels,
                                               Optional<String> suggestion) {
        return initialLabels.stream().filter(label -> !removedLabels.contains(label))
            .map(label -> createSolidLabel(label, suggestion))
            .collect(Collectors.toList());
    }


    // TODO Given added list how to know which one is faded and strike
    private List<Label> populateAddedLabels(List<String> addedLabels, 
        Optional<String> suggestion) {
        List<Label> nextAddedLabels =  addedLabels.stream()
            .map(label -> createSolidLabel(label, suggestion))
            .collect(Collectors.toList());

        // Add faded label to indicated suggested but not added 
        if (suggestion.isPresent() && !nextAddedLabels.contains(suggestion)) {
           nextAddedLabels.add(createFadedLabel(suggestion.get()));
        }
        return nextAddedLabels;
    }

    private void populateAssignedLabels(List<String> initialLabels, 
                                        List<String> addedLabels, 
                                        List<String> removedLabels,
                                        Optional<String> suggestion) {
        assignedLabels.getChildren().clear();
        List<Label> nextInitialLabels = populateInitialLabels(initialLabels, 
                removedLabels, suggestion);
        List<Label> nextAddedLabels = populateAddedLabels(addedLabels, suggestion);
        if (hasNoLabels(initialLabels, addedLabels)) {
            Label label = createTextLabel("No currently selected labels. ");
            assignedLabels.getChildren().add(label);
        } else {
            nextInitialLabels.forEach(label -> assignedLabels.getChildren().add(label));
            if (!nextAddedLabels.isEmpty()) {
                assignedLabels.getChildren().add(new Label("|"));
                assignedLabels.getChildren().addAll(nextAddedLabels);
            }
        } 
        
        
    }

    private void populateBottomBox(List<PickerLabel> bottomLabels, Map<String, Boolean> groups) {
        feedbackLabels.getChildren().clear();
        if (bottomLabels.isEmpty()) {
            Label label = new Label("No labels in repository. ");
            label.setPadding(new Insets(2, 5, 2, 5));
            feedbackLabels.getChildren().add(label);
        } else {
            List<String> groupNames = groups.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
            Collections.sort(groupNames, String.CASE_INSENSITIVE_ORDER);

            groupNames.stream().forEach(group -> {
                Label groupName = new Label(group + (groups.get(group) ? "." : "-"));
                groupName.setPadding(new Insets(0, 5, 5, 0));
                groupName.setMaxWidth(ELEMENT_MAX_WIDTH - 10);
                groupName.setStyle("-fx-font-size: 110%; -fx-font-weight: bold;");

                FlowPane groupPane = new FlowPane();
                groupPane.setHgap(5);
                groupPane.setVgap(5);
                groupPane.setPadding(new Insets(0, 0, 10, 10));
                bottomLabels
                        .stream()
                        .filter(label -> label.getGroup().isPresent())
                        .filter(label -> label.getGroup().get().equalsIgnoreCase(group))
                        .forEach(label -> groupPane.getChildren().add(label.getNode()));
                feedbackLabels.getChildren().addAll(groupName, groupPane);
            });

            FlowPane noGroup = new FlowPane();
            noGroup.setHgap(5);
            noGroup.setVgap(5);
            noGroup.setPadding(new Insets(5, 0, 0, 0));
            bottomLabels
                    .stream()
                    .filter(label -> !label.getGroup().isPresent())
                    .forEach(label -> noGroup.getChildren().add(label.getNode()));
            if (noGroup.getChildren().size() > 0) feedbackLabels.getChildren().add(noGroup);
        }
    }

    // Utility UI methods

    // TODO determine color of label
    private Label createBasicLabel(String name) {
        Label label = new Label(name);
        label.getStyleClass().add("labels");
        FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();
        double width = (double) fontLoader.computeStringWidth(label.getText(), label.getFont());
        label.setPrefWidth(width + 30);
        return label;
    }

    private Label createFadedLabel(String name) {
        Label label = createBasicLabel(name);
        String suggestRemoveStyle = "fx-border-color:black; -fx-opacity:-40; ";
        label.setStyle(suggestRemoveStyle);
        return label;
    }

    private Label createSolidLabel(String name, Optional<String> suggestion) {
        if (suggestion.isPresent() && suggestion.equals(name)) {
            Label striked = createFadedLabel(name);
            striked.getStyleClass().add("labels-removed");
            return striked;
        }
        return createBasicLabel(name);
    }

}
