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
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions

class AllCISDeductionsSpec extends UnitTest {

  ".inYearCisDeductions" should {
    "extract only contractor cisDeductions" in {
      val aCisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"))
      val aCisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"))
      val underTest = AllCISDeductions(
        customerCISDeductions = Some(aCISSource),
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions1, aCisDeductions2)))
      )

      underTest.inYearCisDeductions shouldBe Seq(aCisDeductions1, aCisDeductions2)
    }
  }

  "inYearCisDeductionsWith(empRef)" should {
    "return CisDeductions with given employer reference when exists" in {
      val aCisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"), employerRef = "ref-1")
      val aCisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "ref-2")
      val underTest = AllCISDeductions(
        customerCISDeductions = Some(aCISSource),
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions1, aCisDeductions2)))
      )

      underTest.inYearCisDeductionsWith(employerRef = "ref-2") shouldBe Some(aCisDeductions2)
    }

    "return none when no CiwDeductions for a reference exists" in {
      val aCisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"), employerRef = "ref-1")
      val aCisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "ref-2")
      val underTest = anAllCISDeductions.copy(
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions1, aCisDeductions2)))
      )

      underTest.inYearCisDeductionsWith(employerRef = "unknown-ref") shouldBe None
    }
  }
}
