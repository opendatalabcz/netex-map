package cz.cvut.fit.gaierda1.data.orm

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import kotlin.collections.emptyMap

@Converter(autoApply = true)
class MapToJsonConverter : AttributeConverter<Map<String, String>, String> {
    private val objectMapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, String>?): String =
        objectMapper.writeValueAsString(attribute ?: emptyMap<String, String>())

    override fun convertToEntityAttribute(dbData: String?): Map<String, String> =
        dbData?.let { objectMapper.readValue(it) } ?: emptyMap()
}
