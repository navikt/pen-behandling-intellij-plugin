package no.nav.pensjon.pen.plugin.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import no.nav.pensjon.pen.plugin.dialog.ChangeAktivitetNumberDialog

/**
 * Intention action (Alt+Enter) that changes the A-number of an Aktivitet.
 *
 * Updates file name, class names and references in all sibling .kt files.
 * Discriminator value is NOT changed (to preserve backward compatibility).
 * Optionally renumbers subsequent aktiviteter.
 */
class ChangeAktivitetNumberIntentionAction : PsiElementBaseIntentionAction() {

    override fun getFamilyName() = "PEN Behandling"

    override fun getText() = "Endre aktivitetsnummer"

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val fileName = element.containingFile?.name ?: return false
        return fileName.matches(Regex("A\\d{3}_.*\\.kt"))
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val currentFile = element.containingFile ?: return
        val directory = currentFile.containingDirectory ?: return

        val match = Regex("^A(\\d{3})_(.+)\\.kt$").find(currentFile.name) ?: return
        val currentNumStr = "A${match.groupValues[1]}"
        val currentNum = match.groupValues[1].toInt()
        val behandlingName = guessBehandlingName(directory)

        val dialog = ChangeAktivitetNumberDialog(project, currentNumStr)
        if (!dialog.showAndGet()) return

        val newNumStr = dialog.newNumber
        val newNum = newNumStr.removePrefix("A").toInt()

        WriteCommandAction.runWriteCommandAction(project, "Endre aktivitetsnummer", null, {
            if (dialog.shouldRenumberSubsequent) {
                renumberSubsequent(project, directory, behandlingName, currentNum, newNum, currentFile)
            }

            // Renumber the target aktivitet itself
            renumberAktivitet(project, directory, currentFile, behandlingName, currentNumStr, newNumStr)
        })
    }

    /**
     * Renumbers subsequent aktiviteter when moving an aktivitet to a new number.
     * Calculates the offset and shifts all aktiviteter after the target position.
     */
    private fun renumberSubsequent(
        project: Project,
        directory: PsiDirectory,
        behandlingName: String,
        oldNum: Int,
        newNum: Int,
        currentFile: PsiFile,
    ) {
        // Find all aktivitet files except the current one, sorted by number
        val otherFiles = directory.files
            .filter { it != currentFile && it.name.matches(Regex("A\\d{3}_.*\\.kt")) }
            .mapNotNull { file ->
                extractNumber(file.name)?.let { num -> num to file }
            }
            .sortedBy { it.first }

        // Find files at or after newNum that need to be shifted to make room
        val filesToShift = otherFiles
            .filter { (num, _) -> num >= newNum }
            .sortedByDescending { it.first } // Process highest first to avoid conflicts

        if (filesToShift.isEmpty()) return

        for ((num, file) in filesToShift) {
            val shiftedNum = num + 1
            val oldStr = "A${num.toString().padStart(3, '0')}"
            val newStr = "A${shiftedNum.toString().padStart(3, '0')}"
            renumberAktivitet(project, directory, file, behandlingName, oldStr, newStr)
        }
    }

    /**
     * Renumbers a single aktivitet: updates class name references in all sibling
     * .kt files, then renames the file itself. Does NOT change discriminator values.
     */
    private fun renumberAktivitet(
        project: Project,
        directory: PsiDirectory,
        file: PsiFile,
        behandlingName: String,
        oldNumStr: String,
        newNumStr: String,
    ) {
        val oldClassPrefix = "$behandlingName$oldNumStr"
        val newClassPrefix = "$behandlingName$newNumStr"

        val psiDocManager = PsiDocumentManager.getInstance(project)

        for (sibling in directory.files) {
            if (!sibling.name.endsWith(".kt")) continue
            val doc = psiDocManager.getDocument(sibling) ?: continue
            if (!doc.text.contains(oldClassPrefix)) continue
            doc.setText(doc.text.replace(oldClassPrefix, newClassPrefix))
            psiDocManager.commitDocument(doc)
        }

        val description = file.name.removePrefix("${oldNumStr}_")
        file.virtualFile.rename(this, "${newNumStr}_$description")
    }

    private fun extractNumber(fileName: String): Int? {
        return Regex("^A(\\d{3})_").find(fileName)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun guessBehandlingName(directory: PsiDirectory): String {
        val behandlingFile = directory.files.firstOrNull { it.name.endsWith("Behandling.kt") }
        return behandlingFile?.name?.removeSuffix("Behandling.kt") ?: ""
    }
}
