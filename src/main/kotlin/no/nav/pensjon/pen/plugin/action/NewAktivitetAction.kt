package no.nav.pensjon.pen.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import no.nav.pensjon.pen.plugin.dialog.NewAktivitetDialog
import no.nav.pensjon.pen.plugin.generator.AktivitetGenerator

class NewAktivitetAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val directory = getTargetDirectory(e) ?: return

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

    override fun update(e: AnActionEvent) {
        val directory = getTargetDirectory(e)
        e.presentation.isEnabledAndVisible = directory != null && e.project != null
    }

    private fun getTargetDirectory(e: AnActionEvent): PsiDirectory? {
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        return psiElement as? PsiDirectory
            ?: psiElement?.containingFile?.containingDirectory
    }

    /**
     * Tries to guess the Behandling name from existing files in the directory.
     * Looks for files ending in "Behandling.kt".
     */
    private fun guessBehandlingName(directory: PsiDirectory): String {
        val behandlingFile = directory.files.firstOrNull { file ->
            file.name.endsWith("Behandling.kt")
        }
        return behandlingFile?.name
            ?.removeSuffix("Behandling.kt")
            ?: ""
    }

    /**
     * Suggests the next aktivitet number by looking at existing A\d{3} files.
     */
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
