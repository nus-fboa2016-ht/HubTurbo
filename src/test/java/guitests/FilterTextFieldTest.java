package guitests;

import org.junit.Test;

import javafx.scene.input.KeyCode;
import ui.TestController;
import ui.components.FilterTextField;
import ui.issuepanel.FilterPanel;

public class FilterTextFieldTest extends UITest {

    private void testCompletions(FilterTextField field) {
        // Basic completion
        clearField();
        push(KeyCode.TAB);
        type("cou").push(KeyCode.TAB);
        awaitCondition(() -> field.getText().equals("count"));

        // Completion with selection
        clearField();
        type("count").push(KeyCode.TAB);
        push(KeyCode.LEFT);
        for (int i = 0; i < 3; i++) {
            field.selectBackward();
        }
        // c[oun]t
        type("lo").push(KeyCode.TAB); // 'c' + 'lo' is a prefix of 'closed'
        awaitCondition(() -> field.getText().equals("closedt"));
    }

    @Test
    public void completeWord_confirmedWithTab() {
        FilterTextField field = getFirstPanelField();
        testCompletions(field);
    }

    @Test
    public void completeWord_confirmedWithEnter() {
        FilterTextField field = getFirstPanelField();

        clearField();
        push(KeyCode.TAB);
        type("i");
        push(KeyCode.DOWN, 3);
        push(KeyCode.ENTER);
        awaitCondition(() -> field.getText().equals("id"));
    }

    private FilterTextField getFirstPanelField() {
        FilterPanel issuePanel = (FilterPanel) TestController.getUI().getPanelControl().getPanel(0);
        FilterTextField field = issuePanel.getFilterTextField();
        waitUntilNodeAppears(field);
        click(issuePanel.getFilterTextField());
        return field;
    }

    private void clearField() {
        selectAll();
        push(KeyCode.BACK_SPACE);
    }
}
