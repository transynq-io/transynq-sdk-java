package io.transynq.sdk.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** The SDK's Jackson configuration (docs/design.md section 11). */
public final class Json {
    private static final ObjectMapper MAPPER = create();

    private Json() {
    }

    /** @return the shared, configured mapper */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * Formats an instant the way the API expects: UTC with {@code Z}.
     *
     * @param value the instant
     * @return ISO-8601 text
     */
    public static String formatInstant(OffsetDateTime value) {
        OffsetDateTime utc = value.withOffsetSameInstant(ZoneOffset.UTC);
        String text = utc.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        int nanos = utc.getNano();
        if (nanos != 0) {
            String fraction = String.format("%09d", nanos).replaceAll("0+$", "");
            text += "." + fraction;
        }
        return text + "Z";
    }

    /**
     * Parses an instant; text without an offset is UTC.
     *
     * @param text ISO-8601 text
     * @return the instant in UTC
     */
    public static OffsetDateTime parseInstant(String text) {
        try {
            return OffsetDateTime.parse(text).withOffsetSameInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(text).atOffset(ZoneOffset.UTC);
        }
    }

    private static ObjectMapper create() {
        SimpleModule instants = new SimpleModule("transynq-instants");
        instants.addSerializer(OffsetDateTime.class, new JsonSerializer<OffsetDateTime>() {
            @Override
            public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(formatInstant(value));
            }
        });
        instants.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return parseInstant(p.getValueAsString());
            }
        });
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(instants)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
