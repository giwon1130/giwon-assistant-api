package com.giwon.assistant.common

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class ListStringJsonConverter(
    private val objectMapper: ObjectMapper = ObjectMapper(),
) : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>?): String =
        objectMapper.writeValueAsString(attribute ?: emptyList<String>())

    override fun convertToEntityAttribute(dbData: String?): List<String> =
        if (dbData.isNullOrBlank()) {
            emptyList()
        } else {
            objectMapper.readValue(dbData, object : TypeReference<List<String>>() {})
        }
}
