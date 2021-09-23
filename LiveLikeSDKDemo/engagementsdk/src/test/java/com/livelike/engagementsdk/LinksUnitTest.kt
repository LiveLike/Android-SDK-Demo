package com.livelike.engagementsdk

import com.livelike.engagementsdk.chat.stickerKeyboard.countMatches
import org.junit.Test

open class LinksUnitTest {

    @Test
    fun testLinks() {
        val linksRegex = "((([A-Za-z]{3,9}:(?:\\/\\/)?)(?:[\\-;:&=\\+\\$,\\w]+@)?[A-Za-z0-9\\.\\-]+|(?:(www)?\\.|[\\-;:&=\\+\\$,\\w]+@)[A-Za-z0-9\\.\\-]+)((?:\\/[\\+~%\\/\\.\\w\\-_]*)?\\??(?:[\\-\\+=&;%@\\.\\w_]*)#?(?:[\\.\\!\\/\\\\\\w]*))?)".toRegex()
        val list = arrayListOf(
            "www.livelike.com",
            "http://www.livelike.com",
            "https://www.livelike.com",
            "livelike.com",
            "bit.ly/livelike",
            "https://www.livelike.com?a=abc",
            "www.livelike.com :LeeHype::LeeHype: livelike.com"
        )
        list.forEach {
            val result = linksRegex.toPattern().matcher(it)
            println("Check-> $it -> ${result.countMatches()}")
        }
    }
}
