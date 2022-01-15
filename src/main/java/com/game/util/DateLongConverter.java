package com.game.util;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Date;

@Converter
public class DateLongConverter implements AttributeConverter<Long, Date> {
    @Override
    public Date convertToDatabaseColumn(Long attribute) {
        return new Date(attribute);
    }

    @Override
    public Long convertToEntityAttribute(Date dbData) {
        return dbData.getTime();
    }
}
