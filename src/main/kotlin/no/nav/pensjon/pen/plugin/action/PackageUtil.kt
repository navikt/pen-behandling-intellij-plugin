package no.nav.pensjon.pen.plugin.action

import com.intellij.psi.PsiDirectory

object PackageUtil {

    private val SOURCE_ROOT_MARKERS = listOf(
        "/src/main/java/",
        "/src/main/kotlin/",
        "/src/test/java/",
        "/src/test/kotlin/",
    )

    fun getPackageName(directory: PsiDirectory): String {
        val path = directory.virtualFile.path
        for (marker in SOURCE_ROOT_MARKERS) {
            val index = path.indexOf(marker)
            if (index >= 0) {
                return path.substring(index + marker.length).replace('/', '.')
            }
        }
        return ""
    }
}
