package no.nav.pensjon.pen.plugin.intention

/**
 * Utilities for modifying Behandling/Aktivitet source code to add input/output parameters.
 *
 * Handles two cases:
 * 1. Adding a parameter to an EXISTING Input/Output/Parametere data class
 * 2. Creating a new input/output block from scratch
 */
object ParameterCodeModifier {

    private val INPUT_IMPORTS = listOf(
        "import jakarta.persistence.Column",
        "import jakarta.persistence.Lob",
        "import kotlinx.serialization.Serializable",
        "import kotlinx.serialization.encodeToString",
        "import kotlinx.serialization.json.Json",
    )

    private val OUTPUT_IMPORTS = INPUT_IMPORTS

    fun isBehandlingFile(text: String): Boolean =
        Regex(""":\s*Behandling\s*\(""").containsMatchIn(text)

    fun isAktivitetFile(text: String): Boolean =
        Regex(""":\s*Aktivitet\s*\(""").containsMatchIn(text) ||
                Regex(""":\s*AldeAktivitet\s*\(""").containsMatchIn(text)

    /**
     * Finds the input data class name used in the file.
     * Behandling uses "Parametere" or "Parameter", Aktivitet uses "Input".
     */
    fun findInputDataClassName(text: String, isBehandling: Boolean): String? {
        if (text.contains("data class Parametere(")) return "Parametere"
        if (text.contains("data class Parameter(")) return "Parameter"
        if (text.contains("data class Input(")) return "Input"
        return null
    }

    fun defaultInputDataClassName(isBehandling: Boolean): String =
        if (isBehandling) "Parametere" else "Input"

    // ==================== Add input parameter ====================

    fun addInputParameter(text: String, paramName: String, paramType: String, isBehandling: Boolean): String {
        val hasExisting = text.contains("@Column(name = \"INPUT\")")
        val dataClassName = findInputDataClassName(text, isBehandling) ?: defaultInputDataClassName(isBehandling)

        var result = text
        if (hasExisting) {
            result = addFieldToDataClass(result, dataClassName, paramName, paramType, isOutput = false)
            result = addGetterBeforeDataClass(result, dataClassName, paramName, paramType, isOutput = false)
        } else {
            result = addNewInputBlock(result, dataClassName, paramName, paramType)
            result = addImportsIfMissing(result, INPUT_IMPORTS)
        }
        return result
    }

    // ==================== Add output parameter ====================

    fun addOutputParameter(text: String, paramName: String, paramType: String, isBehandling: Boolean): String {
        val hasExisting = text.contains("@Column(name = \"OUTPUT\")")

        var result = text
        if (hasExisting) {
            result = addFieldToDataClass(result, "Output", paramName, paramType, isOutput = true)
            result = addGetterBeforeDataClass(result, "Output", paramName, paramType, isOutput = true)
        } else {
            result = addNewOutputBlock(result, paramName, paramType, isBehandling)
            result = addImportsIfMissing(result, OUTPUT_IMPORTS)
        }
        return result
    }

    // ==================== Helper functions ====================

    /**
     * Adds a field to an existing data class by finding the closing ')' and inserting before it.
     */
    private fun addFieldToDataClass(
        text: String,
        dataClassName: String,
        paramName: String,
        paramType: String,
        isOutput: Boolean,
    ): String {
        val marker = "data class $dataClassName("
        val classStart = text.indexOf(marker)
        if (classStart == -1) return text

        val openParen = classStart + marker.length - 1
        val closeParen = findMatchingParen(text, openParen) ?: return text

        val existingContent = text.substring(openParen + 1, closeParen).trim()
        val defaultSuffix = if (isOutput) "? = null" else ""

        return if (existingContent.isEmpty()) {
            text.substring(0, openParen + 1) +
                    "\n        val $paramName: $paramType$defaultSuffix\n    " +
                    text.substring(closeParen)
        } else {
            text.substring(0, closeParen) +
                    ",\n        val $paramName: $paramType$defaultSuffix" +
                    text.substring(closeParen)
        }
    }

    /**
     * Adds a getter property before the @Serializable data class declaration.
     */
    private fun addGetterBeforeDataClass(
        text: String,
        dataClassName: String,
        paramName: String,
        paramType: String,
        isOutput: Boolean,
    ): String {
        // Find "@Serializable\n    data class DataClassName("
        val serializable = "    @Serializable\n    data class $dataClassName("
        var insertPos = text.indexOf(serializable)

        if (insertPos == -1) {
            // Try without @Serializable
            val justClass = "    data class $dataClassName("
            insertPos = text.indexOf(justClass)
            if (insertPos == -1) return text
        }

        val getter = if (isOutput) {
            "    val $paramName: $paramType?\n        get() = output?.let { Json.decodeFromString<$dataClassName>(it).$paramName }\n\n"
        } else {
            "    val $paramName: $paramType\n        get() = Json.decodeFromString<$dataClassName>(input).$paramName\n\n"
        }

        return text.substring(0, insertPos) + getter + text.substring(insertPos)
    }

    /**
     * Creates a complete input block from scratch and inserts before the closing '}'.
     */
    private fun addNewInputBlock(
        text: String,
        dataClassName: String,
        paramName: String,
        paramType: String,
    ): String {
        val insertPos = findClassBodyEnd(text) ?: return text

        val block = buildString {
            appendLine()
            appendLine("    @Lob")
            appendLine("    @Column(name = \"INPUT\")")
            appendLine("    private var input: String = \"{}\" // TODO: Initialiser med Json.encodeToString($dataClassName(...))")
            appendLine()
            appendLine("    val $paramName: $paramType")
            appendLine("        get() = Json.decodeFromString<$dataClassName>(input).$paramName")
            appendLine()
            appendLine("    @Serializable")
            appendLine("    data class $dataClassName(")
            appendLine("        val $paramName: $paramType")
            appendLine("    )")
        }

        return text.substring(0, insertPos) + block + text.substring(insertPos)
    }

    /**
     * Creates a complete output block from scratch and inserts before the closing '}'.
     */
    private fun addNewOutputBlock(
        text: String,
        paramName: String,
        paramType: String,
        isBehandling: Boolean,
    ): String {
        val insertPos = findClassBodyEnd(text) ?: return text

        val block = buildString {
            appendLine()
            appendLine("    @Lob")
            appendLine("    @Column(name = \"OUTPUT\")")
            appendLine("    private var output: String? = null")
            appendLine()
            if (isBehandling) {
                appendLine("    fun getOutput(): Output = Json.decodeFromString(output!!)")
                appendLine()
                appendLine("    fun saveOutput(output: Output) {")
                appendLine("        this.output = Json.encodeToString(output)")
                appendLine("    }")
            } else {
                appendLine("    fun setOutput($paramName: $paramType) {")
                appendLine("        output = Json.encodeToString(Output($paramName))")
                appendLine("    }")
            }
            appendLine()
            appendLine("    val $paramName: $paramType?")
            appendLine("        get() = output?.let { Json.decodeFromString<Output>(it).$paramName }")
            appendLine()
            appendLine("    @Serializable")
            appendLine("    data class Output(")
            appendLine("        val $paramName: $paramType? = null")
            appendLine("    )")
        }

        return text.substring(0, insertPos) + block + text.substring(insertPos)
    }

    /**
     * Adds missing imports after the last existing import line.
     */
    private fun addImportsIfMissing(text: String, imports: List<String>): String {
        var result = text
        for (imp in imports) {
            if (!result.contains(imp)) {
                val lastImportIdx = result.lastIndexOf("\nimport ")
                if (lastImportIdx != -1) {
                    val endOfLine = result.indexOf("\n", lastImportIdx + 1)
                    if (endOfLine != -1) {
                        result = result.substring(0, endOfLine) + "\n$imp" + result.substring(endOfLine)
                    }
                }
            }
        }
        return result
    }

    /**
     * Finds the position of the last '}' in the file (end of the outer class body).
     * For Aktivitet files with both entity and processor, finds the end of the first class.
     */
    private fun findClassBodyEnd(text: String): Int? {
        // Find the first class declaration
        val classMatch = Regex("""class \w+[^{]*\{""").find(text) ?: return null
        val openBrace = classMatch.range.last
        return findMatchingBrace(text, openBrace)
    }

    private fun findMatchingParen(text: String, openPos: Int): Int? {
        var depth = 1
        var pos = openPos + 1
        while (pos < text.length && depth > 0) {
            when (text[pos]) {
                '(' -> depth++
                ')' -> depth--
            }
            pos++
        }
        return if (depth == 0) pos - 1 else null
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
