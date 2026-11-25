package io.github.dfauth.dormant.authzn;

import io.github.dfauth.trycatch.Lists;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class ResourceHierarchy {

    private final TreeMap<String, Directive.Node> resources = new TreeMap<>();

    public List<Directive> getDirectives(Resource resource) {
        return getDirectives(new Directive.Node(resources, Collections.emptyList()), resource.getPath());
    }

    private List<Directive> getDirectives(Directive.Node node, Iterator<String> path) {
        if(path.hasNext()) {
            String element = path.next();
            TreeMap<String, Directive.Node> children = node.children();
            return Lists.append(Optional.ofNullable(children.get(element))
                    .map(childNode -> getDirectives(childNode, path))
                    .orElseGet(() -> Optional.ofNullable(children.floorEntry(element))
                            .map(Map.Entry::getValue)
                            .map(Directive.Node::directives)
                            .orElse(Collections.emptyList())), node.directives());
        }
        return node.directives();
    }

    public void addDirective(Directive directive) {
        addDirective(directive, new Directive.Node(resources, Collections.emptyList()), directive.resource().getPath());
    }

    private void addDirective(Directive directive, Directive.Node node, Iterator<String> path) {
        if(path.hasNext()) {
            String element = path.next();
            TreeMap<String, Directive.Node> children = node.children();
            Optional.ofNullable(children.get(element))
                    .ifPresentOrElse(childNode -> addDirective(directive, childNode, path),
                    () -> Optional.ofNullable(children.floorEntry(element))
                            .map(Map.Entry::getValue)
                            .ifPresentOrElse(
                                    n -> addDirective(directive, n, path),
                                    () -> {
                                        Directive.Node n = children.computeIfAbsent(element, k -> new Directive.Node());
                                        addDirective(directive, n, path);
                                    }
                            )
                    );
        } else {
            node.directives().add(directive);
        }
    }
}
