package no.nav.pensjon.pen.plugin.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import no.nav.pensjon.pen.plugin.action.PackageUtil
import no.nav.pensjon.pen.plugin.dialog.NewAktivitetDialog
import no.nav.pensjon.pen.plugin.generator.AktivitetGenerator

/**
 * Intention action (Alt+Enter) that offers to add a new Aktivitet when the caret
 * is inside a Behandling class or an existing Aktivitet file.
 *
 * The new aktivitet is inserted after the current one. If this causes a number
 * collision with subsequent aktiviteter, those are automatically renumbered
 * (file names and class names updated).
 */
class AddAktivitetIntentionAction : PsiElementBaseIntentionAction() {

    override fun getFamilyName() = "PEN Behandling"

    override fun getText() = "Add new Aktivitet to this Behandling"

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        val fileName = file.name
        return fileName.endsWith("Behandling.kt") || fileName.matches(Regex("A\\d{3}_.*\\.kt"))
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val currentFile = element.containingFile ?: return
        val directory = currentFile.containingDirectory ?: return
        val behandlingName = guessBehandlingName(directory)

        val currentNumber = extractNumber(currentFile.name)
        val newNumber = if (currentNumber != null) {
            currentNumber + 1
        } else {
            val highest = findHighestNumber(directory)
            if (highest != null) highest + 1 else 100
        }

        val dialog = NewAktivitetDialog(project, behandlingName)
        if (!dialog.showAndGet()) return

        val newNumberStr = "A${newNumber.toString().padStart(3, '0')}"
        val model = dialog.getModel(newNumberStr)
        val packageName = PackageUtil.getPackageName(directory)
        val kotlinFileType = FileTypeManager.getInstance().getFileTypeByExtension("kt")

        WriteCommandAction.runWriteCommandAction(project, "Create Aktivitet", null, {
            // Renumber existing files with number >= newNumber (highest first to avoid conflicts)
            val filesToRenumber = directory.files
                .filter { f -> extractNumber(f.name)?.let { it >= newNumber } == true }
                .sortedByDescending { f -> extractNumber(f.name) }

            for (file in filesToRenumber) {
                val oldNum = extractNumber(file.name)!!
                renumberAktivitet(project, directory, file, behandlingName, oldNum, oldNum + 1)
            }

            // Create the new aktivitet file
            val content = AktivitetGenerator.generate(
                packageName,
                model.behandlingName,
                model.aktivitetNumber,
                model.aktivitetDescription,
                model.isLastAktivitet,
            )
            val newFile = PsiFileFactory.getInstance(project).createFileFromText(
                "${model.aktivitetNumber}_${model.aktivitetDescription}.kt",
                kotlinFileType,
                content
            )
            val added = directory.add(newFile)

            added.containingFile?.virtualFile?.let {
                FileEditorManager.getInstance(project).openFile(it, true)
            }
        })
    }

    /**
     * Renumbers an aktivitet file: updates class name references in all sibling
     * .kt files, then renames the file itself.
     */
    private fun renumberAktivitet(
        project: Project,
        directory: PsiDirectory,
        file: PsiFile,
        behandlingName: String,
        oldNum: Int,
        newNum: Int
    ) {
        val oldNumStr = "A${oldNum.toString().padStart(3, '0')}"
        val newNumStr = "A${newNum.toString().padStart(3, '0')}"
        val oldClassPrefix = "${behandlingName}${oldNumStr}"
        val newClassPrefix = "${behandlingName}${newNumStr}"

        val psiDocManager = PsiDocumentManager.getInstance(project)

        // Update class name references in all .kt files in the directory
        for (sibling in directory.files) {
            if (!sibling.name.endsWith(".kt")) continue
            val doc = psiDocManager.getDocument(sibling) ?: continue
            if (!doc.text.contains(oldClassPrefix)) continue
            doc.setText(doc.text.replace(oldClassPrefix, newClassPrefix))
            psiDocManager.commitDocument(doc)
        }

        // Rename the file
        val description = file.name.removePrefix("${oldNumStr}_")
        file.virtualFile.rename(this, "${newNumStr}_${description}")
    }

    private fun extractNumber(fileName: String): Int? {
        return Regex("^A(\\d{3})_").find(fileName)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun findHighestNumber(directory: PsiDirectory): Int? {
        return directory.files
            .mapNotNull { extractNumber(it.name) }
            .maxOrNull()
    }

    private fun guessBehandlingName(directory: PsiDirectory): String {
        val behandlingFile = directory.files.firstOrNull { it.name.endsWith("Behandling.kt") }
        return behandlingFile?.name?.removeSuffix("Behandling.kt") ?: ""
    }
}
