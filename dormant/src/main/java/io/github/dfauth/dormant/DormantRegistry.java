package io.github.dfauth.dormant;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static io.github.dfauth.trycatch.TryCatch.tryCatch;

public class DormantRegistry {

    private static final Logger log = LoggerFactory.getLogger(DormantRegistry.class);

    private final Map<Integer, Supplier<Dormant>> factories = new ConcurrentHashMap<>();

    public DormantRegistry(String... basePackages) {
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

    Dormant create(int typeId) {
        Supplier<Dormant> factory = factories.get(typeId);
        if (factory == null) {
            throw new IllegalArgumentException("No Dormant registered for typeId: " + typeId);
        }
        return factory.get();
    }

    @SuppressWarnings("unchecked")
    public <T extends Dormant> T deserialize(byte[] data) {
        var serde = new BinarySerde(new DataInputStream(new ByteArrayInputStream(data)))
                .withRegistry(this);
        int magic = serde.readInt();
        if (magic != BinarySerde.MAGIC_NUMBER) {
            throw new IllegalArgumentException("Invalid magic number: 0x" + Integer.toHexString(magic));
        }
        int typeId = serde.readInt();
        Dormant instance = create(typeId);
        instance.read(serde);
        return (T) instance;
    }
}
