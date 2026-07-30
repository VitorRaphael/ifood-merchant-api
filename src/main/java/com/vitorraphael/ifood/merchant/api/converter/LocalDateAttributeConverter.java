package com.vitorraphael.ifood.merchant.api.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate localDate) {
        return localDate == null ? null : localDate.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String value) {
        return value == null ? null : LocalDate.parse(value);
    }
}