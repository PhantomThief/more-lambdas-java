package com.github.phantomthief.util

import com.github.phantomthief.util.MoreReflection.logDeprecated

/**
 * @author w.vela
 * Created on 2018-06-22.
 */
internal object KotlinCaller {

    @JvmStatic
    fun test1() {
        test();
    }

    @JvmStatic
    private fun test() {
        logDeprecated(System.err::println)
    }
}