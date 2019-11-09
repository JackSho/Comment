package Form;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.NotNull;

public class CommentAction extends EditorAction {

    protected static String COMMENT = "#_";

    public CommentAction() {
        super(new CommentAction.Handler());
    }

    protected CommentAction(EditorActionHandler defaultHandler) {
        super(defaultHandler);
    }

    public static class Handler extends EditorWriteActionHandler {
        public void executeWriteAction(final Editor editor, Caret caret, DataContext context) {
            if (!editor.getSelectionModel().hasSelection(true)) {
                if (Registry.is("editor.skip.copy.and.cut.for.empty.selection")) {
                    return;
                }

                //若没有选中内容，则默认选中光标停留的那一行
                editor.getCaretModel().runForEachCaret(new CaretAction() {
                    public void perform(@NotNull Caret caret) {
                        editor.getSelectionModel().selectLineAtCaret();
                    }
                });
            }
            SelectionModel sm = editor.getSelectionModel();
            String selectedText = sm.getSelectedText();
            assert selectedText != null;
            String newText;
            if (selectedText.contains(COMMENT)) {
                newText = selectedText.replaceFirst(COMMENT, "");
            } else {
                VisualPosition selectionStartPosition = sm.getSelectionStartPosition();
                assert selectionStartPosition != null;
                if (selectionStartPosition.getColumn() >= 2) {
                    int selectStart = sm.getSelectionStart();
                    if (editor.getDocument().getText(new TextRange(selectStart - 2, selectStart)).equals(COMMENT)) {
                        EditorActionUtil.moveCaret(editor.getCaretModel().getPrimaryCaret(), selectStart - 2, true);
                    }
                }

                selectedText = sm.getSelectedText();
                String trimText = selectedText.trim();
                int index = selectedText.indexOf(trimText);
                newText = selectedText.substring(0, index);
                if (trimText.startsWith(COMMENT))
                    newText += selectedText.substring(index + 2);
                else
                    newText += COMMENT + selectedText.substring(index);
            }
            EditorModificationUtil.insertStringAtCaret(editor, newText, true, true);
        }
    }
}
