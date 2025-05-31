package practicum.intershopreactive.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ActionTypeConverter implements Converter<String, ActionType> {
    @Override
    public ActionType convert(String source) {
        try {
            return ActionType.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid action type: " + source);
        }
    }
}

