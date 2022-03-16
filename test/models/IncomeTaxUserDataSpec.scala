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
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData

import java.time.Month

class IncomeTaxUserDataSpec extends UnitTest {

  ".hasInYearCisDeductions" should {
    "true when there are in year CisDeductions" in {
      val underTest = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions))

      underTest.hasInYearCisDeductions shouldBe true
    }

    "false when there are not in year CisDeductions" in {
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq.empty)))

      val underTest = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      underTest.hasInYearCisDeductions shouldBe false
    }
  }

  ".inYearCisDeductionsWith(...)" should {
    "return in year CisDeductions with given reference" in {
      val cisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"))
      val cisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "ref-2")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions1, cisDeductions2))))

      val underTest = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      underTest.inYearCisDeductionsWith(employerRef = "ref-2") shouldBe Some(cisDeductions2)
    }
  }

  ".hasInYearCisDeductionsWith(...)" should {
    "return true when CisDeductions with employerRef exists" in {
      val cisDeductions = aCisDeductions.copy(employerRef = "some-ref")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions))))

      val underTest = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      underTest.hasInYearCisDeductionsWith(employerRef = "some-ref") shouldBe true
    }

    "return false when CisDeductions with employerRef exists" in {
      val underTest = anIncomeTaxUserData

      underTest.hasInYearCisDeductionsWith(employerRef = "unknown-ref") shouldBe false
    }
  }

  ".inYearPeriodDataFor(...)" should {
    "return in year PeriodData for given employer and month" in {
      val periodData = aPeriodData.copy(deductionPeriod = Month.JUNE)
      val cisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"))
      val cisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "ref-2", periodData = Seq(periodData))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions1, cisDeductions2))))

      val underTest = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      underTest.inYearPeriodDataFor(employerRef = "ref-2", Month.JUNE) shouldBe Some(periodData)
    }

    "return empty in year PeriodData for given employer and month if it does not exist" in {
      val underTest = anIncomeTaxUserData

      underTest.inYearPeriodDataFor(employerRef = "unknown-reference", Month.JUNE) shouldBe None
    }
  }

  ".hasInYearPeriodDataFor" should {
    "return true when in year PeriodData for given employer and month exists" in {
      val periodData = aPeriodData.copy(deductionPeriod = Month.JUNE)
      val cisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"))
      val cisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "ref-2", periodData = Seq(periodData))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions1, cisDeductions2))))

      val underTest = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      underTest.hasInYearPeriodDataFor(employerRef = "ref-2", Month.JUNE) shouldBe true
    }

    "return false when in year PeriodData for given employer and month does not exist" in {
      val underTest = anIncomeTaxUserData

      underTest.hasInYearPeriodDataFor(employerRef = "unknown-reference", Month.JUNE) shouldBe false
    }
  }

  ".getCISDeductionsFor" should {
    "extract the latest cisDeductions" in {

      val underTest = anIncomeTaxUserData

      underTest.getCISDeductionsFor(aCisDeductions.employerRef) shouldBe Some(aCisDeductions)
    }
    "extract the latest cisDeductions when no customer data" in {

      val underTest = anIncomeTaxUserData.copy(
        cis = Some(anAllCISDeductions.copy(
          customerCISDeductions = None
        ))
      )

      underTest.getCISDeductionsFor(aCisDeductions.employerRef) shouldBe Some(aCisDeductions)
    }
    "extract the latest cisDeductions when contractor data is latest" in {

      val underTest = anIncomeTaxUserData.copy(
        cis = Some(anAllCISDeductions.copy(
          customerCISDeductions = Some(aCISSource),
          contractorCISDeductions = Some(aCISSource.copy(
            cisDeductions = Seq(
              aCisDeductions.copy(
                periodData = Seq(aPeriodData, aPeriodData.copy(submissionDate = "2021-05-11T16:38:57.489Z"))
              )
            )
          ))
        ))
      )

      underTest.getCISDeductionsFor(aCisDeductions.employerRef) shouldBe Some(aCisDeductions.copy(
        periodData = Seq(aPeriodData, aPeriodData.copy(submissionDate = "2021-05-11T16:38:57.489Z"))
      ))
    }
    "extract the latest cisDeductions when no contractor data" in {

      val underTest = anIncomeTaxUserData.copy(
        cis = Some(anAllCISDeductions.copy(
          customerCISDeductions = None
        ))
      )

      underTest.getCISDeductionsFor(aCisDeductions.employerRef) shouldBe Some(aCisDeductions)
    }
  }

}
