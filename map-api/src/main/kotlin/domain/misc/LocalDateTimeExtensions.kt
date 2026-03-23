package cz.cvut.fit.gaierda1.domain.misc

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

fun LocalDateTime.atOffset(zoneId: ZoneId): OffsetDateTime {
    return this.atOffset(zoneId.rules.getOffset(this))
}
