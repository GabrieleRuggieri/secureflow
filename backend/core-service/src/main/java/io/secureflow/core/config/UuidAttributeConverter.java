/*
 * UuidAttributeConverter — Converte UUID ↔ byte[] per MySQL BINARY(16).
 *
 * Lo schema usa BINARY(16) per storage efficiente (16 byte vs 36 char per UUID string).
 * Big-endian: most significant bits first. autoApply=true lo applica a tutti i campi
 * UUID nelle entity, producendo byte[] per la colonna.
 */
package io.secureflow.core.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Converts UUID to/from MySQL BINARY(16) for efficient storage.
 * Uses big-endian byte order (most significant bits first).
 */
@Converter(autoApply = true)
public class UuidAttributeConverter implements AttributeConverter<UUID, byte[]> {

    /**
     * UUID → byte[16] per MySQL BINARY(16). UUID = 128 bit = 2 long. getMostSignificantBits
     * e getLeastSignificantBits danno i 16 byte in big-endian. MySQL BINARY(16) accetta
     * byte[] direttamente.
     */
    @Override
    public byte[] convertToDatabaseColumn(UUID uuid) {
        if (uuid == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    /**
     * byte[16] → UUID. Legge due long in ordine e ricostruisce l'UUID. Se bytes è null
     * o lunghezza != 16 (corruzione), ritorna null.
     */
    @Override
    public UUID convertToEntityAttribute(byte[] bytes) {
        if (bytes == null || bytes.length != 16) return null;
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }
}
