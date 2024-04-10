package ibb.api.phenotypeservice.helper;

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonFactory;

import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.jackson.JacksonJsonpGenerator;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

public class ESHelper {
    public static String serializeToJSON(JsonpSerializable object) throws IOException {
        StringWriter writer = new StringWriter();
        try (final JacksonJsonpGenerator generator = new JacksonJsonpGenerator(new JsonFactory().createGenerator(writer))) {
            object.serialize(generator, new JacksonJsonpMapper());
        }
        return writer.toString();
    }
}
