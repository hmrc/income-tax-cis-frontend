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

package models

import support.UnitTest
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions

class CISSourceSpec extends UnitTest {

  ".cisDeductionsWith(employerRef)" should {
    "return deduction with given employerRef when exists" in {
      val deductions = aCisDeductions.copy(employerRef = "ref-1")
      val underTest = aCISSource.copy(cisDeductions = Seq(deductions))

      underTest.cisDeductionsWith(employerRef = "ref-1") shouldBe Some(deductions)
    }

    "return None when employerRef does not exist" in {
      val deductions = aCisDeductions.copy(employerRef = "ref-1")
      val underTest = aCISSource.copy(cisDeductions = Seq(deductions))

      underTest.cisDeductionsWith(employerRef = "unknown-ref") shouldBe None
    }
  }
}
