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

package forms

import play.api.data.FormError
import support.UnitTest

import java.time.Month

class DeductionPeriodFormProviderSpec extends UnitTest {

  private val underTest = new DeductionPeriodFormProvider()

  "deductionPeriodForm" should {
    "return a form that maps data when data is correct" in {
      val anyBoolean = true
      val correctData = Map("month" -> "may")

      underTest.deductionPeriodForm(isAgent = anyBoolean).bind(correctData).errors shouldBe Seq.empty
    }

    "return a form that maps data when data is correct and doesn't overlap other months" in {
      val anyBoolean = true
      val correctData = Map("month" -> "may")

      val months = Month.values().toIndexedSeq
      underTest.deductionPeriodForm(isAgent = anyBoolean, months.filterNot(_ == Month.MAY)).bind(correctData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and month is already submitted" in {
        val invalidData = Map("month" -> "may")
        val months = Month.values().toIndexedSeq
        underTest.deductionPeriodForm(isAgent = true, months).bind(invalidData).errors shouldBe Seq(
          FormError("month", Seq("deductionPeriod.error.agent"), Seq())
        )
      }
      "when isAgent is true and month is invalid" in {
        val invalidData = Map("month" -> "beans")
        underTest.deductionPeriodForm(isAgent = true).bind(invalidData).errors shouldBe Seq(
          FormError("month", Seq("deductionPeriod.error.agent"), Seq())
        )
      }
      "when isAgent is true and month is empty" in {
        val invalidData = Map("month" -> "")
        underTest.deductionPeriodForm(isAgent = true).bind(invalidData).errors shouldBe Seq(
          FormError("month", Seq("deductionPeriod.error.agent"), Seq())
        )
      }
      "when isAgent is true and key is invalid" in {
        val invalidData = Map("key" -> "")
        val months = Month.values().toIndexedSeq
        underTest.deductionPeriodForm(isAgent = true, months).bind(invalidData).errors shouldBe Seq(
          FormError("month", Seq("deductionPeriod.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and month is already submitted" in {
        val invalidData = Map("month" -> "may")
        val months = Month.values().toIndexedSeq
        underTest.deductionPeriodForm(isAgent = false, months).bind(invalidData).errors shouldBe Seq(
          FormError("month", Seq("deductionPeriod.error.individual"), Seq())
        )
      }
      "when isAgent is false and month is invalid" in {
        val invalidData = Map("month" -> "beans")
        underTest.deductionPeriodForm(isAgent = false).bind(invalidData).errors shouldBe Seq(
          FormError("month", Seq("deductionPeriod.error.individual"), Seq())
        )
      }
      "when isAgent is false and month is empty" in {
        val invalidData = Map("month" -> "")
        underTest.deductionPeriodForm(isAgent = false).bind(invalidData).errors shouldBe Seq(
          FormError("month", Seq("deductionPeriod.error.individual"), Seq())
        )
      }
      "when isAgent is false and key is invalid" in {
        val invalidData = Map("key" -> "")
        val months = Month.values().toIndexedSeq
        underTest.deductionPeriodForm(isAgent = false, months).bind(invalidData).errors shouldBe Seq(
          FormError("month", Seq("deductionPeriod.error.individual"), Seq())
        )
      }
    }
  }
}
