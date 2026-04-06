package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class TransportModeToStringConverter: AttributeConverter<TransportMode, String> {
    override fun convertToDatabaseColumn(attribute: TransportMode?): String? =
        attribute?.shortCode

    override fun convertToEntityAttribute(dbData: String?): TransportMode? =
        dbData?.let(TransportMode::fromShortCode)
}