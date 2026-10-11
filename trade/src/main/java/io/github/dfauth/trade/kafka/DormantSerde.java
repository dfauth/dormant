package io.github.dfauth.trade.kafka;

import io.github.dfauth.dormant.DecoderFactory;
import io.github.dfauth.dormant.Dormant;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayInputStream;

public class DormantSerde<T extends Dormant> {

    public Serializer<T> serializer() {
        return (s, t) -> t.write();
    }

    public Deserializer<T> deserializer() {
        return (s, bytes) -> DecoderFactory.create(new ByteArrayInputStream(bytes)).readDormant();
    }

}
