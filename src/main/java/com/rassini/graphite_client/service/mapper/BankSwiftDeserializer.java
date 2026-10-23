package com.rassini.graphite_client.service.mapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;

import java.io.IOException;

/**
 * Deserializador específico para {@link GraphiteSupplierDto.BankSwift}.
 * Tolera valores nulos, strings vacíos ("") o con espacios enviados por Graphite,
 * retornando {@code null} de forma controlada sin provocar InvalidFormatException.
 */
public class BankSwiftDeserializer extends JsonDeserializer<GraphiteSupplierDto.BankSwift> {

    @Override
    public GraphiteSupplierDto.BankSwift deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        JsonToken currentToken = p.currentToken();

        if (currentToken == JsonToken.VALUE_STRING) {
            return null;
        }

        if (currentToken == JsonToken.VALUE_NULL) {
            return null;
        }

        if (currentToken == JsonToken.START_OBJECT) {
            GraphiteSupplierDto.BankSwift bankSwift = new GraphiteSupplierDto.BankSwift();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.getCurrentName();
                p.nextToken();

                if ("routing".equals(fieldName)) {
                    bankSwift.setRouting(p.getText());
                } else if ("valid".equals(fieldName)) {
                    if (p.currentToken() == JsonToken.VALUE_TRUE) {
                        bankSwift.setValid(Boolean.TRUE);
                    } else if (p.currentToken() == JsonToken.VALUE_FALSE) {
                        bankSwift.setValid(Boolean.FALSE);
                    } else if (p.currentToken() == JsonToken.VALUE_STRING) {
                        bankSwift.setValid(Boolean.parseBoolean(p.getText()));
                    } else {
                        bankSwift.setValid(null);
                    }
                } else {
                    p.skipChildren();
                }
            }
            return bankSwift;
        }

        return null;
    }
}
