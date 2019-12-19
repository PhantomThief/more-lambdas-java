package com.github.phantomthief.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.github.phantomthief.util.ToStringHelper;

/**
 * @author w.vela
 * Created on 16/5/7.
 */
class ToStringHelperTest {

    @Test
    void test() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        String ori = list.toString();
        List<Integer> list2 = ToStringHelper.wrapToString(List.class, list, i -> i + "!!!!");
        String newToString = list2.toString();
        assertEquals(ori + "!!!!", newToString);
    }
}
