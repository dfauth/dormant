package io.github.dfauth.trade.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.github.dfauth.trade.model.Tenor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TenorDeserializer extends JsonDeserializer<Tenor> implements Converter<String, Tenor> {

    @Override
    public Tenor deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        return Tenor.parse(p.getText());
    }

    @Override
    public Tenor convert(String source) {
        return Tenor.parse(source);
    }
}
