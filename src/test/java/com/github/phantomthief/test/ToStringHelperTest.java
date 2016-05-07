package com.github.phantomthief.test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;

import com.github.phantomthief.util.ToStringHelper;

/**
 * @author w.vela
 * Created on 16/5/7.
 */
public class ToStringHelperTest {

    @Test
    public void test() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        String ori = list.toString();
        List<Integer> list2 = ToStringHelper.wrapToString(List.class, list, i -> i + "!!!!");
        String newToString = list2.toString();
        Assert.assertTrue(Objects.equals(ori + "!!!!", newToString));
    }
}
