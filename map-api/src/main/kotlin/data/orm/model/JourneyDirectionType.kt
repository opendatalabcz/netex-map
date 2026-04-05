package cz.cvut.fit.gaierda1.data.orm.model

enum class JourneyDirectionType(val shorCode: String) {
    OUTBOUND("O"),
    INBOUND("I"),
    CLOCKWISE("C"),
    ANTICLOCKWISE("A");

    companion object {
        fun fromShortCode(code: String): JourneyDirectionType = when (code.uppercase()) {
            "O" -> OUTBOUND
            "I" -> INBOUND
            "C" -> CLOCKWISE
            "A" -> ANTICLOCKWISE
            else -> throw IllegalArgumentException("Invalid journey direction type short code: $code")
        }
    }
}
