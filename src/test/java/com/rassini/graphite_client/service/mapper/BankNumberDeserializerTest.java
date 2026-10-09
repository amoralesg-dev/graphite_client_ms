package com.rassini.graphite_client.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BankNumberDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Debe deserializar Bank_Number como null cuando es un string vacío \"\" (Caso US197697)")
    void testDeserializeEmptyStringBankNumber() throws Exception {
        String json = """
            {
               "Entity_Public_Id": "US197697",
               "ERP_Record": [
                 {
                   "RASSINI_ERP_Entity_ID": "0111",
                   "ERP_Bank_List": [
                     {
                       "Bank_Account_Number": "123456789",
                       "Bank_Number": ""
                     }
                   ]
                 }
               ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);

        assertNotNull(dto);
        assertNotNull(dto.getErpRecords());
        assertEquals(1, dto.getErpRecords().size());
        GraphiteSupplierDto.ErpRecord erp = dto.getErpRecords().get(0);
        assertNotNull(erp.getErpBankList());
        assertEquals(1, erp.getErpBankList().size());
        GraphiteSupplierDto.Bank bank = erp.getErpBankList().get(0);
        assertEquals("123456789", bank.getBankAccountNumber());
        assertNull(bank.getBankNumber(), "Bank_Number con string vacío debe deserializarse a null");
    }

    @Test
    @DisplayName("Debe deserializar Bank_Number como null cuando es un string con solo espacios")
    void testDeserializeWhitespaceBankNumber() throws Exception {
        String json = """
            {
               "Entity_Public_Id": "US197697",
               "ERP_Record": [
                 {
                   "RASSINI_ERP_Entity_ID": "0111",
                   "ERP_Bank_List": [
                     {
                       "Bank_Account_Number": "123456789",
                       "Bank_Number": "    "
                     }
                   ]
                 }
               ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);

        assertNotNull(dto);
        GraphiteSupplierDto.Bank bank = dto.getErpRecords().get(0).getErpBankList().get(0);
        assertNull(bank.getBankNumber(), "Bank_Number con espacios en blanco debe deserializarse a null");
    }

    @Test
    @DisplayName("Debe deserializar Bank_Number como null cuando es explícitamente null")
    void testDeserializeNullBankNumber() throws Exception {
        String json = """
            {
               "Entity_Public_Id": "US197697",
               "ERP_Record": [
                 {
                   "RASSINI_ERP_Entity_ID": "0111",
                   "ERP_Bank_List": [
                     {
                       "Bank_Account_Number": "123456789",
                       "Bank_Number": null
                     }
                   ]
                 }
               ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);

        assertNotNull(dto);
        GraphiteSupplierDto.Bank bank = dto.getErpRecords().get(0).getErpBankList().get(0);
        assertNull(bank.getBankNumber());
    }

    @Test
    @DisplayName("Debe deserializar Bank_Number correctamente cuando viene como un objeto completo")
    void testDeserializeValidObjectBankNumber() throws Exception {
        String json = """
            {
               "Entity_Public_Id": "US197697",
               "ERP_Record": [
                 {
                   "RASSINI_ERP_Entity_ID": "0111",
                   "ERP_Bank_List": [
                     {
                       "Bank_Account_Number": "123456789",
                       "Bank_Number": {
                         "bank_name": "JPMORGAN CHASE BANK",
                         "routing": "021000021",
                         "swift": "CHASUS33",
                         "type": "Checking",
                         "valid": true
                       }
                     }
                   ]
                 }
               ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);

        assertNotNull(dto);
        GraphiteSupplierDto.Bank bank = dto.getErpRecords().get(0).getErpBankList().get(0);
        assertNotNull(bank.getBankNumber());
        assertEquals("JPMORGAN CHASE BANK", bank.getBankNumber().getBankName());
        assertEquals("021000021", bank.getBankNumber().getRouting());
        assertEquals("CHASUS33", bank.getBankNumber().getSwift());
        assertEquals("Checking", bank.getBankNumber().getType());
        assertEquals(Boolean.TRUE, bank.getBankNumber().getValid());
    }
}
