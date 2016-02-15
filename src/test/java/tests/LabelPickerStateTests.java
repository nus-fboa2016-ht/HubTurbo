package tests;

import org.junit.Test;
import ui.components.pickers.LabelPickerState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LabelPickerStateTests {

    public LabelPickerState setupState(String... labelNames) {
        return new LabelPickerState(getHashSet(labelNames));
    }

    public Set<String> getTestRepoLabels() {
        Set<String> setOfLabelNames;
        setOfLabelNames = getHashSet("priority.high", "priority.medium", "priority.low", "highest", "Problem.Heavy",
                "f-aaa", "f-bbb");

        return setOfLabelNames;
    }

    public Set<String> getHashSet(String... labelNames) {
        Set<String> setOfLabelNames = new HashSet<>();
        for (String labelName : labelNames) {
            setOfLabelNames.add(labelName);
        }

        return setOfLabelNames;
    }

    @Test
    public void matchedLabelsTest() {
        LabelPickerState initialState = setupState();
        LabelPickerState nextState = initialState.updateMatchedLabels(
                getTestRepoLabels(),
                "p.h"
        );

        assertEquals(2, nextState.getMatchedLabels().size());

        nextState = nextState.clearMatchedLabels();
        assertEquals(0, nextState.getMatchedLabels().size());
    }

    @Test
    public void currentSuggestionTest() {
        LabelPickerState initialState = setupState();
        LabelPickerState nextState = initialState.updateMatchedLabels(
                getTestRepoLabels(),
                "p.h"
        );

        assertEquals(true, nextState.getCurrentSuggestion().isPresent());
        assertEquals("priority.high", nextState.getCurrentSuggestion().get());

        nextState = nextState.nextSuggestion();
        assertEquals(true, nextState.getCurrentSuggestion().isPresent());
        assertEquals("Problem.Heavy", nextState.getCurrentSuggestion().get());

        nextState = nextState.previousSuggestion();
        assertEquals(true, nextState.getCurrentSuggestion().isPresent());
        assertEquals("priority.high", nextState.getCurrentSuggestion().get());
    }

    @Test
    public void toggleLabelTest() {
        Set<String> repoLabels = getTestRepoLabels();
        LabelPickerState initialState = setupState();
        LabelPickerState nextState = initialState.toggleLabel(repoLabels, "priority.medium");
        assertEquals(1, nextState.getAddedLabels().size());
        nextState = nextState.toggleLabel(repoLabels, "f-aaa");
        assertEquals(2, nextState.getAddedLabels().size());
    }

    @Test
    public void removeConflictingTest() {
        Set<String> repoLabels = getTestRepoLabels();
        LabelPickerState initialState = setupState("priority.low", "priority.high");
        assertEquals(2, initialState.getInitialLabels().size());
        LabelPickerState nextState = initialState.toggleLabel(repoLabels, "priority.medium");

        assertEquals("priority.medium", nextState.getAddedLabels().get(0));
        assertEquals(1, nextState.getAddedLabels().size());
        assertEquals(2, nextState.getRemovedLabels().size());
        assertTrue(nextState.getRemovedLabels().contains("priority.low"));
        assertTrue(nextState.getRemovedLabels().contains("priority.high"));
    }

    @Test
    public void getAssignedLabelsTest() {
        Set<String> repoLabels = getTestRepoLabels();
        LabelPickerState initialState = setupState("priority.low", "priority.high");
        LabelPickerState nextState = initialState.toggleLabel(repoLabels, "priority.medium");
        nextState = nextState.toggleLabel(repoLabels, "priority.medium");
        assertEquals(0, nextState.getAssignedLabels().size());

        nextState = nextState.toggleLabel(repoLabels, "priority.low");
        nextState = nextState.toggleLabel(repoLabels, "priority.low");
        nextState = nextState.toggleLabel(repoLabels, "priority.low");
        nextState = nextState.toggleLabel(repoLabels, "Problem.Heavy");
        nextState = nextState.toggleLabel(repoLabels, "priority.medium");
        assertEquals(2, nextState.getAssignedLabels().size());
        assertTrue(nextState.getAssignedLabels().contains("priority.medium"));
        assertTrue(nextState.getAssignedLabels().contains("Problem.Heavy"));
    }
}