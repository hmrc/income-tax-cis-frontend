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
      monthToDates(Month.JANUARY, 2022) shouldBe ("2021-12-06","2022-01-05")
      monthToDates(Month.FEBRUARY, 2022) shouldBe ("2022-01-06","2022-02-05")
      monthToDates(Month.MARCH, 2022) shouldBe ("2022-02-06","2022-03-05")
      monthToDates(Month.APRIL, 2022) shouldBe ("2022-03-06","2022-04-05")
      monthToDates(Month.MAY, 2022) shouldBe ("2021-04-06","2021-05-05")
      monthToDates(Month.JUNE, 2022) shouldBe ("2021-05-06","2021-06-05")
      monthToDates(Month.JULY, 2022) shouldBe ("2021-06-06","2021-07-05")
      monthToDates(Month.AUGUST, 2022) shouldBe ("2021-07-06","2021-08-05")
      monthToDates(Month.SEPTEMBER, 2022) shouldBe ("2021-08-06","2021-09-05")
      monthToDates(Month.OCTOBER, 2022) shouldBe ("2021-09-06","2021-10-05")
      monthToDates(Month.NOVEMBER, 2022) shouldBe ("2021-10-06","2021-11-05")
      monthToDates(Month.DECEMBER, 2022) shouldBe ("2021-11-06","2021-12-05")
    }
  }
}
