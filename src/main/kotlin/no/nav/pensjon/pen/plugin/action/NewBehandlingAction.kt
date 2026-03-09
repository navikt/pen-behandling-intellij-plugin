package no.nav.pensjon.pen.plugin.action

import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import no.nav.pensjon.pen.plugin.dialog.NewBehandlingDialog
import no.nav.pensjon.pen.plugin.generator.AktivitetGenerator
import no.nav.pensjon.pen.plugin.generator.BehandlingGenerator
import org.jetbrains.kotlin.idea.KotlinFileType

class NewBehandlingAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val directory = getTargetDirectory(e) ?: return

        val dialog = NewBehandlingDialog(project)
        if (!dialog.showAndGet()) return

        val model = dialog.getModel()
        val packageName = PackageUtil.getPackageName(directory)

        WriteCommandAction.runWriteCommandAction(project, "Create Behandling", null, {
            val psiFileFactory = PsiFileFactory.getInstance(project)

            val behandlingContent = BehandlingGenerator.generate(packageName, model)
            val behandlingFile = psiFileFactory.createFileFromText(
                "${model.name}Behandling.kt",
                KotlinFileType.INSTANCE,
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
                KotlinFileType.INSTANCE,
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
        val directory = getTargetDirectory(e)
        e.presentation.isEnabledAndVisible = directory != null && e.project != null
    }

    private fun getTargetDirectory(e: AnActionEvent): PsiDirectory? {
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        return psiElement as? PsiDirectory
            ?: psiElement?.containingFile?.containingDirectory
    }
}
