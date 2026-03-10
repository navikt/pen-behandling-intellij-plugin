package no.nav.pensjon.pen.plugin.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

class AddRequestContextUserIdIntentionAction : PsiElementBaseIntentionAction() {

    override fun getFamilyName() = "PEN Behandling"

    override fun getText() = "Legg til getRequestContextUserId()"

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val text = element.containingFile?.text ?: return false
        return ParameterCodeModifier.isBehandlingFile(text)
                && !text.contains("getRequestContextUserId")
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = element.containingFile ?: return
        val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val text = doc.text

        val value = Messages.showInputDialog(
            project,
            "Verdi for getRequestContextUserId():",
            "Legg til getRequestContextUserId",
            null,
        ) ?: return

        if (value.isBlank()) return

        WriteCommandAction.runWriteCommandAction(project, "Legg til getRequestContextUserId", null, {
            val newText = addRequestContextUserId(text, value.trim())
            doc.setText(newText)
            PsiDocumentManager.getInstance(project).commitDocument(doc)
        })
    }

    companion object {
        fun addRequestContextUserId(text: String, value: String): String {
            // Insert before opprettInitiellAktivitet if it exists, otherwise before the last }
            val overrideLine = "    override fun getRequestContextUserId(): String = \"$value\""

            val initMethodIdx = text.indexOf("override fun opprettInitiellAktivitet()")
            if (initMethodIdx != -1) {
                // Insert before opprettInitiellAktivitet
                val lineStart = text.lastIndexOf('\n', initMethodIdx - 1)
                return text.substring(0, lineStart + 1) +
                        "\n$overrideLine\n" +
                        text.substring(lineStart + 1)
            }

            // Fallback: insert before last closing brace of the class
            val lastBrace = text.lastIndexOf('}')
            if (lastBrace == -1) return text

            return text.substring(0, lastBrace) +
                    "\n$overrideLine\n" +
                    text.substring(lastBrace)
        }
    }
}
