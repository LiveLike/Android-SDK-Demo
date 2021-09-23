package com.livelike.engagementsdk

import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

@RunWith(Theories::class)
open class EpochTimeUnitTest {

    @Theory
    fun testISOFormats(sampleFormat: String) {
        assert(sampleFormat.parseISODateTime() != null)
    }

    @Test
    fun testInValidMonthFormat() {
        assert("2021-22-22T06:55:03,000000+00:00".parseISODateTime() == null)
    }

    companion object {
        @DataPoints
        @JvmField
        var candidates = arrayListOf(
            "2020-09-04T16:40:53.000Z",
            "2020-09-04T16:40:53.000000Z",
            "2020-09-04T16:40:53+05:30",
            "2020-09-04T16:40:53.000000+00:00",
            "2020-09-04T16:40:53Z",
        )
    }
}
