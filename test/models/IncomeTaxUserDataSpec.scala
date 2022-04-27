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

import java.time.Month

import support.UnitTest
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData

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

  ".inYearCisDeductionsWith(empRef, month)" should {
    "return in year CisDeductions with given reference and month if exists" in {
      val mayPeriodData = aPeriodData.copy(deductionPeriod = Month.MAY)
      val julyPeriodData = aPeriodData.copy(deductionPeriod = Month.JULY)
      val cisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"))
      val cisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "ref-2", periodData = Seq(mayPeriodData, julyPeriodData))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions1, cisDeductions2))))

      val underTest = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      underTest.inYearCisDeductionsWith(employerRef = "ref-2", month = Month.JULY) shouldBe Some(cisDeductions2)
    }

    "return None when in year CisDeductions with given reference and month does not exist" in {
      val mayPeriodData = aPeriodData.copy(deductionPeriod = Month.MAY)
      val julyPeriodData = aPeriodData.copy(deductionPeriod = Month.JULY)
      val cisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"), employerRef = "ref-1", periodData = Seq(julyPeriodData))
      val cisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "ref-2", periodData = Seq(mayPeriodData))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions1, cisDeductions2))))

      val underTest = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      underTest.inYearCisDeductionsWith(employerRef = "ref-2", month = Month.JULY) shouldBe None
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

  ".inYearPeriodDataWith(...)" should {
    "return in year PeriodData for a given employer" in {
      val periodData1 = aPeriodData.copy(deductionPeriod = Month.SEPTEMBER)
      val periodData2 = aPeriodData.copy(deductionPeriod = Month.OCTOBER)
      val periodData3 = aPeriodData.copy(deductionPeriod = Month.NOVEMBER)
      val cisDeductions1 = aCisDeductions.copy(employerRef = "12345", periodData = Seq(periodData1, periodData2))
      val cisDeduction2 = aCisDeductions.copy(employerRef = "678910", periodData = Seq(periodData3))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions1, cisDeduction2))))

      val underTest = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      underTest.inYearPeriodDataWith(employerRef = "12345") shouldBe Seq(periodData1, periodData2)
      underTest.inYearPeriodDataWith(employerRef = "678910") shouldBe Seq(periodData3)
    }

    "return empty in year PeriodData for a given employer if the sequence is empty" in {
      val cisDeductions = aCisDeductions.copy(employerRef = "12345", periodData = Seq())
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions))))

      val underTest = anIncomeTaxUserData.copy(Some(allCISDeductions))

      underTest.inYearPeriodDataWith(employerRef = "12345") shouldBe Seq.empty
    }

    "return empty in year PeriodData for a given employer if the employerRef is unknown" in {
      val cisDeductions = aCisDeductions.copy(employerRef = "12345", periodData = Seq(aPeriodData))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions))))
      val underTest = anIncomeTaxUserData.copy(Some(allCISDeductions))

      underTest.inYearPeriodDataWith(employerRef = "unknown-employerRef") shouldBe Seq.empty
    }
  }

  ".hasInYearPeriodDataWith(...)" should {
    "return true when in year period data exists for a given employerRef" in {
      val periodData = aPeriodData.copy(deductionPeriod = Month.OCTOBER)
      val cisDeductions = aCisDeductions.copy(employerRef = "12345", periodData = Seq(periodData))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions))))

      val underTest = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      underTest.hasInYearPeriodDataWith(employerRef = "12345") shouldBe true
    }

    "return false when is in year period data doesn't exist for a given employerRef" in {
      val cisDeductions = aCisDeductions.copy(employerRef = "12345", periodData = Seq())
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions))))

      val underTest = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      underTest.hasInYearPeriodDataWith(employerRef = "12345") shouldBe false
    }
  }

  ".getCISDeductionsFor" should {
    "extract the latest cisDeductions" in {

      val underTest = anIncomeTaxUserData

      underTest.getEOYCISDeductionsFor(aCisDeductions.employerRef) shouldBe Some(aCisDeductions)
    }
    "extract the latest cisDeductions when no customer data" in {

      val underTest = anIncomeTaxUserData.copy(
        cis = Some(anAllCISDeductions.copy(
          customerCISDeductions = None
        ))
      )

      underTest.getEOYCISDeductionsFor(aCisDeductions.employerRef) shouldBe Some(aCisDeductions)
    }
    "extract the latest cisDeductions when contractor data is latest" in {

      val underTest = anIncomeTaxUserData.copy(
        cis = Some(anAllCISDeductions.copy(
          customerCISDeductions = Some(aCISSource),
          contractorCISDeductions = Some(aCISSource.copy(
            cisDeductions = Seq(
              aCisDeductions.copy(
                periodData = Seq(aPeriodData, aPeriodData.copy(deductionPeriod = Month.DECEMBER,submissionDate = "2021-05-11T16:38:57.489Z"))
              )
            )
          ))
        ))
      )

      underTest.getEOYCISDeductionsFor(aCisDeductions.employerRef) shouldBe Some(aCisDeductions.copy(
        totalDeductionAmount = Some(200.00),
        totalCostOfMaterials = Some(100.00),
        totalGrossAmountPaid = Some(900.00),
        periodData = Seq(aPeriodData, aPeriodData.copy(deductionPeriod = Month.DECEMBER, submissionDate = "2021-05-11T16:38:57.489Z"))
      ))
    }
    "extract the latest cisDeductions when no contractor data" in {

      val underTest = anIncomeTaxUserData.copy(
        cis = Some(anAllCISDeductions.copy(
          customerCISDeductions = None
        ))
      )

      underTest.getEOYCISDeductionsFor(aCisDeductions.employerRef) shouldBe Some(aCisDeductions)
    }
  }

}
