package io.github.dfauth.dormant;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static io.github.dfauth.dormant.BinaryEncoder.MAGIC_NUMBER;
import static io.github.dfauth.trycatch.TryCatch.tryCatch;
import static java.util.Optional.empty;

@Slf4j
public class BinaryDecoder implements Decoder {

    private DataInputStream in;

    private final Map<Integer, Supplier<Dormant>> factories = new ConcurrentHashMap<>();

    public BinaryDecoder(String... basePackages) {
        var classGraph = new ClassGraph()
                .enableClassInfo();
        if (basePackages.length > 0) {
            classGraph.acceptPackages(basePackages);
        }
        try (ScanResult scanResult = classGraph.scan()) {
            for (ClassInfo classInfo : scanResult.getClassesImplementing(Dormant.class)) {
                if (classInfo.isAbstract() || classInfo.isInterface()) {
                    continue;
                }
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends Dormant> clazz = (Class<? extends Dormant>) classInfo.loadClass();
                    register(clazz);
                }
                catch (Exception e) {
                    log.warn("Could not register Dormant class {}: {}", classInfo.getName(), e.getMessage());
                }
            }
        }
    }

    BinaryDecoder(DataInputStream in) {
        this();
        this.in = in;
        int magic;
        if ((magic = tryCatch(in::readInt)) != MAGIC_NUMBER) {
            throw new IllegalArgumentException("Invalid magic number: 0x" + Integer.toHexString(magic));
        }
    }

    public void register(Class<? extends Dormant> clazz) {
        if (Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
            throw new IllegalArgumentException("Cannot register abstract class or interface: " + clazz.getName());
        }
        Constructor<? extends Dormant> ctor;
        try {
            ctor = clazz.getDeclaredConstructor();
        }
        catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " has no no-arg constructor");
        }
        ctor.setAccessible(true);
        Constructor<? extends Dormant> finalCtor = ctor;
        Dormant sample = tryCatch((Callable<Dormant>) finalCtor::newInstance);
        int typeId = sample.typeId();
        Supplier<Dormant> factory = () -> tryCatch((Callable<Dormant>) finalCtor::newInstance);
        Supplier<Dormant> existing = factories.putIfAbsent(typeId, factory);
        if (existing != null) {
            String existingClass = existing.get().getClass().getName();
            if (!existingClass.equals(clazz.getName())) {
                log.warn("TypeId collision: {} and {} both map to typeId {}", existingClass, clazz.getName(), typeId);
            }
        }
    }

    public Optional<Dormant> create(int typeId) {
        if(typeId == -1) {
            return empty();
        }
        Supplier<Dormant> factory = factories.get(typeId);
        if (factory == null) {
            throw new IllegalArgumentException("No Dormant registered for typeId: " + typeId);
        }
        return Optional.ofNullable(factory.get());
    }

    public static int peekTypeId(byte[] data) {
        var serde = new BinaryDecoder(new DataInputStream(new ByteArrayInputStream(data)));
        int magic = serde.readInt();
        if (magic != MAGIC_NUMBER) {
            throw new IllegalArgumentException("Invalid magic number: 0x" + Integer.toHexString(magic));
        }
        return serde.readInt();
    }

    public static <T extends Dormant> T deserialize(byte[] data) {
        var decoder = new BinaryDecoder(new DataInputStream(new ByteArrayInputStream(data)));
        return decoder.readDormant();
    }

    // Read methods
    @Override
    public int readInt() {
        return tryCatch(() -> in.readInt());
    }

    @Override
    public long readLong() {
        return tryCatch(() -> in.readLong());
    }

    @Override
    public float readFloat() {
        return tryCatch(() -> in.readFloat());
    }

    @Override
    public double readDouble() {
        return tryCatch(() -> in.readDouble());
    }

    @Override
    public boolean readBoolean() {
        return tryCatch(() -> in.readBoolean());
    }

    @Override
    public byte readByte() {
        return tryCatch(() -> in.readByte());
    }

    @Override
    public short readShort() {
        return tryCatch(() -> in.readShort());
    }

    @Override
    public char readChar() {
        return tryCatch(() -> in.readChar());
    }

    @Override
    public String readString() {
        return tryCatch(() -> {
            int len = in.readInt();
            if (len == -1) return null;
            byte[] bytes = new byte[len];
            in.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        });
    }

    @Override
    public BigDecimal readBigDecimal() {
        if (readBoolean()) {
            int scale = readInt();
            int len = readInt();
            byte[] unscaled = new byte[len];
            tryCatch(() -> in.readFully(unscaled));
            return new BigDecimal(new BigInteger(unscaled), scale);
        }
        return null;
    }

    @Override
    public LocalDate readLocalDate() {
        if (readBoolean()) {
            return LocalDate.ofEpochDay(readLong());
        }
        return null;
    }

    @Override
    public Instant readInstant() {
        if (readBoolean()) {
            long epochSecond = readLong();
            int nano = readInt();
            return Instant.ofEpochSecond(epochSecond, nano);
        }
        return null;
    }

    @Override
    public LocalDateTime readLocalDateTime() {
        if (readBoolean()) {
            LocalDate date = LocalDate.ofEpochDay(readLong());
            LocalTime time = LocalTime.ofNanoOfDay(readLong());
            return LocalDateTime.of(date, time);
        }
        return null;
    }

    @Override
    public byte[] readBytes() {
        int len = readInt();
        if (len == -1) return null;
        byte[] bytes = new byte[len];
        tryCatch(() -> in.readFully(bytes));
        return bytes;
    }

    @Override
    public <E extends Enum<E>> E readEnum(Class<E> enumClass) {
        String name = readString();
        if (name == null) return null;
        return Enum.valueOf(enumClass, name);
    }

    @Override
    public <E extends Enum<E>> E readOrdinal(Class<E> enumClass) {
        int ordinal = readInt();
        if (ordinal == -1) return null;
        return enumClass.getEnumConstants()[ordinal];
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Dormant> T readDormant() {
        int typeId = readInt();
        return create(typeId).map(instance -> {
            instance.read(this);
            return (T) instance;
        }).orElse(null);
    }

    @Override
    public <T> List<T> readList(Reader<T> reader)
    {
        int size = readInt();
        if (size == -1)
        {
            return null;
        }
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            list.add(reader.read(this));
        }
        return list;
    }

    @Override
    public <K, V> Map<K, V> readMap(Reader<K> keyReader, Reader<V> valueReader)
    {
        int size = readInt();
        if (size == -1)
        {
            return null;
        }
        Map<K, V> map = new HashMap<>(size);
        for (int i = 0; i < size; i++)
        {
            K key = keyReader.read(this);
            V value = valueReader.read(this);
            map.put(key, value);
        }
        return map;
    }

}
