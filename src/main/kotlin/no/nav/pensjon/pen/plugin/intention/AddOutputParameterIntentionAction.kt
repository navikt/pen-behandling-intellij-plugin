package no.nav.pensjon.pen.plugin.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import no.nav.pensjon.pen.plugin.dialog.AddParameterDialog

class AddOutputParameterIntentionAction : PsiElementBaseIntentionAction() {

    override fun getFamilyName() = "PEN Behandling"

    override fun getText() = "Legg til output-parameter"

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val text = element.containingFile?.text ?: return false
        return ParameterCodeModifier.isBehandlingFile(text) || ParameterCodeModifier.isAktivitetFile(text)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = element.containingFile ?: return
        val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val text = doc.text

        val isBehandling = ParameterCodeModifier.isBehandlingFile(text)

        val dialog = AddParameterDialog(project, "Legg til output-parameter")
        if (!dialog.showAndGet()) return

        val paramName = dialog.paramName
        val paramType = dialog.paramType

        WriteCommandAction.runWriteCommandAction(project, "Legg til output-parameter", null, {
            val newText = ParameterCodeModifier.addOutputParameter(text, paramName, paramType, isBehandling)
            doc.setText(newText)
            PsiDocumentManager.getInstance(project).commitDocument(doc)
        })
    }
}
