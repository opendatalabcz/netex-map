package cz.cvut.fit.gaierda1.data.orm.model

enum class LineType(val shortCode: String) {
    URBAN("A"),
    URBAN_SUBURBAN("B"),
    INTERNATIONAL_EXCLUDING_CABOTAGE("N"),
    INTERNATIONAL_INCLUDING_CABOTAGE("P"),
    DOMESTIC_INTRA_REGIONAL("V"),
    DOMESTIC_INTER_REGIONAL("Z"),
    DOMESTIC_LONG_DISTANCE("D");

    companion object {
        fun fromJdfCode(code: String): LineType = when (code.uppercase()) {
            "A" -> URBAN
            "B" -> URBAN_SUBURBAN
            "N" -> INTERNATIONAL_EXCLUDING_CABOTAGE
            "P" -> INTERNATIONAL_INCLUDING_CABOTAGE
            "V" -> DOMESTIC_INTRA_REGIONAL
            "Z" -> DOMESTIC_INTER_REGIONAL
            "D" -> DOMESTIC_LONG_DISTANCE
            else -> throw IllegalArgumentException("Unknown line type JDF code: $code")
        }
    }
}
