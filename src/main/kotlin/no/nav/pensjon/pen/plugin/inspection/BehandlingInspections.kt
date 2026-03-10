package no.nav.pensjon.pen.plugin.inspection

import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDirectory
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElementOfType

/**
 * All inspections for Behandling subclasses, consolidated into a single
 * checkClass() call for performance.
 *
 * Checks:
 * - Missing @Entity (ERROR)
 * - Missing @DiscriminatorValue (ERROR)
 * - @DiscriminatorValue does not match convention (WARNING)
 * - Missing @ForvalgtAnsvarligTeam (WARNING)
 */
class BehandlingInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isBehandlingSubclass(aClass)) return null

        val problems = mutableListOf<ProblemDescriptor>()
        val anchor = aClass.uastAnchor?.sourcePsi ?: return null

        // Missing @Entity
        if (!hasAnnotation(aClass, "Entity")) {
            problems += manager.createProblemDescriptor(
                anchor,
                "Behandling subclass '${aClass.name}' is missing @Entity. " +
                        "JPA requires this for single-table inheritance.",
                isOnTheFly, emptyArray(), ProblemHighlightType.ERROR
            )
        }

        // @DiscriminatorValue checks
        val discAnnotation = findAnnotation(aClass, "DiscriminatorValue")
        if (discAnnotation == null) {
            problems += manager.createProblemDescriptor(
                anchor,
                "Behandling subclass '${aClass.name}' is missing @DiscriminatorValue. " +
                        "JPA needs this to identify the subclass in the single-table hierarchy.",
                isOnTheFly, emptyArray(), ProblemHighlightType.ERROR
            )
        } else {
            val value = discAnnotation.findAttributeValue("value")
                ?.evaluate() as? String
            val className = aClass.name
            if (value != null && className != null) {
                val expected = className.removeSuffix("Behandling")
                if (value != expected) {
                    val psi = discAnnotation.sourcePsi ?: anchor
                    problems += manager.createProblemDescriptor(
                        psi,
                        "DiscriminatorValue '$value' does not match expected '$expected'.",
                        isOnTheFly, emptyArray(), ProblemHighlightType.WARNING
                    )
                }
            }
        }

        // Missing @ForvalgtAnsvarligTeam
        if (!hasAnnotation(aClass, "ForvalgtAnsvarligTeam")) {
            problems += manager.createProblemDescriptor(
                anchor,
                "Behandling subclass '${aClass.name}' is missing @ForvalgtAnsvarligTeam. " +
                        "Add @ForvalgtAnsvarligTeam(PESYS_FELLES), @ForvalgtAnsvarligTeam(PESYS_ALDER), " +
                        "or @ForvalgtAnsvarligTeam(PESYS_UFORE).",
                isOnTheFly, emptyArray(), ProblemHighlightType.WARNING
            )
        }

        return problems.toTypedArray().ifEmpty { null }
    }
}

/**
 * All inspections for Aktivitet subclasses, consolidated into a single
 * checkClass() call for performance.
 *
 * Checks:
 * - Missing @Entity (ERROR)
 * - Missing @DiscriminatorValue (ERROR)
 * - @DiscriminatorValue does not match convention (WARNING)
 * - Class name must end with "Aktivitet" (WARNING)
 * - Must have a matching AktivitetProcessor in same file (WARNING)
 */
class AktivitetInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isAktivitetSubclass(aClass)) return null

        val problems = mutableListOf<ProblemDescriptor>()
        val anchor = aClass.uastAnchor?.sourcePsi ?: return null
        val className = aClass.name

        // Missing @Entity
        if (!hasAnnotation(aClass, "Entity")) {
            problems += manager.createProblemDescriptor(
                anchor,
                "Aktivitet subclass '$className' is missing @Entity. " +
                        "JPA requires this for single-table inheritance.",
                isOnTheFly, emptyArray(), ProblemHighlightType.ERROR
            )
        }

        // @DiscriminatorValue checks
        val discAnnotation = findAnnotation(aClass, "DiscriminatorValue")
        if (discAnnotation == null) {
            problems += manager.createProblemDescriptor(
                anchor,
                "Aktivitet subclass '$className' is missing @DiscriminatorValue. " +
                        "JPA needs this to identify the subclass in the single-table hierarchy.",
                isOnTheFly, emptyArray(), ProblemHighlightType.ERROR
            )
        } else {
            val value = discAnnotation.findAttributeValue("value")
                ?.evaluate() as? String
            if (value != null && className != null) {
                val directory = aClass.sourcePsi?.containingFile?.containingDirectory
                val behandlingName = findBehandlingName(directory)
                if (behandlingName != null) {
                    val withoutPrefix = className.removePrefix(behandlingName)
                    val withoutNumber = withoutPrefix.replace(Regex("^A\\d{3,4}"), "")
                    val description = withoutNumber.removeSuffix("Aktivitet")
                    val expected = "${behandlingName}_${description}"
                    if (value != expected) {
                        val psi = discAnnotation.sourcePsi ?: anchor
                        problems += manager.createProblemDescriptor(
                            psi,
                            "DiscriminatorValue '$value' does not match expected '$expected'.",
                            isOnTheFly, emptyArray(), ProblemHighlightType.WARNING
                        )
                    }
                }
            }
        }

        // Class name must end with "Aktivitet"
        if (className != null && !className.endsWith("Aktivitet")) {
            problems += manager.createProblemDescriptor(
                anchor,
                "Aktivitet subclass '$className' should end with 'Aktivitet'. " +
                        "Suggested: '${className}Aktivitet'",
                isOnTheFly, emptyArray(), ProblemHighlightType.WARNING
            )
        }

        // Must have Processor in same file
        val uFile = aClass.sourcePsi?.containingFile?.toUElementOfType<UFile>()
        if (uFile != null) {
            val hasProcessor = uFile.classes.any { sibling ->
                sibling !== aClass && isAktivitetProcessorSubclass(sibling)
            }
            if (!hasProcessor) {
                problems += manager.createProblemDescriptor(
                    anchor,
                    "Aktivitet '$className' has no AktivitetProcessor in the same file. " +
                            "By convention, every Aktivitet entity should have a matching Processor.",
                    isOnTheFly, emptyArray(), ProblemHighlightType.WARNING
                )
            }
        }

        return problems.toTypedArray().ifEmpty { null }
    }
}

/**
 * All inspections for AktivitetProcessor subclasses, consolidated into a single
 * checkClass() call for performance.
 *
 * Checks:
 * - Missing @Component (WARNING)
 * - Behandling generic type doesn't match directory's Behandling (WARNING)
 * - Aktivitet generic type doesn't match Aktivitet in same file (WARNING)
 */
class ProcessorInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isAktivitetProcessorSubclass(aClass)) return null

        val problems = mutableListOf<ProblemDescriptor>()
        val anchor = aClass.uastAnchor?.sourcePsi ?: return null

        // Missing @Component
        val hasComponent = aClass.uAnnotations.any {
            val name = it.qualifiedName
            name?.endsWith("Component") == true || name?.endsWith("Service") == true
        }
        if (!hasComponent) {
            problems += manager.createProblemDescriptor(
                anchor,
                "AktivitetProcessor subclass '${aClass.name}' is missing @Component. " +
                        "Add @Component so Spring can discover it.",
                isOnTheFly, emptyArray(), ProblemHighlightType.WARNING
            )
        }

        // Generic type consistency checks
        val behandlingTypeName = getProcessorTypeArg(aClass, 0)
        val aktivitetTypeName = getProcessorTypeArg(aClass, 1)

        // Behandling type must match directory's Behandling
        if (behandlingTypeName != null) {
            val directory = aClass.sourcePsi?.containingFile?.containingDirectory
            val expectedBehandling = directory?.files
                ?.firstOrNull { it.name.endsWith("Behandling.kt") }
                ?.name?.removeSuffix(".kt")

            if (expectedBehandling != null && behandlingTypeName != expectedBehandling) {
                problems += manager.createProblemDescriptor(
                    anchor,
                    "Processor references '$behandlingTypeName' but the Behandling in this " +
                            "directory is '$expectedBehandling'.",
                    isOnTheFly, emptyArray(), ProblemHighlightType.WARNING
                )
            }
        }

        // Aktivitet type must exist in same file
        if (aktivitetTypeName != null) {
            val uFile = aClass.sourcePsi?.containingFile?.toUElementOfType<UFile>()
            if (uFile != null) {
                val hasMatchingAktivitet = uFile.classes.any { sibling ->
                    sibling !== aClass && sibling.name == aktivitetTypeName
                }
                if (!hasMatchingAktivitet) {
                    problems += manager.createProblemDescriptor(
                        anchor,
                        "Processor references '$aktivitetTypeName' but no such Aktivitet class " +
                                "exists in this file.",
                        isOnTheFly, emptyArray(), ProblemHighlightType.WARNING
                    )
                }
            }
        }

        return problems.toTypedArray().ifEmpty { null }
    }
}

// ==================== UAST utility functions (K1 and K2 compatible) ====================

private fun isBehandlingSubclass(aClass: UClass): Boolean {
    return aClass.uastSuperTypes.any { ref ->
        ref.type.presentableText == "Behandling"
    }
}

private fun isAktivitetSubclass(aClass: UClass): Boolean {
    return aClass.uastSuperTypes.any { ref ->
        val name = ref.type.presentableText
        name == "Aktivitet" || name.startsWith("AldeAktivitet")
    }
}

private fun isAktivitetProcessorSubclass(aClass: UClass): Boolean {
    return aClass.uastSuperTypes.any { ref ->
        ref.type.presentableText.startsWith("AktivitetProcessor") ||
                ref.type.presentableText.startsWith("AldeAktivitetProcessor")
    }
}

private fun hasAnnotation(aClass: UClass, simpleName: String): Boolean {
    return aClass.uAnnotations.any {
        it.qualifiedName?.endsWith(simpleName) == true
    }
}

private fun findAnnotation(aClass: UClass, simpleName: String) =
    aClass.uAnnotations.find { it.qualifiedName?.endsWith(simpleName) == true }

private fun findBehandlingName(directory: PsiDirectory?): String? {
    return directory?.files
        ?.firstOrNull { it.name.endsWith("Behandling.kt") }
        ?.name?.removeSuffix("Behandling.kt")
}

private fun getProcessorTypeArg(aClass: UClass, index: Int): String? {
    val superType = aClass.uastSuperTypes.find { ref ->
        val name = ref.type.presentableText
        name.startsWith("AktivitetProcessor") || name.startsWith("AldeAktivitetProcessor")
    } ?: return null

    val psiType = superType.type as? PsiClassType ?: return null
    return psiType.parameters.getOrNull(index)?.presentableText
}
