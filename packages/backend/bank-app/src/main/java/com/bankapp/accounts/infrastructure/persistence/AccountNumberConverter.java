package com.bankapp.accounts.infrastructure.persistence;

import com.bankapp.accounts.domain.AccountNumber;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class AccountNumberConverter
    implements AttributeConverter<AccountNumber, String>
{

    @Override
    public String convertToDatabaseColumn(AccountNumber number) {
        return number == null ? null : number.value();
    }

    @Override
    public AccountNumber convertToEntityAttribute(String value) {
        return value == null ? null : new AccountNumber(value);
    }
}
