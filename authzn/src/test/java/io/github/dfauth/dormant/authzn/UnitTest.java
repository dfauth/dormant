package io.github.dfauth.dormant.authzn;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static io.github.dfauth.dormant.authzn.Action.CRUDActions.UPDATE;
import static io.github.dfauth.dormant.authzn.AuthorizationDecision.ALLOW;
import static io.github.dfauth.dormant.authzn.AuthorizationDecision.DENY;
import static io.github.dfauth.dormant.authzn.Policies.DENY_ALL;
import static io.github.dfauth.dormant.authzn.Policy.authorize;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class UnitTest {

    private static Policy policy = PolicyLoader.getPolicy();

    @Test
    public void testIt() {
        assertNotNull(policy);
        assertEquals(DENY_ALL, policy);
    }

    @Test
    public void testThis() {
        User user = () -> "id";
        Resource resource = Resource.parse("type:/a/b/c");
        Action action = Action.parse("type:action");
        assertEquals(DENY, authorize(user).toPerform(action).on(resource));
        assertThrows(SecurityException.class, () -> authorize(user).toPerform(action).on(resource).execute(() -> null));
        assertThrows(SecurityException.class, () -> authorize(user,
                action,
                resource,
                () -> null));
        assertThrows(SecurityException.class, () -> authorize(user).toPerform(action).on(resource).execute(() -> {}));
    }

    @Test
    public void testThat() throws Exception {
        User user = () -> "id";
        Resource resource = Resource.parse("type:/a/b/c");
        Action action = Action.parse("type:action");
        Object REF = new Object();
        assertEquals(ALLOW, Policies.ALLOW_ALL._authorize(user).toPerform(action).on(resource));
        assertEquals(REF,  Policies.ALLOW_ALL._authorize(user).toPerform(action).on(resource).execute(() -> REF));
        assertEquals(REF,  Policies.ALLOW_ALL._authorize(user,
                action,
                resource,
                () -> REF));
    }

    @Test
    public void testSimplePolicy() throws Exception {
        User user = () -> "name";
        Resource resource = Resource.parse("type:/a/b/c");
        SimplePolicy simplePolicy = new SimplePolicy();
        Action action = Action.parse("type:action");
        simplePolicy.add(new Directive(resource, action, user));
        assertEquals(ALLOW,  simplePolicy._authorize(user,
                action,
                resource));
        assertEquals(ALLOW,  simplePolicy._authorize(user,
                action,
                Resource.parse("type:/a/b/c")));
        assertEquals(ALLOW,  simplePolicy._authorize(user,
                action,
                Resource.parse("type:/a/b/c/d")));
        assertEquals(DENY,  simplePolicy._authorize(user,
                action,
                Resource.parse("type:/a/b")));
        assertEquals(DENY,  simplePolicy._authorize(user,
                Action.parse("type:action1"),
                Resource.parse("type:/a/b/c")));
        assertEquals(DENY,  simplePolicy._authorize(() -> "blah",
                action,
                Resource.parse("type:/a/b/c")));
    }

    @Test
    public void testWildcard() {
        User user = () -> "name";
        Resource resource = Resource.parse("type:/a/b/c");
        SimplePolicy simplePolicy = new SimplePolicy();
        Action wildcardAction = Action.parse("type:*");
        Action action = Action.parse("type:action");
        simplePolicy.add(new Directive(resource, wildcardAction, user));
        assertEquals(ALLOW,  simplePolicy._authorize(user,
                action,
                resource));
        assertEquals(ALLOW,  simplePolicy._authorize(user,
                action,
                Resource.parse("type:/a/b/c")));
        assertEquals(ALLOW,  simplePolicy._authorize(user,
                action,
                Resource.parse("type:/a/b/c/d")));
        assertEquals(DENY,  simplePolicy._authorize(user,
                action,
                Resource.parse("type:/a/b")));
        assertEquals(ALLOW,  simplePolicy._authorize(user,
                Action.parse("type:action1"),
                Resource.parse("type:/a/b/c")));
        assertEquals(DENY,  simplePolicy._authorize(() -> "blah",
                action,
                Resource.parse("type:/a/b/c")));
    }

    @Test
    public void testDenyDirective() {
        User user = () -> "name";
        Resource resource = Resource.parse("type:/a/b/c");
        SimplePolicy simplePolicy = new SimplePolicy();
        Action action = UPDATE;
        simplePolicy.add(new Directive(resource, action, user));
        simplePolicy.add(new Directive(Resource.parse("type:/a/b/c/d"), Action.CRUDActions.ALL, user, DENY));
        simplePolicy.add(new Directive(Resource.parse("type:/a/b/c/d/e"), action, user, ALLOW));
        assertEquals(ALLOW,  simplePolicy._authorize(user,
                action,
                resource));
        assertEquals(ALLOW,  simplePolicy._authorize(user,
                action,
                Resource.parse("type:/a/b/c")));
        assertEquals(DENY,  simplePolicy._authorize(user,
                action,
                Resource.parse("type:/a/b/c/d")));
        assertEquals(ALLOW,  simplePolicy._authorize(user,
                action,
                Resource.parse("type:/a/b/c/d/e")));
    }

}
