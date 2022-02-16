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

class MonthEnumsSpec extends UnitTest {

  ".apply" should {
    s"return the correct month when the string matches 'May'" in {
      Month.apply("May") shouldBe MAY
    }

    "throw an exception when an unknown key is entered" in {
      val input: String = "strawberries"

      val caught =
        intercept[Exception] {
          Month.apply(input)
        }

      assert(caught.getMessage == s"'${input.toUpperCase}' cannot be converted to a month")
    }

  }

  "each month" should {
    "have a value which is the month written as a string (May)" in {
      MAY.value shouldBe "May"
    }
    "have a value which is the month written as a string (June)" in {
      JUNE.value shouldBe "June"
    }
    "have a value which is the month written as a string (July)" in {
      JULY.value shouldBe "July"
    }
    "have a value which is the month written as a string (August)" in {
      AUGUST.value shouldBe "August"
    }
    "have a value which is the month written as a string (September)" in {
      SEPTEMBER.value shouldBe "September"
    }
    "have a value which is the month written as a string (October)" in {
      OCTOBER.value shouldBe "October"
    }
    "have a value which is the month written as a string (November)" in {
      NOVEMBER.value shouldBe "November"
    }
    "have a value which is the month written as a string (December)" in {
      DECEMBER.value shouldBe "December"
    }
    "have a value which is the month written as a string (January)" in {
      JANUARY.value shouldBe "January"
    }
    "have a value which is the month written as a string (February)" in {
      FEBRUARY.value shouldBe "February"
    }
    "have a value which is the month written as a string (March)" in {
      MARCH.value shouldBe "March"
    }
    "have a value which is the month written as a string (April)" in {
      APRIL.value shouldBe "April"
    }
  }
}
