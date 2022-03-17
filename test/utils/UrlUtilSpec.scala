/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import akka.util.ByteString.UTF_8
import support.UnitTest

import java.net.{URLDecoder, URLEncoder}

class UrlUtilSpec extends UnitTest {

  ".decode" should {
    "return a decoded string" in {
      UrlUtils.decode(value = "123%2F12345") shouldBe URLDecoder.decode("123%2F12345",UTF_8)
      UrlUtils.decode(value = "11111") shouldBe URLDecoder.decode("11111",UTF_8)
      UrlUtils.decode(value = "toast") shouldBe URLDecoder.decode("toast",UTF_8)
    }
  }

    ".encode" should {
    "return an encoded string" in {
      UrlUtils.encode(value = "111/22222") shouldBe URLEncoder.encode("111/22222", UTF_8)
      UrlUtils.encode(value = "55555") shouldBe URLEncoder.encode("55555", UTF_8)
      UrlUtils.encode(value = "strawberries") shouldBe URLEncoder.encode("strawberries", UTF_8)
    }
  }

}
