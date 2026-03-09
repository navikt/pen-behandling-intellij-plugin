package no.nav.pensjon.pen.plugin.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames

/**
 * Warns when @DiscriminatorValue on a Behandling subclass ends with "Behandling".
 *
 * Per the PEN conventions, the discriminator value should be the behandling name
 * without the "Behandling" suffix.
 */
class DiscriminatorValueInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)

                if (!isBehandlingSubclass(klass)) return

                val discriminatorAnnotation = klass.annotationEntries.firstOrNull { annotation ->
                    annotation.shortName?.asString() == "DiscriminatorValue"
                } ?: return

                val value = extractStringArgument(discriminatorAnnotation) ?: return

                if (value.endsWith("Behandling")) {
                    holder.registerProblem(
                        discriminatorAnnotation,
                        "DiscriminatorValue '$value' should not end with 'Behandling'. " +
                                "Suggested: '${value.removeSuffix("Behandling")}'"
                    )
                }
            }
        }
    }
}

/**
 * Warns when a Behandling subclass is missing @ForvalgtAnsvarligTeam.
 *
 * Every Behandling must declare which team is responsible for it.
 */
class MissingForvalgtAnsvarligTeamInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)

                if (!isBehandlingSubclass(klass)) return

                val hasAnnotation = klass.annotationEntries.any { annotation ->
                    annotation.shortName?.asString() == "ForvalgtAnsvarligTeam"
                }

                if (!hasAnnotation) {
                    val nameIdentifier = klass.nameIdentifier ?: return
                    holder.registerProblem(
                        nameIdentifier,
                        "Behandling subclass '${klass.name}' is missing @ForvalgtAnsvarligTeam annotation. " +
                                "Add @ForvalgtAnsvarligTeam(PESYS_FELLES), @ForvalgtAnsvarligTeam(PESYS_ALDER), " +
                                "or @ForvalgtAnsvarligTeam(PESYS_UFORE)."
                    )
                }
            }
        }
    }
}

/**
 * Warns when an AktivitetProcessor subclass is missing @Component.
 *
 * AktivitetProcessor classes must be Spring components to be auto-discovered.
 */
class MissingComponentInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)

                if (!isAktivitetProcessorSubclass(klass)) return

                val hasComponent = klass.annotationEntries.any { annotation ->
                    val name = annotation.shortName?.asString()
                    name == "Component" || name == "Service"
                }

                if (!hasComponent) {
                    val nameIdentifier = klass.nameIdentifier ?: return
                    holder.registerProblem(
                        nameIdentifier,
                        "AktivitetProcessor subclass '${klass.name}' is missing @Component annotation. " +
                                "Add @Component so Spring can discover it."
                    )
                }
            }
        }
    }
}

// --- Utility functions ---

private fun isBehandlingSubclass(klass: KtClass): Boolean {
    val superNames = klass.getSuperNames()
    return superNames.any { it == "Behandling" }
}

private fun isAktivitetProcessorSubclass(klass: KtClass): Boolean {
    val superTypeList = klass.superTypeListEntries
    return superTypeList.any { entry ->
        val typeRef = entry.typeReference?.text ?: ""
        typeRef.startsWith("AktivitetProcessor")
    }
}

private fun extractStringArgument(annotation: KtAnnotationEntry): String? {
    val args = annotation.valueArguments
    if (args.isEmpty()) return null

    val expr = args.first().getArgumentExpression()
    if (expr is KtStringTemplateExpression) {
        val entries = expr.entries
        if (entries.size == 1 && entries[0] is KtLiteralStringTemplateEntry) {
            return (entries[0] as KtLiteralStringTemplateEntry).text
        }
    }
    // Handle named argument: value = "..."
    return expr?.text?.removeSurrounding("\"")
}
