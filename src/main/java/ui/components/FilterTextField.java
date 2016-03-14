package ui.components;

import filter.ParseException;
import filter.Parser;
import filter.expression.QualifierType;
import javafx.geometry.Side;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A field augmented with the ability to revert edits, autocompletion, on-the-fly error
 * checking, and callbacks for various actions.
 */
public class FilterTextField extends TextField {

    private static final int MAX_SUGGESTIONS = 10;

    // A character class that indicates the characters considered to be word boundaries.
    // Specialised to HubTurbo's filter syntax.
    private static final String WORD_BOUNDARY_REGEX = "[ (:)]";

    // Background colours of FilterTextField.
    // The background colour set depends on whether the text in the textfield is a valid filter.
    private static final String VALID_FILTER_STYLE = "-fx-control-inner-background: white";
    private static final String INVALID_FILTER_STYLE = "-fx-control-inner-background: #EE8993";

    // For on-the-fly parsing and checking
    private final ValidationSupport validationSupport = new ValidationSupport();
    private final SuggestionMenu suggestion;

    // Callback functions
    private Runnable cancel = () -> {};
    private Function<String, String> confirm = (s) -> s;

    // The list of keywords which will be used in completion
    private List<String> keywords = new ArrayList<>(QualifierType.getCompletionKeywords());


    public FilterTextField(String initialText) {
        super(initialText);
        setup();
        suggestion = setupSuggestion();
    }

    private void setup() {
        setPrefColumnCount(30);
        validationSupport.registerValidator(this, (c, newValue) -> {
            boolean wasError = false;
            try {
                Parser.parse(getText());
                setStyleForValidFilter();
            } catch (ParseException e) {
                wasError = true;
                setStyleForInvalidFilter();
            }
            return ValidationResult.fromErrorIf(this, "Parse error", wasError);
        });
        setOnKeyTyped(e -> {
            boolean isModifierKeyPress = e.isAltDown() || e.isMetaDown() || e.isControlDown();
            String key = e.getCharacter();
            if (key == null || key.isEmpty() || isModifierKeyPress) {
                return;
            }
            char typed = e.getCharacter().charAt(0);

            if (typed == '\t') {
                if (!suggestion.isShowing() && shouldStartCompletion()) {
                    suggestion.show(this, Side.BOTTOM, 0, 0);
                } else {
                    suggestion.hide();
                }
                e.consume();
            }
            
        });
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.TAB) {
                 // Disable tab for UI traversal
                 e.consume();
            }
        });
        setOnKeyReleased(e -> {
            e.consume();

            if (suggestion.isShowing() && !isNavigationKey(e)) {
                suggestion.loadSuggestions(getMatchingKeywords(getCurrentWord()));
            }

            if (e.getCode() == KeyCode.ENTER) {
                confirmEdit();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                cancel.run();
            }
        });
    }


    /**
     * Sets the style of FilterTextField to the style for valid filter
     */
    public final void setStyleForValidFilter() {
        this.setStyle(VALID_FILTER_STYLE);
    }

    /**
     * Sets the style of FilterTextField to the style for invalid filter
     */
    public final void setStyleForInvalidFilter() {
        this.setStyle(INVALID_FILTER_STYLE);
    }

    private boolean isNavigationKey(KeyEvent e) {
        return e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN;
    }

    /**
     * Completion is only started when there's effectively nothing (only whitespace)
     * after the caret, or if there is selected text (indicating either that we are in
     * the midst of completion, or that the user does not want the selected text and is
     * typing to replace it)
     * @return true if completion should be started
     */
    private boolean shouldStartCompletion() {
        return getCharAfterCaret().trim().isEmpty() || !getSelectedText().isEmpty();
    }

    /**
     * @param query
     * @return suggested keyword that contains a given query
     */
    private List<String> getMatchingKeywords(String query) {
        return keywords.stream().filter(keyword -> keyword.contains(query)).collect(Collectors.toList());
    }


    /**
     * Determines the word currently being edited.
     */
    private String getCurrentWord() {
        int caret = Math.min(getSelection().getStart(), getSelection().getEnd());
        return getText().substring(getInitialCaretPosition(caret), caret);
    }

    /**
     * @param caret
     * @return index before first char in current word 
     */
    private int getInitialCaretPosition(int caret) {
        int pos = regexLastIndexOf(getText().substring(0, caret), WORD_BOUNDARY_REGEX);
        if (pos == -1) {
            pos = 0;
        }
        return pos > 0 ? pos + 1 : pos;
    }

    /**
     * Given a string and a regex, determines the last occurrence of a substring matching
     * that regex in the string.
     * Caveat: algorithm only works for character-class regexes
     */
    private static int regexLastIndexOf(String inString, String charClassRegex) {
        String reversed = new StringBuilder(inString).reverse().toString();
        Pattern pattern = Pattern.compile(charClassRegex);
        Matcher m = pattern.matcher(reversed);
        if (m.find()) {
            return reversed.length() - (m.start() + 1);
        } else {
            return -1;
        }
    }

    /**
     * Returns the character following the caret as a string. The string returned
     * is guaranteed to be of length 1, unless the caret is at the end of the field,
     * in which case it is empty.
     */
    private String getCharAfterCaret() {
        if (getCaretPosition() < getText().length()) {
            return getText().substring(getCaretPosition(), getCaretPosition() + 1);
        }
        return "";
    }


    /**
     * Commits the current contents of the field. This triggers its 'confirm' callback.
     */
    private void confirmEdit() {
        String newText = confirm.apply(getText());
        int caretPosition = getCaretPosition();
        setText(newText);
        positionCaret(caretPosition);
    }

    // SuggestionMenu 
    
    private final SuggestionMenu setupSuggestion() {
        SuggestionMenu suggestion = new SuggestionMenu(MAX_SUGGESTIONS);
        suggestion.loadSuggestions(keywords);
        suggestion.setOnHidden(e -> completeWord());
        return suggestion;
    }

    private void completeWord() {
        int caret = getCaretPosition();
        selectRange(getInitialCaretPosition(caret), caret);
        suggestion.getSelectedContent().ifPresent(this::replaceSelection);
    }

    /**
     * Sets the contents of the field and acts as if it was confirmed by the user.
     */
    public void setFilterText(String text) {
        setText(text);
        confirmEdit();
    }

    /**
     * Sets the 'cancel' callback, which will be called with Esc is pressed and there is
     * no edit to revert.
     */
    public FilterTextField setOnCancel(Runnable cancel) {
        this.cancel = cancel;
        return this;
    }

    /**
     * Sets the 'confirm' callback, which will be called when Enter is pressed, or when the
     * contents of the field are manually set with {@link #setFilterText}. The callback will
     * be passed the current contents of the field.
     */
    public FilterTextField setOnConfirm(Function<String, String> confirm) {
        this.confirm = confirm;
        return this;
    }

    /**
     * Sets the list of words to be used as completion candidates.
     */
    public void setCompletionKeywords(List<String> words) {
        keywords = new ArrayList<>(words);
    }
}
