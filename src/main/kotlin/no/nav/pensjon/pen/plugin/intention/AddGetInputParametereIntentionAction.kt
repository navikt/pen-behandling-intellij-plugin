package no.nav.pensjon.pen.plugin.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

/**
 * Intention action (Alt+Enter) that generates a getInputParametere() override
 * in a Behandling class, based on existing input getter properties.
 *
 * This makes input parameters visible in Verdande.
 */
class AddGetInputParametereIntentionAction : PsiElementBaseIntentionAction() {

    override fun getFamilyName() = "PEN Behandling"

    override fun getText() = "Legg til getInputParametere()"

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val text = element.containingFile?.text ?: return false
        return ParameterCodeModifier.isBehandlingFile(text)
                && !text.contains("getInputParametere()")
                && text.contains("@Column(name = \"INPUT\")")
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = element.containingFile ?: return
        val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val text = doc.text

        val inputProperties = findInputProperties(text)
        if (inputProperties.isEmpty()) return

        val mapEntries = inputProperties.joinToString(",\n") { (name, _) ->
            val label = name.replaceFirstChar { it.uppercase() }
            "        \"$label\" to $name.toString()"
        }

        val override = buildString {
            appendLine()
            appendLine("    override fun getInputParametere(): Map<String, String?> = mapOf(")
            appendLine(mapEntries)
            append("    )")
        }

        // Insert before the data class or before the closing brace
        val insertPos = findInsertPosition(text)
        if (insertPos == -1) return

        WriteCommandAction.runWriteCommandAction(project, "Legg til getInputParametere()", null, {
            val newText = text.substring(0, insertPos) + override + "\n" + text.substring(insertPos)
            doc.setText(newText)
            PsiDocumentManager.getInstance(project).commitDocument(doc)
        })
    }

    /**
     * Finds input getter properties by looking for patterns like:
     *   val someProperty: SomeType
     *       get() = Json.decodeFromString<...>(input).someProperty
     */
    private fun findInputProperties(text: String): List<Pair<String, String>> {
        val pattern = Regex("""val (\w+): (\S+)\s*\n\s*get\(\) = Json\.decodeFromString<\w+>\(input\)\.\1""")
        return pattern.findAll(text).map { it.groupValues[1] to it.groupValues[2] }.toList()
    }

    /**
     * Finds the best insert position — before @Serializable data class or before closing brace.
     */
    private fun findInsertPosition(text: String): Int {
        val serializableIdx = text.indexOf("    @Serializable\n    data class Parametere(")
        if (serializableIdx != -1) return serializableIdx

        val altIdx = text.indexOf("    @Serializable\n    data class Parameter(")
        if (altIdx != -1) return altIdx

        // Fall back to before the last closing brace of the first class
        val classMatch = Regex("""class \w+[^{]*\{""").find(text) ?: return -1
        return findMatchingBrace(text, classMatch.range.last) ?: -1
    }

    private fun findMatchingBrace(text: String, openPos: Int): Int? {
        var depth = 1
        var pos = openPos + 1
        while (pos < text.length && depth > 0) {
            when (text[pos]) {
                '{' -> depth++
                '}' -> depth--
            }
            pos++
        }
        return if (depth == 0) pos - 1 else null
    }
}
