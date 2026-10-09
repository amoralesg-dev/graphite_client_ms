package com.rassini.graphite_client.service.mapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;

import java.io.IOException;

/**
 * Deserializador específico para {@link GraphiteSupplierDto.BankNumber}.
 * Tolera valores nulos, strings vacíos ("") o con espacios enviados por Graphite,
 * retornando {@code null} de forma controlada sin provocar InvalidFormatException.
 * Si recibe un objeto JSON válido, delega la deserialización a los campos de la clase.
 */
public class BankNumberDeserializer extends JsonDeserializer<GraphiteSupplierDto.BankNumber> {

    @Override
    public GraphiteSupplierDto.BankNumber deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        JsonToken currentToken = p.currentToken();

        if (currentToken == JsonToken.VALUE_STRING) {
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            // Si viniera un string no vacío imprevisto, se retorna null o se controla según contrato
            return null;
        }

        if (currentToken == JsonToken.VALUE_NULL) {
            return null;
        }

        if (currentToken == JsonToken.START_OBJECT) {
            // Deserializa el objeto leyendo sus campos sin recursión cíclica
            GraphiteSupplierDto.BankNumber bankNumber = new GraphiteSupplierDto.BankNumber();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.getCurrentName();
                p.nextToken(); // Avanzar al valor

                if ("bank_name".equals(fieldName)) {
                    bankNumber.setBankName(p.getText());
                } else if ("swift".equals(fieldName)) {
                    bankNumber.setSwift(p.getText());
                } else if ("routing".equals(fieldName)) {
                    bankNumber.setRouting(p.getText());
                } else if ("type".equals(fieldName)) {
                    bankNumber.setType(p.getText());
                } else if ("valid".equals(fieldName)) {
                    if (p.currentToken() == JsonToken.VALUE_TRUE) {
                        bankNumber.setValid(Boolean.TRUE);
                    } else if (p.currentToken() == JsonToken.VALUE_FALSE) {
                        bankNumber.setValid(Boolean.FALSE);
                    } else if (p.currentToken() == JsonToken.VALUE_STRING) {
                        bankNumber.setValid(Boolean.parseBoolean(p.getText()));
                    } else {
                        bankNumber.setValid(null);
                    }
                } else {
                    p.skipChildren();
                }
            }
            return bankNumber;
        }

        return null;
    }
}
