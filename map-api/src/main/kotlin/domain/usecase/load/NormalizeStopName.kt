package cz.cvut.fit.gaierda1.domain.usecase.load

import org.springframework.stereotype.Component

@Component
class NormalizeStopName: NormalizeStopNameUseCase {
    private fun String.collapseCommas(): String = replace(Regex("""\s*,(?:\s*,)*\s*"""), ",")
    private fun String.removeSquareBrackets(): String = replace(Regex("""\s*\[[^]]*]"""), "")
    private fun String.addSpaceAfterDots(): String = replace(Regex("""\.(?:\s+|([^,]))"""), ". $1")
    private fun String.replaceUnbreakableSpaces(): String = replace("\u00A0", " ")
    private fun String.removeTrailingComma(): String = if (endsWith(",")) substring(0, length - 1) else this
    private fun String.removeXInCircle(): String = replace(Regex("""Ⓧ\s*"""), "")

    override fun normalize(stopName: String): String {
        return stopName
            .collapseCommas()
            .removeSquareBrackets()
            .addSpaceAfterDots()
            .replaceUnbreakableSpaces()
            .removeXInCircle()
            .removeTrailingComma()
    }
}
