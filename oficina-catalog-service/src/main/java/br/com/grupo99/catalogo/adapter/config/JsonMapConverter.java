package br.com.grupo99.catalogo.adapter.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

/**
 * Conversor customizado para Map<String, Object> ↔ DynamoDB String (JSON).
 *
 * Necessário porque DynamoDB Enhanced Client não suporta Map<String, Object>
 * nativamente.
 * O mapa é serializado como JSON string no DynamoDB.
 */
public class JsonMapConverter implements AttributeConverter<Map<String, Object>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public AttributeValue transformFrom(Map<String, Object> input) {
        if (input == null) {
            return AttributeValue.builder().nul(true).build();
        }
        try {
            return AttributeValue.builder().s(MAPPER.writeValueAsString(input)).build();
        } catch (JsonProcessingException e) {
            return AttributeValue.builder().nul(true).build();
        }
    }

    @Override
    public Map<String, Object> transformTo(AttributeValue input) {
        if (input == null || Boolean.TRUE.equals(input.nul()) || input.s() == null) {
            return null;
        }
        try {
            return MAPPER.readValue(input.s(), new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public EnhancedType<Map<String, Object>> type() {
        return EnhancedType.mapOf(String.class, Object.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
