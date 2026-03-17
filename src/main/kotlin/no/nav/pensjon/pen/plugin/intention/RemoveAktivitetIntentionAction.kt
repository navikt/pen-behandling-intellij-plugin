package no.nav.pensjon.pen.plugin.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Intention action (Alt+Enter) that removes an Aktivitet file and renumbers
 * all subsequent aktiviteter (those with a higher number) by decrementing by 1.
 */
class RemoveAktivitetIntentionAction : PsiElementBaseIntentionAction() {

    override fun getFamilyName() = "PEN Behandling"

    override fun getText() = "Slett aktivitet og renummer etterfølgende"

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val fileName = element.containingFile?.name ?: return false
        return fileName.matches(Regex("A\\d{3}_.*\\.kt"))
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val currentFile = element.containingFile ?: return
        val directory = currentFile.containingDirectory ?: return

        val match = Regex("^A(\\d{3})_(.+)\\.kt$").find(currentFile.name) ?: return
        val currentNum = match.groupValues[1].toInt()
        val behandlingName = guessBehandlingName(directory)

        val confirmed = Messages.showOkCancelDialog(
            project,
            "Er du sikker på at du vil slette '${currentFile.name}' og renummer etterfølgende aktiviteter?",
            "Slett aktivitet",
            "Slett",
            "Avbryt",
            Messages.getWarningIcon(),
        )
        if (confirmed != Messages.OK) return

        // Collect subsequent files before deletion
        val filesToRenumber = directory.files
            .mapNotNull { file -> extractNumber(file.name)?.let { num -> num to file } }
            .filter { (num, _) -> num > currentNum }
            .sortedBy { it.first }

        WriteCommandAction.runWriteCommandAction(project, "Slett aktivitet", null, {
            FileEditorManager.getInstance(project).closeFile(currentFile.virtualFile)
            currentFile.virtualFile.delete(this)
            renumberSubsequent(project, directory, behandlingName, filesToRenumber)
        })
    }

    private fun renumberSubsequent(
        project: Project,
        directory: PsiDirectory,
        behandlingName: String,
        filesToRenumber: List<Pair<Int, PsiFile>>,
    ) {
        for ((num, file) in filesToRenumber) {
            val oldNumStr = "A${num.toString().padStart(3, '0')}"
            val newNumStr = "A${(num - 1).toString().padStart(3, '0')}"
            renumberAktivitet(project, directory, file, behandlingName, oldNumStr, newNumStr)
        }
    }

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
