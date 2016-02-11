package ui.components.issue_creators;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.parboiled.matchervisitors.GetStarterCharVisitor;
import org.pegdown.PegDownProcessor;

import backend.resource.TurboIssue;
import backend.resource.TurboUser;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

/**
 * Models GitHub comment box with toggleable Markdown preview
 */
public class IssueContentPane extends StackPane {

    public static final String HTML_TEMPLATE =
            "<!DOCTYPE html><html><head></head><body>%s</body></html>";
    public static final String REFERENCE_ISSUE = "#%d %s";
    public static final int MAX_SUGGESTION_ENTRIES = 5;
    // Magic number that accounts for width of each character in textarea
    public static final int CARET_OFFSET = 8;
    // Vertical offset for popup
    public static final int VERTICAL_OFFSET = 20;

    public static final KeyCodeCombination PREVIEW = new KeyCodeCombination(KeyCode.P,
            KeyCodeCombination.ALT_DOWN);
    public static final KeyCodeCombination MENTION = new KeyCodeCombination(KeyCode.AT);
    public static final KeyCodeCombination REFERENCE = new KeyCodeCombination(KeyCode.POUND);
    public static final KeyCodeCombination HIDE_SUGGGESTIONS =
            new KeyCodeCombination(KeyCode.SPACE);

    private static final int COL_PREF_COUNT = 30;

    private final PegDownProcessor markdownProcessor;
    private final TextArea body;
    private final WebView preview;

    private final SuggestionMenu suggestions;
    private final IssueCreatorPresenter presenter;

    public IssueContentPane(String content, IssueCreatorPresenter presenter) {
        this.presenter = presenter;
        markdownProcessor = initMarkdownProcessor();
        suggestions = new SuggestionMenu(MAX_SUGGESTION_ENTRIES);

        // Order of these methods are important
        body = initBody();
        preview = initWebView();

        body.setText(content);
        generatePreview(content);
        setupHandlers();
    }

    /**
     * Returns content in text area
     */
    public String getContent() {
        return body.getText();
    }

    private void generatePreview(String content) {
        String bodyContent = markdownProcessor.markdownToHtml(content);
        String html = String.format(HTML_TEMPLATE, bodyContent);
        preview.getEngine().loadContent(html, "text/html");
    }

    private void setupHandlers() {
        setOnMouseClicked(this::mouseClickHandler);
        setOnKeyPressed(this::keyPressHandler);
        body.setOnKeyPressed(this::bodyKeyPressHandler);
    }


    // ==============
    // Event handlers
    // ==============

    private void mouseClickHandler(MouseEvent e) {
        body.toFront();
    }

    private void keyPressHandler(KeyEvent e) {
        if (PREVIEW.match(e)) {
            generatePreview(body.getText());
            togglePane();
        }
        e.consume();
    }

    /**
     * Triggers context menu for every keyword
     */
    private void bodyKeyPressHandler(KeyEvent e) {
        Point2D pos = getPopupPosition(body.getCaretPosition());
        suggestions.show(body, pos.getX(), pos.getY());
    }

    // ===========
    // Suggestions
    // ===========

    /**
     * Converts caret position from local offset to screen coordinate
     * 
     * @param caretPosition
     * @return Point2D screen coordinates
     */
    private Point2D getPopupPosition(int caretPosition) {
        return body.localToScreen(caretPosition * CARET_OFFSET,
                Math.ceil(caretPosition / COL_PREF_COUNT) * VERTICAL_OFFSET);
    }

    private void showSuggestions(List<String> searchResult) {
        suggestions.loadSuggestions(searchResult);
    }

    private List<String> getAllIssues(List<TurboIssue> issues) {
        return issues.stream().map(i -> String.format(REFERENCE_ISSUE, i.getId(), i.getTitle()))
                .collect(Collectors.toList());
    }

    private List<String> getAllUsers(List<TurboUser> users) {
        return users.stream().map(TurboUser::getRealName).collect(Collectors.toList());
    }

    // =================
    // UI initialization
    // =================

    /**
     * Initializes WebView and set index
     */
    private WebView initWebView() {
        WebView preview = new WebView();
        this.getChildren().add(1, preview);
        return preview;
    }

    /**
     * Initializes TextArea and set index
     */
    private TextArea initBody() {
        TextArea body = new TextArea();
        body.setPrefColumnCount(COL_PREF_COUNT);
        body.setWrapText(true);
        suggestions.loadSuggestions(Arrays.asList("test", "babi", "halo"));
        this.getChildren().add(0, body);
        return body;
    }

    private void togglePane() {
        preview.toFront();
    }

    /**
     * Setups Markdown processor to handle GitHub flavored markdown syntax
     */
    private PegDownProcessor initMarkdownProcessor() {
        return new PegDownProcessor();
    }

}