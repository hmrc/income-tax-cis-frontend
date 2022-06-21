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

import support.UnitTest
import utils.DateMonthUtil.{monthToDates, monthToTaxYearConverter}

import java.time.Month

class DateMonthUtilSpec extends UnitTest {

  "monthToTaxYearConverter" should {
    "return the correct taxYear when a month is passed" in {
      monthToTaxYearConverter(Month.JUNE, 2022) shouldBe 2021
      monthToTaxYearConverter(Month.FEBRUARY, 2022) shouldBe 2022
    }
  }

  "monthToDates" should {
    "return the correct dates for a month and tax year" in {

      Month.values().map{
        month =>
          val (from, to) = monthToDates(month, 2022)

          month match {
            case Month.JANUARY => from shouldBe "2021-12-06"; to shouldBe "2022-01-05"
            case Month.FEBRUARY => from shouldBe "2022-01-06"; to shouldBe "2022-02-05"
            case Month.MARCH => from shouldBe "2022-02-06"; to shouldBe "2022-03-05"
            case Month.APRIL => from shouldBe "2022-03-06"; to shouldBe "2022-04-05"
            case Month.MAY => from shouldBe "2021-04-06"; to shouldBe "2021-05-05"
            case Month.JUNE => from shouldBe "2021-05-06"; to shouldBe "2021-06-05"
            case Month.JULY => from shouldBe "2021-06-06"; to shouldBe "2021-07-05"
            case Month.AUGUST => from shouldBe "2021-07-06"; to shouldBe "2021-08-05"
            case Month.SEPTEMBER => from shouldBe "2021-08-06"; to shouldBe "2021-09-05"
            case Month.OCTOBER => from shouldBe "2021-09-06"; to shouldBe "2021-10-05"
            case Month.NOVEMBER => from shouldBe "2021-10-06"; to shouldBe "2021-11-05"
            case Month.DECEMBER => from shouldBe "2021-11-06"; to shouldBe "2021-12-05"
          }
      }
    }
  }
}
