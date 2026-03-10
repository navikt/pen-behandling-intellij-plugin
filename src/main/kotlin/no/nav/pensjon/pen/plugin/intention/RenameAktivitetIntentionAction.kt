package no.nav.pensjon.pen.plugin.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

/**
 * Intention action (Alt+Enter) that renames an Aktivitet following PEN conventions.
 *
 * Updates:
 * - File name (A###_OldDesc.kt → A###_NewDesc.kt)
 * - Class names ({Behandling}A###OldDesc → {Behandling}A###NewDesc)
 * - Discriminator value ("{Behandling}_OldDesc" → "{Behandling}_NewDesc")
 * - All references in sibling .kt files in the same directory
 */
class RenameAktivitetIntentionAction : PsiElementBaseIntentionAction() {

    override fun getFamilyName() = "PEN Behandling"

    override fun getText() = "Rename Aktivitet (PEN conventions)"

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val fileName = element.containingFile?.name ?: return false
        return fileName.matches(Regex("A\\d{3}_.*\\.kt"))
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val currentFile = element.containingFile ?: return
        val directory = currentFile.containingDirectory ?: return

        val match = Regex("^A(\\d{3})_(.+)\\.kt$").find(currentFile.name) ?: return
        val numStr = "A${match.groupValues[1]}"
        val oldDesc = match.groupValues[2]
        val behandlingName = guessBehandlingName(directory)

        val newDesc = Messages.showInputDialog(
            project,
            "New description (PascalCase, e.g. 'OpprettOppgave'):",
            "Rename Aktivitet $numStr",
            null,
            oldDesc,
            object : InputValidator {
                override fun checkInput(input: String) =
                    input.isNotBlank() && input[0].isUpperCase() && !input.contains(' ')

                override fun canClose(input: String) = checkInput(input)
            }
        ) ?: return

        if (newDesc == oldDesc) return

        val oldClassPrefix = "${behandlingName}${numStr}${oldDesc}"
        val newClassPrefix = "${behandlingName}${numStr}${newDesc}"
        val oldDiscriminator = "${behandlingName}_${oldDesc}"
        val newDiscriminator = "${behandlingName}_${newDesc}"

        WriteCommandAction.runWriteCommandAction(project, "Rename Aktivitet", null, {
            val psiDocManager = PsiDocumentManager.getInstance(project)

            for (sibling in directory.files) {
                if (!sibling.name.endsWith(".kt")) continue
                val doc = psiDocManager.getDocument(sibling) ?: continue
                var content = doc.text
                var changed = false

                if (content.contains(oldClassPrefix)) {
                    content = content.replace(oldClassPrefix, newClassPrefix)
                    changed = true
                }
                if (content.contains(oldDiscriminator)) {
                    content = content.replace(oldDiscriminator, newDiscriminator)
                    changed = true
                }
                if (changed) {
                    doc.setText(content)
                    psiDocManager.commitDocument(doc)
                }
            }

            currentFile.virtualFile.rename(this, "${numStr}_${newDesc}.kt")
        })
    }

    private fun guessBehandlingName(directory: PsiDirectory): String {
        val behandlingFile = directory.files.firstOrNull { it.name.endsWith("Behandling.kt") }
        return behandlingFile?.name?.removeSuffix("Behandling.kt") ?: ""
    }
}
