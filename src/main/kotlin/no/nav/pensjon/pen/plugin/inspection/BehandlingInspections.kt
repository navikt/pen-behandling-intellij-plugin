package no.nav.pensjon.pen.plugin.inspection

import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClassType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElementOfType

// ==================== Existing inspections ====================

/**
 * Warns when @DiscriminatorValue on a Behandling subclass ends with "Behandling".
 */
class DiscriminatorValueInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isBehandlingSubclass(aClass)) return null

        val annotation = aClass.uAnnotations.find {
            it.qualifiedName?.endsWith("DiscriminatorValue") == true
        } ?: return null

        val value = annotation.findAttributeValue("value")
            ?.evaluate() as? String ?: return null

        if (!value.endsWith("Behandling")) return null

        val psi = annotation.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "DiscriminatorValue '$value' should not end with 'Behandling'. " +
                        "Suggested: '${value.removeSuffix("Behandling")}'",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.WARNING
            )
        )
    }
}

/**
 * Warns when a Behandling subclass is missing @ForvalgtAnsvarligTeam.
 */
class MissingForvalgtAnsvarligTeamInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isBehandlingSubclass(aClass)) return null

        val hasAnnotation = aClass.uAnnotations.any {
            it.qualifiedName?.endsWith("ForvalgtAnsvarligTeam") == true
        }
        if (hasAnnotation) return null

        val psi = aClass.uastAnchor?.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "Behandling subclass '${aClass.name}' is missing @ForvalgtAnsvarligTeam annotation. " +
                        "Add @ForvalgtAnsvarligTeam(PESYS_FELLES), @ForvalgtAnsvarligTeam(PESYS_ALDER), " +
                        "or @ForvalgtAnsvarligTeam(PESYS_UFORE).",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.WARNING
            )
        )
    }
}

/**
 * Warns when an AktivitetProcessor subclass is missing @Component.
 */
class MissingComponentInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isAktivitetProcessorSubclass(aClass)) return null

        val hasComponent = aClass.uAnnotations.any {
            val name = it.qualifiedName
            name?.endsWith("Component") == true || name?.endsWith("Service") == true
        }
        if (hasComponent) return null

        val psi = aClass.uastAnchor?.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "AktivitetProcessor subclass '${aClass.name}' is missing @Component annotation. " +
                        "Add @Component so Spring can discover it.",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.WARNING
            )
        )
    }
}

// ==================== New inspections ====================

/**
 * Errors when a Behandling subclass is missing @Entity.
 *
 * JPA requires @Entity for single-table inheritance to work. Without it,
 * the class won't be persisted — the compiler won't catch this.
 */
class MissingEntityOnBehandlingInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isBehandlingSubclass(aClass)) return null
        if (hasAnnotation(aClass, "Entity")) return null

        val psi = aClass.uastAnchor?.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "Behandling subclass '${aClass.name}' is missing @Entity. " +
                        "JPA requires this for single-table inheritance.",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.ERROR
            )
        )
    }
}

/**
 * Errors when an Aktivitet subclass is missing @Entity.
 */
class MissingEntityOnAktivitetInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isAktivitetSubclass(aClass)) return null
        if (hasAnnotation(aClass, "Entity")) return null

        val psi = aClass.uastAnchor?.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "Aktivitet subclass '${aClass.name}' is missing @Entity. " +
                        "JPA requires this for single-table inheritance.",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.ERROR
            )
        )
    }
}

/**
 * Errors when a Behandling subclass is missing @DiscriminatorValue.
 *
 * Without a discriminator value, JPA cannot distinguish this subclass
 * in the single-table inheritance hierarchy.
 */
class MissingDiscriminatorOnBehandlingInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isBehandlingSubclass(aClass)) return null
        if (hasAnnotation(aClass, "DiscriminatorValue")) return null

        val psi = aClass.uastAnchor?.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "Behandling subclass '${aClass.name}' is missing @DiscriminatorValue. " +
                        "JPA needs this to identify the subclass in the single-table hierarchy.",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.ERROR
            )
        )
    }
}

/**
 * Errors when an Aktivitet subclass is missing @DiscriminatorValue.
 */
class MissingDiscriminatorOnAktivitetInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isAktivitetSubclass(aClass)) return null
        if (hasAnnotation(aClass, "DiscriminatorValue")) return null

        val psi = aClass.uastAnchor?.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "Aktivitet subclass '${aClass.name}' is missing @DiscriminatorValue. " +
                        "JPA needs this to identify the subclass in the single-table hierarchy.",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.ERROR
            )
        )
    }
}

/**
 * Warns when @DiscriminatorValue on an Aktivitet subclass ends with "Aktivitet".
 *
 * Per PEN conventions, discriminator values are semantic names
 * like "FalskId_SjekkOmIdentErFalsk", not suffixed with "Aktivitet".
 */
class AktivitetDiscriminatorValueInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isAktivitetSubclass(aClass)) return null

        val annotation = aClass.uAnnotations.find {
            it.qualifiedName?.endsWith("DiscriminatorValue") == true
        } ?: return null

        val value = annotation.findAttributeValue("value")
            ?.evaluate() as? String ?: return null

        if (!value.endsWith("Aktivitet")) return null

        val psi = annotation.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "DiscriminatorValue '$value' should not end with 'Aktivitet'. " +
                        "Suggested: '${value.removeSuffix("Aktivitet")}'",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.WARNING
            )
        )
    }
}

/**
 * Warns when an Aktivitet entity class has no matching AktivitetProcessor
 * in the same file.
 *
 * By PEN convention, every Aktivitet entity should have its Processor
 * defined in the same file.
 */
class AktivitetWithoutProcessorInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isAktivitetSubclass(aClass)) return null

        val uFile = aClass.sourcePsi?.containingFile?.toUElementOfType<UFile>() ?: return null
        val hasProcessor = uFile.classes.any { sibling ->
            sibling !== aClass && isAktivitetProcessorSubclass(sibling)
        }
        if (hasProcessor) return null

        val psi = aClass.uastAnchor?.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "Aktivitet '${aClass.name}' has no AktivitetProcessor in the same file. " +
                        "By convention, every Aktivitet entity should have a matching Processor.",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.WARNING
            )
        )
    }
}

/**
 * Warns when an Aktivitet subclass name doesn't end with "Aktivitet".
 */
class AktivitetNamingInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isAktivitetSubclass(aClass)) return null
        val name = aClass.name ?: return null
        if (name.endsWith("Aktivitet")) return null

        val psi = aClass.uastAnchor?.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "Aktivitet subclass '$name' should end with 'Aktivitet'. " +
                        "Suggested: '${name}Aktivitet'",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.WARNING
            )
        )
    }
}

// ==================== Consistency inspections ====================

/**
 * Warns when an AktivitetProcessor references a Behandling type that doesn't
 * match the Behandling class in the same directory.
 */
class ProcessorBehandlingMismatchInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isAktivitetProcessorSubclass(aClass)) return null

        val behandlingTypeName = getProcessorTypeArg(aClass, 0) ?: return null

        val directory = aClass.sourcePsi?.containingFile?.containingDirectory ?: return null
        val expectedBehandling = directory.files
            .firstOrNull { it.name.endsWith("Behandling.kt") }
            ?.name?.removeSuffix(".kt")
            ?: return null

        if (behandlingTypeName == expectedBehandling) return null

        val psi = aClass.uastAnchor?.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "Processor references '$behandlingTypeName' but the Behandling in this " +
                        "directory is '$expectedBehandling'.",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.WARNING
            )
        )
    }
}

/**
 * Warns when an AktivitetProcessor references an Aktivitet type that doesn't
 * exist in the same file.
 *
 * By convention, the Aktivitet entity and its Processor are always in the same file.
 */
class ProcessorAktivitetMismatchInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isAktivitetProcessorSubclass(aClass)) return null

        val aktivitetTypeName = getProcessorTypeArg(aClass, 1) ?: return null

        val uFile = aClass.sourcePsi?.containingFile?.toUElementOfType<UFile>() ?: return null
        val hasMatchingAktivitet = uFile.classes.any { sibling ->
            sibling !== aClass && sibling.name == aktivitetTypeName
        }
        if (hasMatchingAktivitet) return null

        val psi = aClass.uastAnchor?.sourcePsi ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                psi,
                "Processor references '$aktivitetTypeName' but no such Aktivitet class " +
                        "exists in this file.",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.WARNING
            )
        )
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

/**
 * Extracts a generic type argument from an AktivitetProcessor supertype.
 * Index 0 = Behandling type, index 1 = Aktivitet type.
 */
private fun getProcessorTypeArg(aClass: UClass, index: Int): String? {
    val superType = aClass.uastSuperTypes.find { ref ->
        val name = ref.type.presentableText
        name.startsWith("AktivitetProcessor") || name.startsWith("AldeAktivitetProcessor")
    } ?: return null

    val psiType = superType.type as? PsiClassType ?: return null
    return psiType.parameters.getOrNull(index)?.presentableText
}
