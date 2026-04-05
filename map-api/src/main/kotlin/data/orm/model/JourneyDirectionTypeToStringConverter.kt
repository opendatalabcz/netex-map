package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class JourneyDirectionTypeToStringConverter: AttributeConverter<JourneyDirectionType, String> {
    override fun convertToDatabaseColumn(attribute: JourneyDirectionType?): String? =
        attribute?.shorCode

    override fun convertToEntityAttribute(dbData: String?): JourneyDirectionType? =
        dbData?.let(JourneyDirectionType::fromShortCode)
}
