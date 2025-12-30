package cz.cvut.fit.gaierda1.data.orm

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.ZoneId

@Converter(autoApply = true)
class ZoneIdToStringConverter : AttributeConverter<ZoneId, String> {
    override fun convertToDatabaseColumn(attribute: ZoneId?): String? =
        attribute?.id

    override fun convertToEntityAttribute(dbData: String?): ZoneId? =
        dbData?.let(ZoneId::of)
}
