package io.github.dfauth.dormant;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.github.dfauth.trycatch.TryCatch.tryCatch;

public class BinaryEncoder implements Encoder {

    public static final int MAGIC_NUMBER = 0xDECACAFE;

    private DataOutputStream out;

    BinaryEncoder(DataOutputStream out) {
        this.out = out;
    }

    @Override
    public int magicNumber() {
        return MAGIC_NUMBER;
    }

    public static byte[] serialize(Dormant dormant) {
        var baos = new ByteArrayOutputStream();
        var serde = new BinaryEncoder(new DataOutputStream(baos));
        serde.writeInt(serde.magicNumber());
        serde.writeInt(dormant.typeId());
        dormant.write(serde);
        return baos.toByteArray();
    }

    // Write methods
    @Override
    public Encoder writeInt(int value) {
        tryCatch(() -> out.writeInt(value));
        return this;
    }

    @Override
    public Encoder writeLong(long value) {
        tryCatch(() -> out.writeLong(value));
        return this;
    }

    @Override
    public Encoder writeFloat(float value) {
        tryCatch(() -> out.writeFloat(value));
        return this;
    }

    @Override
    public Encoder writeDouble(double value) {
        tryCatch(() -> out.writeDouble(value));
        return this;
    }

    @Override
    public Encoder writeBoolean(boolean value) {
        tryCatch(() -> out.writeBoolean(value));
        return this;
    }

    @Override
    public Encoder writeByte(byte value) {
        tryCatch(() -> out.writeByte(value));
        return this;
    }

    @Override
    public Encoder writeShort(short value) {
        tryCatch(() -> out.writeShort(value));
        return this;
    }

    @Override
    public Encoder writeChar(char value) {
        tryCatch(() -> out.writeChar(value));
        return this;
    }

    @Override
    public Encoder writeString(String value) {
        tryCatch(() -> {
            if (value == null) {
                out.writeInt(-1);
            } else {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
            }
        });
        return this;
    }

    @Override
    public Encoder writeBigDecimal(BigDecimal value) {
        writeBoolean(value != null);
        if (value != null) {
            writeInt(value.scale());
            byte[] unscaled = value.unscaledValue().toByteArray();
            writeInt(unscaled.length);
            tryCatch(() -> out.write(unscaled));
        }
        return this;
    }

    @Override
    public Encoder writeLocalDate(LocalDate value) {
        writeBoolean(value != null);
        if (value != null) {
            writeLong(value.toEpochDay());
        }
        return this;
    }

    @Override
    public Encoder writeInstant(Instant value) {
        writeBoolean(value != null);
        if (value != null) {
            writeLong(value.getEpochSecond());
            writeInt(value.getNano());
        }
        return this;
    }

    @Override
    public Encoder writeLocalDateTime(LocalDateTime value) {
        writeBoolean(value != null);
        if (value != null) {
            writeLong(value.toLocalDate().toEpochDay());
            writeLong(value.toLocalTime().toNanoOfDay());
        }
        return this;
    }

    @Override
    public Encoder writeBytes(byte[] value) {
        if (value == null) {
            writeInt(-1);
        } else {
            writeInt(value.length);
            tryCatch(() -> out.write(value));
        }
        return this;
    }

    @Override
    public Encoder writeEnum(Enum<?> value) {
        writeString(value != null ? value.name() : null);
        return this;
    }

    @Override
    public Encoder writeOrdinal(Enum<?> value) {
        writeInt(value != null ? value.ordinal() : -1);
        return this;
    }

    @Override
    public Encoder writeDormant(Dormant value) {
        writeBoolean(value != null);
        if (value != null) {
            writeInt(value.typeId());
            value.write(this);
        }
        return this;
    }

    @Override
    public <T> Encoder writeList(List<T> list, Writer<T> writer)
    {
        if (list == null)
        {
            writeInt(-1);
        }
        else
        {
            writeInt(list.size());
            for (T element : list)
            {
                writer.write(this, element);
            }
        }
        return this;
    }

    @Override
    public <K, V> Encoder writeMap(Map<K, V> map, Writer<K> keyWriter, Writer<V> valueWriter)
    {
        if (map == null)
        {
            writeInt(-1);
        }
        else
        {
            writeInt(map.size());
            for (Map.Entry<K, V> entry : map.entrySet())
            {
                keyWriter.write(this, entry.getKey());
                valueWriter.write(this, entry.getValue());
            }
        }
        return this;
    }

}
