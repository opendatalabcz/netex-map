package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class LineTypeToStringConverter: AttributeConverter<LineType, String> {
    override fun convertToDatabaseColumn(attribute: LineType?): String? =
        attribute?.shortCode

    override fun convertToEntityAttribute(dbData: String?): LineType? =
        dbData?.let(LineType::fromJdfCode)
}
