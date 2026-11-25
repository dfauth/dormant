package io.github.dfauth.dormant.authzn;

import io.github.dfauth.dormant.Decoder;
import io.github.dfauth.dormant.Dormant;
import io.github.dfauth.dormant.Encoder;
import io.github.dfauth.trycatch.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public record Directive(Resource resource, Action action, Principal principal, AuthorizationDecision decision) implements Dormant {

    public Directive(Resource resource, Action action, Principal principal) {
        this(resource, action, principal,AuthorizationDecision.ALLOW);
    }

    @Override
    public void write(Encoder encoder) {
        encoder.writeString(resource.toString())
                .writeString(action.toString())
                .writeString(principal.toString())
                .writeEnum(decision);
    }

    @Override
    public void read(Decoder decoder) {
    }

    public record Node(TreeMap<String, Directive.Node> children, List<Directive> directives) {
        public Node(List<Directive> directives) {
            this(new TreeMap<>(), directives);
        }
        public Node(Directive directive) {
            this(Lists.modifiableList(directive));
        }
        public Node(String element) {
            this();
            children.put(element, new Node());
        }
        public Node() {
            this(new ArrayList<>());
        }
    }
}
