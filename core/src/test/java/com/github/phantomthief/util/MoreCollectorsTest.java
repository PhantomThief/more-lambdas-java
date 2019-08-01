package com.github.phantomthief.util;

import static com.github.phantomthief.util.MoreCollectors.groupingByAllowNullKey;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * MoreCollectorsTest
 * <p>
 * Write the code. Change the world.
 *
 * @author trang
 * @date 2019-07-31
 */
class MoreCollectorsTest {

    /**
     * toMap() 本身支持 null key，但不支持 null value
     */
    @Test
    void toMapTest() {
        Map<Integer, TestEnum1> map =
                Stream.of(TestEnum1.values()).collect(toMap(TestEnum1::getValue, identity()));
        assertEquals(4, map.size());
        assertNotNull(map.get(null));
    }

    @Test
    void groupingByTest() {
        assertThrows(NullPointerException.class,
                () -> Stream.of(TestEnum2.values()).collect(groupingBy(TestEnum2::getValue)));
    }

    @Test
    void groupingByAllowNullKeyTest() {
        Map<Integer, List<TestEnum2>> map =
                Stream.of(TestEnum2.values()).collect(groupingByAllowNullKey(TestEnum2::getValue));
        assertEquals(5, map.size());
        assertEquals(2, map.get(0).size());
        assertEquals(1, map.get(null).size());
    }

    /**
     * mapFactory 如果为不支持 null 的 map 类型，仍然会抛出 NPE
     * 支持 null key，但仍然不支持 null value
     */
    @Test
    void groupingByAllowNullKeyTest2() {
        assertThrows(NullPointerException.class,
                () -> Stream.of(TestEnum2.values())
                        .collect(groupingByAllowNullKey(TestEnum2::getValue, Hashtable::new, toList())));
    }

    private enum TestEnum1 {
        A(1),
        B(2),
        C(3),
        F(null),
        ;

        private final Integer value;

        TestEnum1(Integer value) {
            this.value = value;
        }

        private Integer getValue() {
            return value;
        }
    }

    private enum TestEnum2 {
        A(1),
        B(2),
        C(3),
        D(0),
        E(0),
        F(null),
        ;

        private final Integer value;

        TestEnum2(Integer value) {
            this.value = value;
        }

        private Integer getValue() {
            return value;
        }
    }

}