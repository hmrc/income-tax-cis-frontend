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

import models.submission.CISSubmission
import support.{TaxYearProvider, UnitTest}
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData

import java.time.Month

class IncomeTaxUserDataSpec extends UnitTest with TaxYearProvider {

  ".hasExclusivelyCustomerEoyCisDeductionsWith" should {
    "return false when no data" in {
      val underTest = anIncomeTaxUserData.copy(cis = None)

      underTest.hasExclusivelyCustomerEoyCisDeductionsWith(aCisDeductions.employerRef,aPeriodData.deductionPeriod) shouldBe false
    }
    "return false when only contractor data" in {
      val underTest = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(customerCISDeductions = None)))

      underTest.hasExclusivelyCustomerEoyCisDeductionsWith(aCisDeductions.employerRef,aPeriodData.deductionPeriod) shouldBe false
    }
    "return true when only customer data" in {
      val underTest = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(contractorCISDeductions = None)))

      underTest.hasExclusivelyCustomerEoyCisDeductionsWith(aCisDeductions.employerRef,aPeriodData.deductionPeriod) shouldBe true
    }
  }

  ".toSubmissionWithoutPeriod" should {
    "return none when no data" in {
      val underTest = anIncomeTaxUserData.copy(cis = None)

      underTest.toSubmissionWithoutPeriod(aCisDeductions.employerRef,aPeriodData.deductionPeriod, taxYearEOY) shouldBe None
    }
    "return a submission model without the selected month" in {

      val mayPeriodData = aPeriodData.copy(deductionPeriod = Month.MAY)
      val julyPeriodData = aPeriodData.copy(deductionPeriod = Month.JULY)
      val cisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"))
      val cisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "ref-2", periodData = Seq(mayPeriodData, julyPeriodData))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions1, cisDeductions2))))

      val underTest = anIncomeTaxUserData.copy(
        cis = Some(allCISDeductions)
      )

      underTest.toSubmissionWithoutPeriod("ref-2",Month.MAY, taxYearEOY) shouldBe Some(
        CISSubmission(
          None,None,List(submission.PeriodData(
            deductionFromDate = "2021-06-06",
            deductionToDate = "2021-07-05",
            grossAmountPaid = Some(450.0),
            deductionAmount = 100.0,
            costOfMaterials = Some(50.0)
          )),Some("submissionId")
        )
      )
    }
  }

  ".contractorPeriodsFor" should {
    "return empty seq when no data" in {
      val underTest = anIncomeTaxUserData.copy(cis = None)

      underTest.contractorPeriodsFor("12345") shouldBe Seq.empty
    }
    "return list of months" in {
      val underTest = anIncomeTaxUserData

      underTest.contractorPeriodsFor(aCisDeductions.employerRef) shouldBe Seq(Month.MAY)
    }
  }

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

  "endOfYearCisDeductionsWith" should {
    "return CisDeductions when employerRef exists" in {
      val underTest = anIncomeTaxUserData

      underTest.endOfYearCisDeductionsWith(aCisDeductions.employerRef, aPeriodData.deductionPeriod) shouldBe Some(aCisDeductions)
    }

    "return no CisDeductions when employerRef does not exist" in {
      val underTest = anIncomeTaxUserData

      underTest.endOfYearCisDeductionsWith("12345", aPeriodData.deductionPeriod) shouldBe None
    }
  }

  ".hasEOYCisDeductionsWith(employerRef: String)" should {
    "return true when CisDeductions with employerRef exists" in {
      val cisDeductions = aCisDeductions.copy(employerRef = "some-ref")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = None, customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions))))

      val underTest = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      underTest.hasEoyCisDeductionsWith(employerRef = "some-ref") shouldBe true
    }

    "return false when CisDeductions with employerRef does not exist" in {
      val underTest = anIncomeTaxUserData

      underTest.hasEoyCisDeductionsWith(employerRef = "unknown-ref") shouldBe false
    }
  }

  ".hasEoyCisDeductionsWith(employerRef: String, month: Month)" should {
    "return false when CisDeductions with employerRef and month does not exist" in {
      val underTest = anIncomeTaxUserData

      underTest.hasEoyCisDeductionsWith(employerRef = "unknown-ref", aPeriodData.deductionPeriod) shouldBe false
    }

    "return true when CisDeductions with employerRef and month does exist" in {
      val underTest = anIncomeTaxUserData

      underTest.hasEoyCisDeductionsWith(aCisDeductions.employerRef, aPeriodData.deductionPeriod) shouldBe true
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

  ".eoyCisDeductionsWith" should {
    "extract the latest cisDeductions" in {
      val underTest = anIncomeTaxUserData

      underTest.eoyCisDeductionsWith(aCisDeductions.employerRef) shouldBe Some(aCisDeductions)
    }

    "extract the latest cisDeductions when no customer data" in {
      val underTest = anIncomeTaxUserData.copy(
        cis = Some(anAllCISDeductions.copy(
          customerCISDeductions = None
        ))
      )

      underTest.eoyCisDeductionsWith(aCisDeductions.employerRef) shouldBe Some(aCisDeductions)
    }

    "extract the latest cisDeductions when contractor data is latest" in {
      val underTest = anIncomeTaxUserData.copy(
        cis = Some(anAllCISDeductions.copy(
          customerCISDeductions = Some(aCISSource),
          contractorCISDeductions = Some(aCISSource.copy(
            cisDeductions = Seq(
              aCisDeductions.copy(
                periodData = Seq(aPeriodData, aPeriodData.copy(deductionPeriod = Month.DECEMBER, submissionDate = "2021-05-11T16:38:57.489Z"))
              )
            )
          ))
        ))
      )

      underTest.eoyCisDeductionsWith(aCisDeductions.employerRef) shouldBe Some(aCisDeductions.copy(
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

      underTest.eoyCisDeductionsWith(aCisDeductions.employerRef) shouldBe Some(aCisDeductions)
    }
  }

  ".customerCisDeductionsWith(employerRef)" should {
    "return customer deductions when exist for given employerRef" in {
      val deductions: CisDeductions = aCisDeductions.copy(employerRef = "some-employer-ref")
      val underTest = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))))

      underTest.customerCisDeductionsWith(employerRef = "some-employer-ref") shouldBe Some(deductions)
    }

    "return None when cis is None" in {
      val underTest = anIncomeTaxUserData.copy(cis = None)

      underTest.customerCisDeductionsWith(employerRef = "any-ref") shouldBe None
    }

    "return None when customerCisDeductions is None" in {
      val underTest = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(customerCISDeductions = None)))

      underTest.customerCisDeductionsWith(employerRef = "any-ref") shouldBe None
    }

    "return None when customerCisDeductions has empty deductions list" in {
      val underTest = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq.empty)))))

      underTest.customerCisDeductionsWith(employerRef = "any-ref") shouldBe None
    }

    "return None when customer deductions for given employerRef do not exist" in {
      val underTest = anIncomeTaxUserData

      underTest.customerCisDeductionsWith(employerRef = "unknown-employer-ref") shouldBe None
    }
  }
}
