package io.github.dfauth.dormant.authzn;

import lombok.NonNull;
import org.junit.platform.commons.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.stream;
import static java.util.function.Predicate.not;

public interface Resource {

    static String format(Resource resource) {
        return resource.type() + ":" + StreamSupport.stream(Spliterators.spliteratorUnknownSize(resource.getPath(), 0), false).collect(Collectors.joining("/"));
    }

    static Resource parse(String str) {
        return Optional.of(str.split(":"))
                .filter(arr -> arr.length > 1)
                .map(arr -> new ResourceRecord(arr[0],
                        stream(arr[1].split("/"))
                                .filter(not(StringUtils::isBlank))
                                .toList())
                )
                .orElseThrow();
    }

    String type();

    Iterator<String> getPath();

    record ResourceRecord(String type, List<String> path) implements Resource {

        @Override
        public Iterator<String> getPath() {
            return path.iterator();
        }

        @NonNull
        @Override
        public String toString() {
            return format(this);
        }
    }
}
