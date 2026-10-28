package com.rassini.graphite_client.service.mapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;

import java.io.IOException;

/**
 * Deserializador específico para {@link GraphiteSupplierDto.BankAccountIban}
 * ({@code Bank_Account_IBAN} en JSON).
 * Tolera valores nulos, strings vacíos (""), strings no vacíos (capturados como routing)
 * u objetos JSON con routing, valid, fromDatabase, bank_name, swift y type.
 */
public class BankAccountIbanDeserializer extends JsonDeserializer<GraphiteSupplierDto.BankAccountIban> {

    @Override
    public GraphiteSupplierDto.BankAccountIban deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        JsonToken currentToken = p.currentToken();

        if (currentToken == JsonToken.VALUE_NULL) {
            return null;
        }

        if (currentToken == JsonToken.VALUE_STRING) {
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            GraphiteSupplierDto.BankAccountIban iban = new GraphiteSupplierDto.BankAccountIban();
            iban.setRouting(text);
            iban.setType("IBAN");
            iban.setValid(Boolean.TRUE);
            return iban;
        }

        if (currentToken == JsonToken.START_OBJECT) {
            GraphiteSupplierDto.BankAccountIban iban = new GraphiteSupplierDto.BankAccountIban();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.getCurrentName();
                p.nextToken();

                if ("bank_name".equals(fieldName)) {
                    iban.setBankName(p.getText());
                } else if ("routing".equals(fieldName)) {
                    iban.setRouting(p.getText());
                } else if ("swift".equals(fieldName)) {
                    iban.setSwift(p.getText());
                } else if ("type".equals(fieldName)) {
                    iban.setType(p.getText());
                } else if ("valid".equals(fieldName)) {
                    if (p.currentToken() == JsonToken.VALUE_TRUE) {
                        iban.setValid(Boolean.TRUE);
                    } else if (p.currentToken() == JsonToken.VALUE_FALSE) {
                        iban.setValid(Boolean.FALSE);
                    } else if (p.currentToken() == JsonToken.VALUE_STRING) {
                        iban.setValid(Boolean.parseBoolean(p.getText()));
                    } else {
                        iban.setValid(null);
                    }
                } else if ("fromDatabase".equals(fieldName)) {
                    if (p.currentToken() == JsonToken.VALUE_TRUE) {
                        iban.setFromDatabase(Boolean.TRUE);
                    } else if (p.currentToken() == JsonToken.VALUE_FALSE) {
                        iban.setFromDatabase(Boolean.FALSE);
                    } else if (p.currentToken() == JsonToken.VALUE_STRING) {
                        iban.setFromDatabase(Boolean.parseBoolean(p.getText()));
                    } else {
                        iban.setFromDatabase(null);
                    }
                } else {
                    p.skipChildren();
                }
            }
            return iban;
        }

        return null;
    }
}
