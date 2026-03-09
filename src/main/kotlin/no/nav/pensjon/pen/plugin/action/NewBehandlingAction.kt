package no.nav.pensjon.pen.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import no.nav.pensjon.pen.plugin.dialog.NewBehandlingDialog
import no.nav.pensjon.pen.plugin.generator.AktivitetGenerator
import no.nav.pensjon.pen.plugin.generator.BehandlingGenerator

class NewBehandlingAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val directory = getTargetDirectory(e) ?: return

        val dialog = NewBehandlingDialog(project)
        if (!dialog.showAndGet()) return

        val model = dialog.getModel()
        val packageName = PackageUtil.getPackageName(directory)
        val kotlinFileType = FileTypeManager.getInstance().getFileTypeByExtension("kt")

        WriteCommandAction.runWriteCommandAction(project, "Create Behandling", null, {
            val psiFileFactory = PsiFileFactory.getInstance(project)

            val behandlingContent = BehandlingGenerator.generate(packageName, model)
            val behandlingFile = psiFileFactory.createFileFromText(
                "${model.name}Behandling.kt",
                kotlinFileType,
                behandlingContent
            )
            val addedBehandling = directory.add(behandlingFile)

            val aktivitetContent = AktivitetGenerator.generate(
                packageName,
                model.name,
                model.initialAktivitetNumber,
                model.initialAktivitetDescription,
                isLastAktivitet = true,
            )
            val aktivitetFile = psiFileFactory.createFileFromText(
                "${model.initialAktivitetNumber}_${model.initialAktivitetDescription}.kt",
                kotlinFileType,
                aktivitetContent
            )
            directory.add(aktivitetFile)

            // Open the behandling file in editor
            addedBehandling.containingFile?.virtualFile?.let {
                FileEditorManager.getInstance(project).openFile(it, true)
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && getTargetDirectory(e) != null
    }

    private fun getTargetDirectory(e: AnActionEvent): PsiDirectory? {
        // Try PSI_ELEMENT first (right-click on directory)
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        if (psiElement is PsiDirectory) return psiElement
        psiElement?.containingFile?.containingDirectory?.let { return it }

        // Fall back to VIRTUAL_FILE (right-click on file, editor tab, etc.)
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
        val project = e.project ?: return null
        val psiManager = PsiManager.getInstance(project)
        val dir = if (virtualFile.isDirectory) virtualFile else virtualFile.parent ?: return null
        return psiManager.findDirectory(dir)
    }
}
