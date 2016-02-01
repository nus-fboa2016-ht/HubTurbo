package ui.components.pickers;

import backend.resource.TurboIssue;
import backend.resource.TurboLabel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

import ui.UI;

public class LabelPickerDialog extends Dialog<List<String>> {

    private static final int ELEMENT_MAX_WIDTH = 108;
    private final LabelPickerUILogic uiLogic;
    private final List<TurboLabel> repoLabels;
    private final Set<String> repoLabelsString;
    private final Map<String, Boolean> groups;
    private final TurboIssue issue;

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
        this.repoLabels = repoLabels;
        this.repoLabelsString = repoLabels.stream()
                .map(TurboLabel::getActualName)
                .collect(Collectors.toSet());
        this.issue = issue;
        LabelPickerState initialState = new LabelPickerState(new HashSet<String>(issue.getLabels()));
        uiLogic = new LabelPickerUILogic();

        // Logic initialisation
        this.groups = initGroups(repoLabels);

        // UI creation
        initUI(stage, issue);
        updateUI(initialState);
        setupEvents(stage);
        Platform.runLater(queryField::requestFocus);
    }

    // Init Assets 
    private Map<String, Boolean> initGroups(List<TurboLabel> repoLabels) {
        Map<String, Boolean> groups = new HashMap<>();
        repoLabels.forEach(label -> {
            if (label.getGroup().isPresent() && !groups.containsKey(label.getGroup().get())) {
                groups.put(label.getGroup().get(), label.isExclusive());
            }        
        });
        return groups;
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
                uiLogic.determineState(new LabelPickerState(new HashSet<String>(issue.getLabels())),
                        repoLabelsString,
                        queryField.getText().toLowerCase());
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
        List<String> matchedLabels = state.getMatchedLabels();
        List<String> assignedLabels = state.getAssignedLabels();
        Optional<String> suggestion = state.getCurrentSuggestion();
       
        // Population of UI elements
        populateAssignedLabels(initialLabels, addedLabels, removedLabels, suggestion);
        populateFeedbackLabels(initialLabels, assignedLabels, matchedLabels, suggestion);
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
        return initialLabels.stream()
            .map(label -> createBasicLabel(label))
            .map(label -> processInitialLabel(label, removedLabels, suggestion))
            .collect(Collectors.toList());
    }


    private Label processAddedLabel(String name, Optional<String> suggestion) {
        Label label = createBasicLabel(name);
        if (suggestion.isPresent() && suggestion.get().equals(label.getText())) {
            setFadedLabel(label);
            setStrikedLabel(label);
        }
        return label;
    }

    // TODO Given added list how to know which one is faded and strike
    private List<Label> populateAddedLabels(List<String> addedLabels, 
        Optional<String> suggestion) {
        List<Label> nextAddedLabels =  addedLabels.stream()
            .map(label -> processAddedLabel(label, suggestion))
            .collect(Collectors.toList());

        // Faded and striked when already present in addedLabels
        if (suggestion.isPresent() && !addedLabels.contains(suggestion.get())) {
            Label addedLabel = createBasicLabel(suggestion.get());
            setFadedLabel(addedLabel);
            nextAddedLabels.add(addedLabel);
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

    private void populateFeedbackLabels(List<String> initialLabels,
                                        List<String> assignedLabels,
                                        List<String> matchedLabels,
                                        Optional<String> suggestion) {
        feedbackLabels.getChildren().clear();
        if (initialLabels.isEmpty()) {
            Label label = new Label("No labels in repository. ");
            label.setPadding(new Insets(2, 5, 2, 5));
            feedbackLabels.getChildren().add(label);
        } else {
            List<String> groupNames = getGroupNames(groups);
            groupNames.forEach(group -> {
                addGroup(repoLabels, groups, group, matchedLabels, assignedLabels,
                         suggestion);
            });
            addNoGroup(matchedLabels, assignedLabels, suggestion);
        }
    }
                                        
    // Utility methods

    // TODO remove side effect
    private void setSelected(Label label) {
        label.setText(label.getText() + " ✓" );
    }

    // TODO Remove side effect 
    private void addNoGroup(List<String> matchedLabels, 
                                List<String> assignedLabels,
                                Optional<String> suggestion) {
        FlowPane noGroup = new FlowPane();
        noGroup.setHgap(5);
        noGroup.setVgap(5);
        noGroup.setPadding(new Insets(5, 0, 0, 0));
        repoLabels.stream()
            .filter(label -> !label.getGroup().isPresent())
            .map(label -> processFeedbackLabel(
                createBasicLabel(label.getActualName()), 
                matchedLabels, assignedLabels, suggestion))
            .forEach(label -> {
                noGroup.getChildren().add(label);
            });
        //addSelected(labels, matchedLabels);
        if (noGroup.getChildren().size() > 0) {
            feedbackLabels.getChildren().add(noGroup);
        }

    }

    private Label processGroupLabels(TurboLabel label, List<String> matchedLabels,
                                     List<String> assignedLabels, 
                                     Optional<String> suggestion) {
        Label newLabel = createBasicLabel(label.getActualName());
        if (matchedLabels.size() > 0 && !matchedLabels.contains(label.getActualName())) {
            setFadedLabel(newLabel);
        }

        if (suggestion.isPresent() && suggestion.get().equals(label.getActualName())) {
            setSuggestedLabel(newLabel);
        }
        
        if (assignedLabels.contains(label.getActualName())) {
            setSelected(newLabel);
        }
        return newLabel;
    }

    private Label processFeedbackLabel(Label label, List<String> matchedLabels, 
                                       List<String> assignedLabels, 
                                       Optional<String> suggestion) {
        if (matchedLabels.size() > 0 && !matchedLabels.contains(label.getText())) {
            setFadedLabel(label);
        }

        if (suggestion.isPresent() && suggestion.get().equals(label.getText())) {
            setSuggestedLabel(label);
        }
        
        if (assignedLabels.contains(label.getText())) {
            setSelected(label);
        }
        return label;
    }

    private void addGroup(List<TurboLabel> repoLabels, 
                          Map<String, Boolean> groups, String group,
                          List<String> matchedLabels, List<String> assignedLabels,
                          Optional<String> suggestion) {
        Label groupName = new Label(group + (groups.get(group) ? "." : "-"));
        groupName.setPadding(new Insets(0, 5, 5, 0));
        groupName.setMaxWidth(ELEMENT_MAX_WIDTH - 10);
        groupName.setStyle("-fx-font-size: 110%; -fx-font-weight: bold;");

        FlowPane groupPane = new FlowPane();
        groupPane.setHgap(5);
        groupPane.setVgap(5);
        groupPane.setPadding(new Insets(0, 0, 10, 10));
        repoLabels
                .stream()
                .filter(label -> label.getGroup().isPresent())
                .filter(label -> label.getGroup().get().equalsIgnoreCase(group))
                .map(label -> processGroupLabels(label, matchedLabels, 
                                                 assignedLabels, suggestion))
                .forEach(label -> {
                  groupPane.getChildren().add(label);
                });
        feedbackLabels.getChildren().addAll(groupName, groupPane);
    }

    private String getColour(String name, List<TurboLabel> repoLabels) {
        String colour = repoLabels.stream().filter(
            label -> label.getActualName().equals(name)).findAny().get().getColour();
        return colour;
    }

    public String getStyle(String name, List<TurboLabel> repoLabels) {
        String colour = getColour(name, repoLabels);
        int r = Integer.parseInt(colour.substring(0, 2), 16);
        int g = Integer.parseInt(colour.substring(2, 4), 16);
        int b = Integer.parseInt(colour.substring(4, 6), 16);
        double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        boolean bright = luminance > 128;
        return "-fx-background-color: #" + colour + "; -fx-text-fill: " + (bright ? "black;" : "white;");
    }


    private Label processInitialLabel(Label label, List<String> removedLabels, 
                                     Optional<String> suggestion) {
        // initial label only faded when it is already removed
        if (removedLabels.contains(label.getText())
            && suggestion.isPresent() && suggestion.get().equals(label.getText())) {
            setFadedLabel(label);
        } 
        
        if (suggestion.isPresent() && suggestion.get().equals(label.getText())) {
           setStrikedLabel(label);
           setFadedLabel(label); 
        }
        return label;
    }

    private void handleClick(String labelName) {
        // Disable text field upon clicking on a label
        queryField.setDisable(true);
    }

    // TODO handling group label text which contains partial name only
    private Label createBasicLabel(String name) {
        Label label = new Label(name);
        label.getStyleClass().add("labels");
        label.setStyle(getStyle(name, this.repoLabels));
        FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();
        double width = (double) fontLoader.computeStringWidth(label.getText(), label.getFont());
        label.setPrefWidth(width + 30);
        label.setOnMouseClicked(e -> handleClick(label.getText()));
        return label;
    }

    private void setSuggestedLabel(Label label) {
        String suggestRemoveStyle = label.getStyle() + 
            "-fx-border-color:black; -fx-border-width:2px; ";
        label.setStyle(suggestRemoveStyle);
    }

    private void setFadedLabel(Label label) {
        String suggestRemoveStyle = label.getStyle() + 
            "-fx-opacity: 40%; ";
        label.setStyle(suggestRemoveStyle);
    }

    private void setStrikedLabel(Label label) {
        label.getStyleClass().add("labels-removed");
    }

    private List<String> getGroupNames(Map<String, Boolean> groups) {
        List<String> groupNames = groups.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
        Collections.sort(groupNames);
        return groupNames;
    }
}
