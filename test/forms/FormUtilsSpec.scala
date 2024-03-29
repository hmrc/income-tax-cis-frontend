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

import forms.AmountForm.amountForm
import play.api.data.Form
import support.UnitTest

class FormUtilsSpec extends UnitTest with FormUtils {

  def theForm(): Form[BigDecimal] = {
    amountForm("nothing to see here", "this not good", "too big")
  }

  "The form" should {
    "fill the form" when {
      "the prior and cya amount are different" in {
        val actual = fillForm(theForm(), Some(44.44), Some(23.33))
        actual shouldBe theForm().fill(23.33)
      }

      "there's cya data and no prior data" in {
        val actual = fillForm(theForm(), None, Some(5.55))
        actual shouldBe theForm().fill(5.55)
      }
    }
  }

  "not fill the form" when {
    "there is no data" in {
      val actual = fillForm(theForm(), None, None)
      actual.value shouldBe None
    }

    "the prior amount and cya amount are the same" in {
      val actual = fillForm(theForm(), Some(10.00), Some(10.00))
      actual.value shouldBe None
    }
  }
}