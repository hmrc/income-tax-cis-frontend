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

class FormsProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val amount: String = 123.0.toString
  private val correctBooleanData = Map(YesNoForm.yesNo -> anyBoolean.toString)
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val overMaximumAmount: Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty

  private val underTest = new FormsProvider()

  ".labourPayAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.labourPayAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.labourPayAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("labourPayPage.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.labourPayAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("labourPayPage.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.labourPayAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("labourPayPage.error.wrongFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.labourPayAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("labourPayPage.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.labourPayAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("labourPayPage.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.labourPayAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("labourPayPage.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.labourPayAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("labourPayPage.error.wrongFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.labourPayAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("labourPayPage.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".deductionAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.deductionAmountForm().bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form with error when key is wrong" in {
      underTest.deductionAmountForm().bind(wrongKeyData).errors shouldBe
        Seq(FormError(AmountForm.amount, Seq("deductionAmountPage.error.noEntry"), Seq()))
    }

    "return a form with error when data is empty" in {
      underTest.deductionAmountForm().bind(Map.empty[String, String]).errors shouldBe
        Seq(FormError(AmountForm.amount, Seq("deductionAmountPage.error.noEntry"), Seq()))
    }

    "return a form with error when data is wrongFormat" in {
      underTest.deductionAmountForm().bind(wrongAmountFormat).errors shouldBe
        Seq(FormError(AmountForm.amount, Seq("deductionAmountPage.error.wrongFormat"), Seq()))
    }

    "return a form with error when data is overMaximum" in {
      underTest.deductionAmountForm().bind(overMaximumAmount).errors shouldBe
        Seq(FormError(AmountForm.amount, Seq("deductionAmountPage.error.overMaximum"), Seq()))
    }
  }

  ".deductionsFromPaymentsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.deductionsFromPaymentsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.deductionsFromPaymentsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("deductionsFromPaymentsPage.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.deductionsFromPaymentsForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("deductionsFromPaymentsPage.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.deductionsFromPaymentsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("deductionsFromPaymentsPage.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.deductionsFromPaymentsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("deductionsFromPaymentsPage.error.individual"), Seq())
        )
      }
    }
  }

  ".materialsYesNoForm" should {
    "return a form that maps data when data is correct" in {
      underTest.materialsYesNoForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.materialsYesNoForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("materialsPage.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.materialsYesNoForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("materialsPage.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.materialsYesNoForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("materialsPage.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.materialsYesNoForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("materialsPage.error.individual"), Seq())
        )
      }
    }
  }

  ".materialsAmountForm" should {
    "return a form errors for individuals" when {
      "an empty form is submitted" in {
        underTest.materialsAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("materialsAmountPage.error.noEntry.individual"), Seq())
        )
      }

      "the data is in the wrong format" in {
        underTest.materialsAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("materialsAmountPage.error.incorrectFormat.individual"), Seq())
        )
      }

      "the data is over the maximum amount" in {
        underTest.materialsAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("materialsAmountPage.error.overMaximum.individual"), Seq())
        )
      }
    }

    "return a form errors for agents" when {
      "an empty form is submitted" in {
        underTest.materialsAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("materialsAmountPage.error.noEntry.agent"), Seq())
        )
      }

      "the data is in the wrong format" in {
        underTest.materialsAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("materialsAmountPage.error.incorrectFormat.agent"), Seq())
        )
      }

      "the data is over the maximum amount" in {
        underTest.materialsAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("materialsAmountPage.error.overMaximum.agent"), Seq())
        )
      }
    }
  }
}
