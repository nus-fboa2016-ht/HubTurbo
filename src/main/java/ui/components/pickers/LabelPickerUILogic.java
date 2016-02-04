package ui.components.pickers;

import backend.resource.TurboIssue;
import backend.resource.TurboLabel;
import util.Utility;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LabelPickerUILogic {

    private final TurboIssue issue;
    private final LabelPickerDialog dialog;
    private final List<TurboLabel> allLabels;
    private final List<PickerLabel> topLabels = new ArrayList<>();
    private List<PickerLabel> bottomLabels;
    private final Map<String, Boolean> groups;
    private final Map<String, Boolean> resultList;
    private Optional<String> targetLabel = Optional.empty();

    // Used for multiple spaces
    private String lastAction = "";
    private int previousNumberOfActions = 0;

    LabelPickerUILogic(TurboIssue issue, List<TurboLabel> repoLabels, LabelPickerDialog dialog) {
        this.issue = issue;
        this.dialog = dialog;
        this.allLabels = populateAllLabels(repoLabels);
        this.groups = initializeGroups(repoLabels);
        this.resultList = initializeResultList(repoLabels);
        this.topLabels.addAll(determineExistingLabels(issue, allLabels));
        updateBottomLabels("");
        populatePanes();
    }

    public LabelPickerUILogic(TurboIssue issue, List<TurboLabel> repoLabels) {
        this.issue = issue;
        this.dialog = null;
        this.allLabels = populateAllLabels(repoLabels);
        this.groups = initializeGroups(repoLabels);
        this.resultList = initializeResultList(repoLabels);
        this.topLabels.addAll(determineExistingLabels(issue, allLabels));
        updateBottomLabels("");
        populatePanes();
    }

    private List<TurboLabel> populateAllLabels(List<TurboLabel> repoLabels) {
        return repoLabels.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    private Map<String, Boolean> initializeGroups(List<TurboLabel> repoLabels) {
        return repoLabels.stream()
                .collect(Collectors.toMap(
                        TurboLabel::getActualName,
                        label -> issue.getLabels().contains(label.getActualName())));
    }

    private Map<String, Boolean> initializeResultList(List<TurboLabel> repoLabels) {
        return repoLabels.stream()
                .filter(label -> label.getGroup().isPresent() && !groups.containsKey(label.getGroup().get()))
                .collect(Collectors.toMap(
                        label -> label.getGroup().get(),
                        TurboLabel::isExclusive));
    }

    private void populatePanes() {
        if (dialog != null) dialog.populatePanes(getExistingLabels(), getNewTopLabels(), bottomLabels, groups);
    }

    public void toggleLabel(String name) {
        addRemovePossibleLabel("");
        preProcessAndUpdateTopLabels(name);
        updateBottomLabels(""); // clears search query, removes faded-out overlay on bottom labels
        populatePanes();
    }

    public void toggleSelectedLabel(String text) {
        if (!bottomLabels.isEmpty() && !text.isEmpty() && hasHighlightedLabel()) {
            toggleLabel(
                    bottomLabels.stream().filter(PickerLabel::isHighlighted).findFirst().get().getActualName());
        }
    }

    public void processTextFieldChange(String text) {
        String[] textArray = text.split(" ");
        if (textArray.length > 0) {
            String query = textArray[textArray.length - 1];
            if (previousNumberOfActions != textArray.length || !query.equals(lastAction)) {
                processUserInput(textArray, query);
            }
        }
    }

    private void processUserInput(String[] textArray, String query) {
        previousNumberOfActions = textArray.length;
        lastAction = query;

        boolean appliedToGroup = applyToGroup(query);
        if (!appliedToGroup) {
            updateBottomLabels(query);
        }

        if (hasHighlightedLabel()) {
            addRemovePossibleLabel(getHighlightedLabelName().get().getActualName());
        } else {
            addRemovePossibleLabel("");
        }

        populatePanes();
    }

    private boolean applyToGroup(String query) {
        if (TurboLabel.getDelimiter(query).isPresent()) {
            String delimiter = TurboLabel.getDelimiter(query).get();
            String[] queryArray = query.split(Pattern.quote(delimiter));
            if (queryArray.length == 1) {
                updateBottomLabels(queryArray[0], "");
                return true;
            } else if (queryArray.length == 2) {
                updateBottomLabels(queryArray[0], queryArray[1]);
                return true;
            }
        }
        return false;
    }

    /*
    * Top pane methods do not need to worry about capitalisation because they
    * all deal with actual labels.
    */
    @SuppressWarnings("unused")
    private void ______TOP_PANE______() {}

    private List<PickerLabel> determineExistingLabels(TurboIssue issue, List<TurboLabel> allLabels) {
        // used once to populate topLabels at the start
        return allLabels.stream()
                .filter(label -> issue.getLabels().contains(label.getActualName()))
                .map(label -> new PickerLabel(label, this, true))
                .collect(Collectors.toList());
    }

    private void preProcessAndUpdateTopLabels(String name) {
        Optional<TurboLabel> turboLabel =
                allLabels.stream().filter(label -> label.getActualName().equals(name)).findFirst();
        if (turboLabel.isPresent()) {
            if (turboLabel.get().isExclusive() && !resultList.get(name)) {
                // exclusive label check
                String group = turboLabel.get().getGroup().get();
                allLabels
                        .stream()
                        .filter(TurboLabel::isExclusive)
                        .filter(label -> label.getGroup().get().equals(group))
                        .forEach(label -> removeFromTopLabels(label.getActualName()));
                addToTopLabels(name);
            } else {
                if (resultList.get(name)) {
                    removeFromTopLabels(name);
                } else {
                    addToTopLabels(name);
                }
            }
        }
    }

    private void addToTopLabels(String name) {
        // adds new labels to the end of the list
        resultList.put(name, true); // update resultList first
        if (issue.getLabels().contains(name)) {
            topLabels.stream()
                    .filter(label -> label.getActualName().equals(name))
                    .forEach(label -> {
                        label.setIsRemoved(false);
                        label.setIsFaded(false);
                    });
        } else {
            allLabels.stream()
                    .filter(label -> label.getActualName().equals(name))
                    .filter(label -> resultList.get(label.getActualName()))
                    .filter(label -> !isInTopLabels(label.getActualName()))
                    .findFirst()
                    .ifPresent(label -> topLabels.add(new PickerLabel(label, this, true)));
        }
    }

    private void removeFromTopLabels(String name) {
        resultList.put(name, false);
        topLabels.stream()
                .filter(label -> label.getActualName().equals(name))
                .findFirst()
                .ifPresent(label -> {
                    if (issue.getLabels().contains(name)) {
                        label.setIsRemoved(true);
                    } else {
                        topLabels.remove(label);
                    }
                });
    }

    private boolean isInTopLabels(String name) {
        // used to prevent duplicates in topLabels
        return topLabels.stream()
                .filter(label -> label.getActualName().equals(name))
                .findAny()
                .isPresent();
    }

    private void addRemovePossibleLabel(String name) {
        removePreviousSelection();

        if (!name.isEmpty()) {
            // Try to add current selection
            if (isInTopLabels(name)) {
                // if it exists in the top pane
                topLabels.stream()
                        .filter(label -> label.getActualName().equals(name))
                        .findFirst()
                        .ifPresent(label -> {
                            label.setIsHighlighted(true);
                            if (issue.getLabels().contains(name)) {
                                // if it is an existing label toggle fade and strike through
                                label.setIsFaded(resultList.get(name));
                                label.setIsRemoved(resultList.get(name));
                            } else {
                                // else set fade and strike through
                                // if space is pressed afterwards, label is removed from topLabels altogether
                                label.setIsFaded(true);
                                label.setIsRemoved(true);
                            }
                        });
            } else {
                // add it to the top pane
                allLabels.stream()
                        .filter(label -> label.getActualName().equals(name))
                        .findFirst()
                        .ifPresent(label -> topLabels.add(
                                new PickerLabel(label, this, false, true, false, true, true)));
            }
            targetLabel = Optional.of(name);
        }
    }

    private void removePreviousSelection() {
        if (targetLabel.isPresent()) {
            // if there's a previous possible selection, delete it
            // targetLabel can be
            topLabels.stream()
                    .filter(label -> label.getActualName().equals(targetLabel.get()))
                    .findFirst()
                    .ifPresent(label -> {
                        if (issue.getLabels().contains(targetLabel.get()) || resultList.get(targetLabel.get())) {
                            // if it is an existing label toggle fade and strike through
                            label.setIsHighlighted(false);
                            label.setIsFaded(false);
                            if (resultList.get(label.getActualName())) {
                                label.setIsRemoved(false);
                            } else {
                                label.setIsRemoved(true);
                            }
                        } else {
                            // if not then remove it
                            topLabels.remove(label);
                        }
                    });
            targetLabel = Optional.empty();
        }
    }

    // Bottom box deals with possible matches so we usually ignore the case for these methods.
    @SuppressWarnings("unused")
    private void ______BOTTOM_BOX______() {}

    private void updateBottomLabels(String group, String match) {
        List<String> groupNames = groups.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
        boolean isValidGroup = groupNames.stream()
                .filter(validGroup -> Utility.startsWithIgnoreCase(validGroup, group))
                .findAny()
                .isPresent();

        if (isValidGroup) {
            List<String> validGroups = groupNames.stream()
                    .filter(validGroup -> Utility.startsWithIgnoreCase(validGroup, group))
                    .collect(Collectors.toList());
            // get all labels that contain search query
            // fade out labels which do not match
            bottomLabels = allLabels
                    .stream()
                    .map(label -> new PickerLabel(label, this, false))
                    .map(label -> {
                        if (resultList.get(label.getActualName())) {
                            label.setIsSelected(true); // add tick if selected
                        }
                        if (!label.getGroup().isPresent() ||
                                !validGroups.contains(label.getGroup().get()) ||
                                !Utility.containsIgnoreCase(label.getName(), match)) {
                            label.setIsFaded(true); // fade out if does not match search query
                        }
                        return label;
                    })
                    .collect(Collectors.toList());
            if (!bottomLabels.isEmpty()) highlightFirstMatchingItem(match);
        } else {
            updateBottomLabels(match);
        }
    }

    private void updateBottomLabels(String match) {
        // get all labels that contain search query
        // fade out labels which do not match
        bottomLabels = allLabels
                .stream()
                .map(label -> new PickerLabel(label, this, false))
                .map(label -> {
                    if (resultList.get(label.getActualName())) {
                        label.setIsSelected(true); // add tick if selected
                    }
                    if (!match.isEmpty() && !Utility.containsIgnoreCase(label.getActualName(), match)) {
                        label.setIsFaded(true); // fade out if does not match search query
                    }
                    return label;
                })
                .collect(Collectors.toList());

        if (!match.isEmpty() && !bottomLabels.isEmpty()) highlightFirstMatchingItem(match);
    }

    public void moveHighlightOnLabel(boolean isDown) {
        if (hasHighlightedLabel()) {
            // used to move the highlight on the bottom labels
            // find all matching labels
            List<PickerLabel> matchingLabels = bottomLabels.stream()
                    .filter(label -> !label.isFaded())
                    .collect(Collectors.toList());

            // move highlight around
            for (int i = 0; i < matchingLabels.size(); i++) {
                if (matchingLabels.get(i).isHighlighted()) {
                    if (isDown && i < matchingLabels.size() - 1) {
                        matchingLabels.get(i).setIsHighlighted(false);
                        matchingLabels.get(i + 1).setIsHighlighted(true);
                        addRemovePossibleLabel(matchingLabels.get(i + 1).getActualName());
                    } else if (!isDown && i > 0) {
                        matchingLabels.get(i - 1).setIsHighlighted(true);
                        matchingLabels.get(i).setIsHighlighted(false);
                        addRemovePossibleLabel(matchingLabels.get(i - 1).getActualName());
                    }
                    populatePanes();
                    return;
                }
            }
        }
    }

    private void highlightFirstMatchingItem(String match) {
        List<PickerLabel> matches = bottomLabels.stream()
                .filter(label -> !label.isFaded())
                .collect(Collectors.toList());

        // try to highlight labels that begin with match first
        Optional<PickerLabel> labelStartingWithMatch =  matches.stream()
                .filter(label -> Utility.startsWithIgnoreCase(label.getName(), match))
                .findFirst();

        if (labelStartingWithMatch.isPresent()) {
            labelStartingWithMatch.get().setIsHighlighted(true);
        } else {
            // if not then highlight first matching label
            matches.stream()
                    .findFirst()
                    .ifPresent(label -> label.setIsHighlighted(true));
        }
    }

    public boolean hasHighlightedLabel() {
        return bottomLabels.stream()
                .filter(PickerLabel::isHighlighted)
                .findAny()
                .isPresent();
    }

    private Optional<PickerLabel> getHighlightedLabelName() {
        return bottomLabels.stream()
                .filter(PickerLabel::isHighlighted)
                .findAny();
    }

    @SuppressWarnings("unused")
    private void ______BOILERPLATE______() {}

    public Map<String, Boolean> getResultList() {
        return resultList;
    }

    private List<PickerLabel> getExistingLabels() {
        return topLabels.stream()
                .filter(label -> issue.getLabels().contains(label.getActualName()))
                .collect(Collectors.toList());
    }

    private List<PickerLabel> getNewTopLabels() {
        return topLabels.stream()
                .filter(label -> !issue.getLabels().contains(label.getActualName()))
                .collect(Collectors.toList());
    }

}
