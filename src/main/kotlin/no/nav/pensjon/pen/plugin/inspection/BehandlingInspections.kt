package no.nav.pensjon.pen.plugin.inspection

import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import org.jetbrains.uast.UClass

/**
 * Warns when @DiscriminatorValue on a Behandling subclass ends with "Behandling".
 *
 * Per the PEN conventions, the discriminator value should be the behandling name
 * without the "Behandling" suffix.
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
 *
 * Every Behandling must declare which team is responsible for it.
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
 *
 * AktivitetProcessor classes must be Spring components to be auto-discovered.
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

// --- UAST utility functions (K1 and K2 compatible) ---

private fun isBehandlingSubclass(aClass: UClass): Boolean {
    return aClass.uastSuperTypes.any { ref ->
        ref.type.presentableText == "Behandling"
    }
}

private fun isAktivitetProcessorSubclass(aClass: UClass): Boolean {
    return aClass.uastSuperTypes.any { ref ->
        ref.type.presentableText.startsWith("AktivitetProcessor")
    }
}
