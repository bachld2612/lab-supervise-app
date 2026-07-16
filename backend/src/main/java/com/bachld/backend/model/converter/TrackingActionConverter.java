package com.bachld.backend.model.converter;

import com.bachld.backend.util.enums.TrackingAction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TrackingActionConverter implements AttributeConverter<TrackingAction, Integer> {

  @Override
  public Integer convertToDatabaseColumn(TrackingAction attribute) {
    return attribute == null ? TrackingAction.NORMAL.getValue() : attribute.getValue();
  }

  @Override
  public TrackingAction convertToEntityAttribute(Integer dbData) {
    return TrackingAction.fromValue(dbData);
  }
}
