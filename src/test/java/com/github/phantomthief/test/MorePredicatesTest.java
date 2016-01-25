/**
 * 
 */
package com.github.phantomthief.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import com.github.phantomthief.util.MorePredicates;

/**
 * @author w.vela
 */
public class MorePredicatesTest {

    @Test
    public void testDistinctUsing() {
        List<Bean> source = Arrays.asList(new Bean("name1", "address1"), new Bean("name1",
                "address2"), new Bean("name3", "address2"));
        List<Bean> target = source.stream() //
                .filter(MorePredicates.distinctUsing(Bean::getName)) //
                .collect(Collectors.toList());
        Set<String> result = new HashSet<>();
        for (Bean bean : target) {
            assert (result.add(bean.getName()));
        }
    }

    @Test
    public void testAfter() {
        List<Integer> source = IntStream.range(1, 100).boxed().collect(Collectors.toList());
        List<Integer> target = source.stream() //
                .filter(MorePredicates.afterElement(10)) //
                .collect(Collectors.toList());
        System.out.println(target);
        for (Integer i : target) {
            assert (i >= 10);
        }
        target = source.stream() //
                .filter(MorePredicates.afterElement(10, false)) //
                .collect(Collectors.toList());
        System.out.println(target);
        for (Integer i : target) {
            assert (i > 10);
        }
        target = source.stream() //
                .filter(MorePredicates.after(i -> i == 10, false)) //
                .collect(Collectors.toList());
        System.out.println(target);
        for (Integer i : target) {
            assert (i > 10);
        }
    }

    private static final class Bean {

        private final String name;
        private final String address;

        /**
         * @param name
         * @param address
         */
        Bean(String name, String address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((address == null) ? 0 : address.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Bean)) {
                return false;
            }
            Bean other = (Bean) obj;
            if (address == null) {
                if (other.address != null) {
                    return false;
                }
            } else if (!address.equals(other.address)) {
                return false;
            }
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Bean [name=" + name + ", address=" + address + "]";
        }

    }
}
