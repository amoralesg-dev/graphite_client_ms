package com.rassini.graphite_client.service.mapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;

import java.io.IOException;

/**
 * Deserializador específico para {@link GraphiteSupplierDto.BankWireAbaRouting}
 * (utilizado tanto en {@code Bank_Wire_ABA_Routing} como en {@code Bank_ACH_ABA_Routing}).
 * Tolera valores nulos, strings vacíos ("") o con espacios enviados por Graphite,
 * retornando {@code null} de forma controlada sin provocar InvalidFormatException.
 */
public class BankWireAbaRoutingDeserializer extends JsonDeserializer<GraphiteSupplierDto.BankWireAbaRouting> {

    @Override
    public GraphiteSupplierDto.BankWireAbaRouting deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        JsonToken currentToken = p.currentToken();

        if (currentToken == JsonToken.VALUE_STRING) {
            return null;
        }

        if (currentToken == JsonToken.VALUE_NULL) {
            return null;
        }

        if (currentToken == JsonToken.START_OBJECT) {
            GraphiteSupplierDto.BankWireAbaRouting abaRouting = new GraphiteSupplierDto.BankWireAbaRouting();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.getCurrentName();
                p.nextToken();

                if ("bank_name".equals(fieldName)) {
                    abaRouting.setBankName(p.getText());
                } else if ("routing".equals(fieldName)) {
                    abaRouting.setRouting(p.getText());
                } else if ("swift".equals(fieldName)) {
                    abaRouting.setSwift(p.getText());
                } else if ("type".equals(fieldName)) {
                    abaRouting.setType(p.getText());
                } else if ("valid".equals(fieldName)) {
                    if (p.currentToken() == JsonToken.VALUE_TRUE) {
                        abaRouting.setValid(Boolean.TRUE);
                    } else if (p.currentToken() == JsonToken.VALUE_FALSE) {
                        abaRouting.setValid(Boolean.FALSE);
                    } else if (p.currentToken() == JsonToken.VALUE_STRING) {
                        abaRouting.setValid(Boolean.parseBoolean(p.getText()));
                    } else {
                        abaRouting.setValid(null);
                    }
                } else if ("fromDatabase".equals(fieldName)) {
                    if (p.currentToken() == JsonToken.VALUE_TRUE) {
                        abaRouting.setFromDatabase(Boolean.TRUE);
                    } else if (p.currentToken() == JsonToken.VALUE_FALSE) {
                        abaRouting.setFromDatabase(Boolean.FALSE);
                    } else if (p.currentToken() == JsonToken.VALUE_STRING) {
                        abaRouting.setFromDatabase(Boolean.parseBoolean(p.getText()));
                    } else {
                        abaRouting.setFromDatabase(null);
                    }
                } else {
                    p.skipChildren();
                }
            }
            return abaRouting;
        }

        return null;
    }
}
