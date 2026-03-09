package no.nav.pensjon.pen.plugin.action

import com.intellij.ide.IdeView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import no.nav.pensjon.pen.plugin.dialog.NewBehandlingDialog
import no.nav.pensjon.pen.plugin.generator.AktivitetGenerator
import no.nav.pensjon.pen.plugin.generator.BehandlingGenerator

class NewBehandlingAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val ideView = e.getData(LangDataKeys.IDE_VIEW) ?: return
        val directory = ideView.orChooseDirectory ?: return

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

            addedBehandling.containingFile?.virtualFile?.let {
                FileEditorManager.getInstance(project).openFile(it, true)
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val ideView = e.getData(LangDataKeys.IDE_VIEW)
        e.presentation.isEnabledAndVisible = e.project != null && ideView != null && ideView.directories.isNotEmpty()
    }
}
