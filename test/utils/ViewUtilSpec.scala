/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import support.UnitTest

import java.time.Month

class ViewUtilSpec extends UnitTest with GuiceOneAppPerSuite with ViewTest {

  "calling method convertBoolToYesNo" should {
    "return yes when cis field is true" in {
      ViewUtils.convertBoolToYesOrNo(cisField = true) shouldBe "Yes"
    }

    "return no when cis field is false" in {
      ViewUtils.convertBoolToYesOrNo(cisField = false) shouldBe "No"
    }
  }

  "calling method DateFormatter" should {
    "reformat date when valid date is passed" in {
      ViewUtils.dateFormatter("2022-03-10").get shouldBe "10 March 2022"
    }

    "return None when invalid date is passed" in {
      ViewUtils.dateFormatter("10-03-2022") shouldBe None
      ViewUtils.dateFormatter("2022/03/10") shouldBe None
      ViewUtils.dateFormatter("01 March 2022") shouldBe None
    }
  }

  "bigDecimalCurrency" should {
    "Place comma in appropriate place when given amount over 999" in {
      ViewUtils.bigDecimalCurrency("45000.10") shouldBe "£45,000.10"
    }
  }

  "translateMonthAndTaxYear" should {
    "correctly translate the month and tax year" in {
      ViewUtils.translatedMonthAndTaxYear(Month.JULY, 2022) shouldBe "July 2021"
      ViewUtils.translatedMonthAndTaxYear(Month.APRIL, 2022) shouldBe "April 2022"
    }
  }
}
