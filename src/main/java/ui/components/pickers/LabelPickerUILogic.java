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
    private List<TurboLabel> allLabels;
    private final List<PickerLabel> topLabels;
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
        allLabels = initAllLabels(repoLabels);
        groups = initGroups(repoLabels);
        resultList = populateAllLabels(repoLabels, issue);
        topLabels = addExistingLabels();
        updateBottomLabels("");
        populatePanes();
    }

    public LabelPickerUILogic(TurboIssue issue, List<TurboLabel> repoLabels) {
        this.issue = issue;
        this.dialog = null;
        allLabels = initAllLabels(repoLabels);
        groups = initGroups(repoLabels);
        resultList = populateAllLabels(repoLabels, issue);
        topLabels = addExistingLabels();
        updateBottomLabels("");
        populatePanes();
    }

    private List<TurboLabel> initAllLabels(List<TurboLabel> repoLabels) {
        ArrayList<TurboLabel> allLabels = new ArrayList<>(repoLabels);
        Collections.sort(allLabels);
        return allLabels;
    }

    private boolean isGroupNotAdded(TurboLabel label, Map<String, Boolean> groups) {
        return label.getGroup().isPresent() && !groups.containsKey(label.getGroup().get());
    }

    // TODO convert stream to Map
    private Map<String, Boolean> initGroups(List<TurboLabel> repoLabels) {
        Map<String, Boolean> groups = new HashMap<>();
        repoLabels.forEach(label -> {
            if (isGroupNotAdded(label, groups)) {
                groups.put(label.getGroup().get(), label.isExclusive());
            }
        });
        return groups;
    }

    // TODO convert stream to Map
    private Map<String, Boolean> populateAllLabels(List<TurboLabel> repoLabels, 
        TurboIssue issue) {
        // populate resultList by going through repoLabels and seeing which ones currently exist
        // in issue.getLabels()
        Map<String, Boolean> resultList = new HashMap<>();
        repoLabels.forEach(label -> {
            // matching with exact labels so no need to worry about capitalisation
            resultList.put(label.getActualName(), issue.getLabels().contains(label.getActualName()));
        });
        return resultList;
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

   
    private boolean hasChangedQuery(int previousNumberOfActions, 
                                    String[] textArray, String query) {
        return previousNumberOfActions != textArray.length || !query.equals(lastAction);
    }

    private boolean isBottomLabelsUpdated(String query) {
        boolean isBottomLabelsUpdated = false;
        // group check
        // TODO rewrite this to remove some nesting, and the PMD warning
        if (TurboLabel.getDelimiter(query).isPresent()) {
            String delimiter = TurboLabel.getDelimiter(query).get();
            String[] queryArray = query.split(Pattern.quote(delimiter));
            if (queryArray.length == 1) {
                isBottomLabelsUpdated = true;
                updateBottomLabels(queryArray[0], "");
            } else if (queryArray.length == 2) {
                isBottomLabelsUpdated = true;
                updateBottomLabels(queryArray[0], queryArray[1]);
            }
        }
        return isBottomLabelsUpdated;
    }

    @SuppressWarnings("PMD")
    public void processTextFieldChange(String text) {
        String[] textArray = text.split(" ");
        if (textArray.length > 0) {
            String query = textArray[textArray.length - 1];
            if (hasChangedQuery(previousNumberOfActions, textArray, query)) {
                previousNumberOfActions = textArray.length;
                lastAction = query;
                boolean isBottomLabelsUpdated = isBottomLabelsUpdated(query);

                if (!isBottomLabelsUpdated) {
                    updateBottomLabels(query);
                }

                if (hasHighlightedLabel()) {
                    addRemovePossibleLabel(getHighlightedLabelName().get().getActualName());
                } else {
                    addRemovePossibleLabel("");
                }
                populatePanes();
            }
        }
    }


    /*
    * Top pane methods do not need to worry about capitalisation because they
    * all deal with actual labels.
    */
    @SuppressWarnings("unused")
    private void ______TOP_PANE______() {}

    private List<PickerLabel> addExistingLabels() {
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
                        .forEach(label -> updateTopLabels(label.getActualName(), 
                                false, resultList));
                updateTopLabels(name, true, resultList);
            } else {
                updateTopLabels(name, !resultList.get(name), resultList);
            }
        }
    }

    private void removeMatchingLabels(String name, List<PickerLabel> topLabels) {
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

    // Re-enable a label if it is removed
    private void resetRemovedLabel(String name, List<PickerLabel> topLabels) {
        topLabels.stream()
                .filter(label -> label.getActualName().equals(name))
                .forEach(label -> {
                    label.setIsRemoved(false);
                    label.setIsFaded(false);
                });
    }

    // Add to topLabels if not exists
    private void addNewLabels(String name, Map<String, Boolean> resultList) {
        allLabels.stream()
                .filter(label -> label.getActualName().equals(name))
                .filter(label -> resultList.get(label.getActualName()))
                .filter(label -> !isInTopLabels(label.getActualName()))
                .findFirst()
                .ifPresent(label -> topLabels.add(new PickerLabel(label, this, true)));
    }

    private void updateTopLabels(String name, boolean isAdd, Map<String, Boolean> resultList) {
        // adds new labels to the end of the list
        resultList.put(name, isAdd); // update resultList first
        if (isAdd) {
            if (issue.getLabels().contains(name)) {
                resetRemovedLabel(name, topLabels);
            } else {
                addNewLabels(name, resultList);
            }
        } else {
            removeMatchingLabels(name, topLabels);
        }
    }



    private boolean isInTopLabels(String name) {
        // used to prevent duplicates in topLabels
        return topLabels.stream()
                .filter(label -> label.getActualName().equals(name))
                .findAny()
                .isPresent();
    }

    // Performs action on topLabels based on targeted label
    private void processTargetLabel(List<PickerLabel> topLabels) {
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
    }

    private void addToTopLabel(String name, List<PickerLabel> topLabels) {
        allLabels.stream()
                .filter(label -> label.getActualName().equals(name))
                .findFirst()
                .ifPresent(label -> topLabels.add(
                        new PickerLabel(label, this, false, true, false, true, true)));
    }

    // TODO remove access to resultList
    private void setTargetLabelState(String name, TurboIssue issue, PickerLabel label) {
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
    }

    private void setFirstMatchingTopLabel(String name) {
        topLabels.stream()
                .filter(label -> label.getActualName().equals(name))
                .findFirst()
                .ifPresent(label -> {
                    setTargetLabelState(name, issue, label);
                });
    }

    private void addRemovePossibleLabel(String name) {
        // Deletes previous selection
        if (targetLabel.isPresent()) {
            // if there's a previous possible selection, delete it
            // targetLabel can be
            processTargetLabel(topLabels);
            targetLabel = Optional.empty();
        }

        if (!name.isEmpty()) {
            // Try to add current selection
            if (isInTopLabels(name)) {
                // if it exists in the top pane
                setFirstMatchingTopLabel(name);
            } else {
                // add it to the top pane
                addToTopLabel(name, topLabels);
            }
            targetLabel = Optional.of(name);
        }
    }




    // Bottom box deals with possible matches so we usually ignore the case for these methods.
    @SuppressWarnings("unused")
    private void ______BOTTOM_BOX______() {}

    private boolean isValidGroup(String group, List<String> groupNames) {
        return groupNames.stream()
                .filter(validGroup -> Utility.startsWithIgnoreCase(validGroup, group))
                .findAny()
                .isPresent();
    }

    private List<String> getValidGroupNames(String group, List<String> groupNames) {
        List<String> validGroups = groupNames.stream()
                .filter(validGroup -> Utility.startsWithIgnoreCase(validGroup, group))
                .collect(Collectors.toList());
        return validGroups;
    }

    private void updateSelectedLabel(PickerLabel label) {
        if (resultList.get(label.getActualName())) {
            label.setIsSelected(true); // add tick if selected
        }
    }

    private List<PickerLabel> getBottomLabels(String match, List<String> validGroups) {
        return allLabels.stream()
                .map(label -> new PickerLabel(label, this, false))
                .map(label -> {
                    updateSelectedLabel(label);
                    if (!label.getGroup().isPresent() ||
                            !validGroups.contains(label.getGroup().get()) ||
                            !Utility.containsIgnoreCase(label.getName(), match)) {
                        label.setIsFaded(true); // fade out if does not match search query
                    }
                    return label;
                })
                .collect(Collectors.toList());
    }

    private List<PickerLabel> getBottomLabels(String match) {
        return allLabels.stream()
                .map(label -> new PickerLabel(label, this, false))
                .map(label -> {
                    updateSelectedLabel(label);
                    if (!match.isEmpty() && !Utility.containsIgnoreCase(label.getActualName(), match)) {
                        label.setIsFaded(true); // fade out if does not match search query
                    }
                    return label;
                })
                .collect(Collectors.toList());
    }

    private void updateBottomLabels(String group, String match) {
        List<String> groupNames = groups.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());

        if (isValidGroup(group, groupNames)) {
            List<String> validGroups = getValidGroupNames(group, groupNames);
            // get all labels that contain search query
            // fade out labels which do not match
            this.bottomLabels = getBottomLabels(match, validGroups);
            if (!bottomLabels.isEmpty()) highlightFirstMatchingItem(match);
        } else {
            updateBottomLabels(match);
        }
    }

    private void updateBottomLabels(String match) {
        // get all labels that contain search query
        // fade out labels which do not match
        this.bottomLabels = getBottomLabels(match);
        if (!match.isEmpty() && !bottomLabels.isEmpty()) highlightFirstMatchingItem(match);
    }


    private List<PickerLabel> findMatchingLabels() {
        List<PickerLabel> matchingLabels = bottomLabels.stream()
                .filter(label -> !label.isFaded())
                .collect(Collectors.toList());
        return matchingLabels;
    }

    // TODO see if possibe to avoid for loop
    public void moveHighlightOnLabel(boolean isDown) {
        if (hasHighlightedLabel()) {
            // used to move the highlight on the bottom labels
            // find all matching labels
            List<PickerLabel> matchingLabels = findMatchingLabels();

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
        List<PickerLabel> matches = findMatchingLabels();

        // try to highlight labels that begin with match first
        matches.stream()
                .filter(label -> Utility.startsWithIgnoreCase(label.getName(), match))
                .findFirst()
                .ifPresent(label -> label.setIsHighlighted(true));

        // if not then highlight first matching label
        if (!hasHighlightedLabel()) {
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
