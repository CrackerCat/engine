package jadx.tests.integration.conditions;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import org.junit.Test;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestConditions11 extends IntegrationTest {

    @Test
    public void test() {
        ClassNode cls = getClassNode(TestCls.class);
        String code = cls.getCode().toString();

        assertThat(code, containsOne("if (a || b > 2) {"));
        assertThat(code, containsOne("f();"));
        assertThat(code, not(containsString("return")));
        assertThat(code, not(containsString("else")));

    }

    public static class TestCls {

        public void test(boolean a, int b) {
            if (a || b > 2) {
                f();
            }
        }

        private void f() {
        }
    }
}