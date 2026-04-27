package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class TransportModeListToStringConverter: AttributeConverter<List<TransportMode>, String?> {
    override fun convertToDatabaseColumn(attribute: List<TransportMode>): String? =
        if (attribute.isEmpty()) null else attribute.joinToString(separator = ",") { it.shortCode }

    override fun convertToEntityAttribute(dbData: String?): List<TransportMode> =
        if (dbData == null) emptyList() else dbData.split(",").map(TransportMode::fromShortCode)
}
