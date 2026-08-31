package com.bankapp.auth.infrastructure.persistence;

import com.bankapp.auth.domain.PasswordHash;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class PasswordHashConverter implements AttributeConverter<PasswordHash, String> {

    @Override
    public String convertToDatabaseColumn(PasswordHash hash) {
        return hash == null ? null : hash.value();
    }

    @Override
    public PasswordHash convertToEntityAttribute(String value) {
        return value == null ? null : new PasswordHash(value);
    }
}
