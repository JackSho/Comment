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

    protected static final String FORM_COMMENT_SIGN = "#_";
    protected static final String LINE_COMMENT_SIGN = ";";

    public CommentAction() {
        super(new CommentAction.Handler());
    }

    protected CommentAction(EditorActionHandler defaultHandler) {
        super(defaultHandler);
    }

    public static class Handler extends EditorWriteActionHandler {
        public String cancelLineComment(String input, String lineCommentSign) {
            if (!input.startsWith(lineCommentSign))
                return input;
            else
                return cancelLineComment(input.substring(lineCommentSign.length()), lineCommentSign);
        }

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
            if (selectedText.contains(FORM_COMMENT_SIGN)) {
                newText = selectedText.replaceFirst(FORM_COMMENT_SIGN, "");
            } else {
                VisualPosition selectionStartPosition = sm.getSelectionStartPosition();
                assert selectionStartPosition != null;
                if (selectionStartPosition.getColumn() >= 2) {
                    int selectStart = sm.getSelectionStart();
                    if (editor.getDocument().getText(new TextRange(selectStart - 2, selectStart)).equals(FORM_COMMENT_SIGN)) {
                        EditorActionUtil.moveCaret(editor.getCaretModel().getPrimaryCaret(), selectStart - 2, true);
                    }
                }

                selectedText = sm.getSelectedText();
                String trimText = selectedText.trim();
                int index = selectedText.indexOf(trimText);
                newText = selectedText.substring(0, index); // 当前行前面的空白字符串

                // 以行注释开头，取消行注释
                if (trimText.startsWith(LINE_COMMENT_SIGN)) {
                    String noLineComment = cancelLineComment(trimText, LINE_COMMENT_SIGN);
                    int withoutCommentIndex = selectedText.indexOf(noLineComment);
                    newText += selectedText.substring(withoutCommentIndex);
                } else if (trimText.startsWith(FORM_COMMENT_SIGN))  // 以块注释开头，取消块注释
                    newText += selectedText.substring(index + 2);
                else                                                // 不以注释开头，添加块注释
                    newText += FORM_COMMENT_SIGN + selectedText.substring(index);
            }
            EditorModificationUtil.insertStringAtCaret(editor, newText, true, true);
        }
    }
}
