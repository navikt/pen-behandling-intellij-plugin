package no.nav.pensjon.pen.plugin.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import no.nav.pensjon.pen.plugin.action.PackageUtil
import no.nav.pensjon.pen.plugin.dialog.NewAktivitetDialog
import no.nav.pensjon.pen.plugin.generator.AktivitetGenerator

/**
 * Intention action (Alt+Enter) that offers to add a new Aktivitet when the caret
 * is inside a Behandling class or an existing Aktivitet file.
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

        val suggestedName = guessBehandlingName(directory)
        val suggestedNumber = guessNextAktivitetNumber(directory)

        val dialog = NewAktivitetDialog(project, suggestedName, suggestedNumber)
        if (!dialog.showAndGet()) return

        val model = dialog.getModel()
        val packageName = PackageUtil.getPackageName(directory)
        val kotlinFileType = FileTypeManager.getInstance().getFileTypeByExtension("kt")

        WriteCommandAction.runWriteCommandAction(project, "Create Aktivitet", null, {
            val psiFileFactory = PsiFileFactory.getInstance(project)

            val content = AktivitetGenerator.generate(
                packageName,
                model.behandlingName,
                model.aktivitetNumber,
                model.aktivitetDescription,
                model.isLastAktivitet,
            )
            val file = psiFileFactory.createFileFromText(
                "${model.aktivitetNumber}_${model.aktivitetDescription}.kt",
                kotlinFileType,
                content
            )
            val added = directory.add(file)

            added.containingFile?.virtualFile?.let {
                FileEditorManager.getInstance(project).openFile(it, true)
            }
        })
    }

    private fun guessBehandlingName(directory: PsiDirectory): String {
        val behandlingFile = directory.files.firstOrNull { file ->
            file.name.endsWith("Behandling.kt")
        }
        return behandlingFile?.name
            ?.removeSuffix("Behandling.kt")
            ?: ""
    }

    private fun guessNextAktivitetNumber(directory: PsiDirectory): String {
        val existingNumbers = directory.files
            .mapNotNull { file ->
                Regex("^A(\\d{3})_").find(file.name)?.groupValues?.get(1)?.toIntOrNull()
            }
            .sorted()

        if (existingNumbers.isEmpty()) return "A101"

        val last = existingNumbers.last()
        return "A${(last + 1).toString().padStart(3, '0')}"
    }
}
