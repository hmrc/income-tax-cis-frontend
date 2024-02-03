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

package models

import support.UnitTest
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData

import java.time.Month

class AllCISDeductionsSpec extends UnitTest {

  "contractorPeriodsFor" should {
    "get months for an employer" in {
      anAllCISDeductions.contractorPeriodsFor(aCisDeductions.employerRef) shouldBe Seq(Month.MAY)
    }
    "return an empty list when no data" in {
      anAllCISDeductions.contractorPeriodsFor("12345555") shouldBe Seq.empty
    }
  }

  "allEmployerRefs" should {
    "return an empty list when no data" in {
      anAllCISDeductions.copy(None, None).allEmployerRefs shouldBe Seq.empty
    }
    "return a list when contractor data exists" in {
      anAllCISDeductions.copy(customerCISDeductions = None).allEmployerRefs shouldBe Seq(aCisDeductions.employerRef)
    }
    "return a list when customer data exists" in {
      anAllCISDeductions.copy(contractorCISDeductions = None).allEmployerRefs shouldBe Seq(aCisDeductions.employerRef)
    }
    "return a list when both sources of data exist" in {
      anAllCISDeductions.copy(contractorCISDeductions = Some(
        aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(employerRef = "4567"))))).allEmployerRefs shouldBe Seq(aCisDeductions.employerRef, "4567")
    }
  }

  ".endOfYearCisDeductions" should {
    "return customer data" in {
      val underTest = AllCISDeductions(
        customerCISDeductions = Some(aCISSource),
        contractorCISDeductions = None)

      underTest.endOfYearCisDeductions shouldBe Seq(aCisDeductions)
    }

    "return contractor data" in {
      val underTest = AllCISDeductions(
        customerCISDeductions = None,
        contractorCISDeductions = Some(aCISSource))

      underTest.endOfYearCisDeductions shouldBe Seq(aCisDeductions)
    }

    "return no data" in {
      val underTest = AllCISDeductions(
        customerCISDeductions = None,
        contractorCISDeductions = None)

      underTest.endOfYearCisDeductions shouldBe Seq()
    }

    "return a full list that contains both contractor and customer data" in {
      val underTest = AllCISDeductions(
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(
          aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(aPeriodData)),
          aCisDeductions.copy(employerRef = "222", contractorName = Some("Builders R Us"), periodData = Seq(aPeriodData))
        ))),
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(
          aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(aPeriodData.copy(deductionPeriod = Month.DECEMBER))),
          aCisDeductions.copy(employerRef = "222", contractorName = Some("Builders R Us"), periodData = Seq(
            aPeriodData.copy(costOfMaterials = Some(9600.50), submissionDate = "2022-05-11T16:38:57.489Z")
          ))
        )))
      )

      underTest.endOfYearCisDeductions shouldBe Seq(
        aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(
          aPeriodData, aPeriodData.copy(deductionPeriod = Month.DECEMBER)
        ),
          totalCostOfMaterials = Some(100.00),
          totalDeductionAmount = Some(200.00),
          totalGrossAmountPaid = Some(900.00)
        ),
        aCisDeductions.copy(employerRef = "222", contractorName = Some("Builders R Us"), periodData = Seq(
          aPeriodData.copy(costOfMaterials = Some(9600.50), submissionDate = "2022-05-11T16:38:57.489Z")
        ),
          totalDeductionAmount = Some(100.00),
          totalCostOfMaterials = Some(9600.50),
          totalGrossAmountPaid = Some(450.00)
        )
      )
    }

    "return a full list that contains both contractor and customer periods" in {
      val underTest = AllCISDeductions(
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(
          aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(
            aPeriodData, aPeriodData.copy(deductionPeriod = Month.OCTOBER), aPeriodData.copy(deductionPeriod = Month.MARCH, deductionAmount = Some(30000))
          ))
        ))),
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(
          aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(
            aPeriodData, aPeriodData.copy(deductionPeriod = Month.DECEMBER), aPeriodData.copy(deductionPeriod = Month.MARCH, deductionAmount = Some(40000))
          ))
        )))
      )

      underTest.endOfYearCisDeductions shouldBe Seq(
        aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(
          aPeriodData, aPeriodData.copy(deductionPeriod = Month.OCTOBER), aPeriodData.copy(deductionPeriod = Month.DECEMBER),
          aPeriodData.copy(deductionPeriod = Month.MARCH, deductionAmount = Some(30000))
        ),
          totalCostOfMaterials = Some(200.00),
          totalDeductionAmount = Some(30300.00),
          totalGrossAmountPaid = Some(1800.00)
        )
      )
    }

    "return a full list that contains both contractor and customer data when customer data is the latest" in {
      val underTest = AllCISDeductions(
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(
          aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(aPeriodData)),
          aCisDeductions.copy(employerRef = "222", contractorName = Some("Builders R Us"), periodData = Seq(aPeriodData.copy(
            submissionDate = "2022-05-11T16:38:57.489Z"
          )))
        ))),
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(
          aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(aPeriodData.copy(deductionPeriod = Month.DECEMBER))),
          aCisDeductions.copy(employerRef = "222", contractorName = Some("Builders R Us"), periodData = Seq(
            aPeriodData.copy(costOfMaterials = Some(9600.50)
            ))
          )))
        ))

      underTest.endOfYearCisDeductions shouldBe Seq(
        aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(
          aPeriodData, aPeriodData.copy(deductionPeriod = Month.DECEMBER)
        ),
          totalDeductionAmount = Some(200.00),
          totalCostOfMaterials = Some(100.00),
          totalGrossAmountPaid = Some(900.00)
        ),
        aCisDeductions.copy(employerRef = "222", contractorName = Some("Builders R Us"), periodData = Seq(
          aPeriodData.copy(submissionDate = "2022-05-11T16:38:57.489Z")
        ),
          totalDeductionAmount = Some(100.00),
          totalCostOfMaterials = Some(50.00),
          totalGrossAmountPaid = Some(450.00)
        )
      )
    }

    "return a full list that contains both contractor and customer data when no data overlaps and should be sorted by name" in {
      val underTest = AllCISDeductions(
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(
          aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(aPeriodData)),
          aCisDeductions.copy(employerRef = "222", contractorName = Some("Builders R Us"), periodData = Seq(aPeriodData))
        ))),
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(
          aCisDeductions.copy(employerRef = "333", contractorName = Some("Varnish Finish"), periodData = Seq(aPeriodData.copy(deductionPeriod = Month.DECEMBER))),
          aCisDeductions.copy(employerRef = "444", contractorName = Some("Mega Drills"), periodData = Seq(
            aPeriodData.copy(costOfMaterials = Some(9600.50), submissionDate = "2022-05-11T16:38:57.489Z")
          ))
        )))
      )

      underTest.endOfYearCisDeductions shouldBe Seq(
        aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(aPeriodData)),
        aCisDeductions.copy(employerRef = "222", contractorName = Some("Builders R Us"), periodData = Seq(aPeriodData)),
        aCisDeductions.copy(employerRef = "444", contractorName = Some("Mega Drills"), periodData = Seq(
          aPeriodData.copy(costOfMaterials = Some(9600.50), submissionDate = "2022-05-11T16:38:57.489Z")
        ), totalCostOfMaterials = Some(9600.50)),
        aCisDeductions.copy(employerRef = "333", contractorName = Some("Varnish Finish"), periodData = Seq(aPeriodData.copy(deductionPeriod = Month.DECEMBER)))
      )
    }

    "return a full list that contains both contractor and customer data when no month data overlaps and should be sorted by month" in {
      val underTest = AllCISDeductions(
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(
          aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(
            aPeriodData.copy(deductionPeriod = Month.FEBRUARY),
            aPeriodData.copy(deductionPeriod = Month.OCTOBER)
          ))
        ))),
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(
          aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(
            aPeriodData.copy(deductionPeriod = Month.DECEMBER),
            aPeriodData.copy(deductionPeriod = Month.NOVEMBER),
            aPeriodData.copy(deductionPeriod = Month.MARCH),
          ))
        )))
      )

      underTest.endOfYearCisDeductions shouldBe Seq(
        aCisDeductions.copy(employerRef = "111", contractorName = Some("All Builders"), periodData = Seq(
          aPeriodData.copy(deductionPeriod = Month.OCTOBER),
          aPeriodData.copy(deductionPeriod = Month.NOVEMBER),
          aPeriodData.copy(deductionPeriod = Month.DECEMBER),
          aPeriodData.copy(deductionPeriod = Month.FEBRUARY),
          aPeriodData.copy(deductionPeriod = Month.MARCH)
        ),
          totalCostOfMaterials = Some(250.00),
          totalDeductionAmount = Some(500.00),
          totalGrossAmountPaid = Some(2250.00)
        )
      )
    }
  }

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

  ".eoyCisDeductionsWith(empRef)" should {
    "return CisDeductions with given employer reference when exists" in {
      val aCisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"), employerRef = "ref-1")
      val aCisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "ref-2")
      val underTest = AllCISDeductions(
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions1, aCisDeductions2))),
        contractorCISDeductions = None
      )

      underTest.eoyCisDeductionsWith(employerRef = "ref-2") shouldBe Some(aCisDeductions2)
    }

    "return none when no CiwDeductions for a reference exists" in {
      val aCisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"), employerRef = "ref-1")
      val aCisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "ref-2")
      val underTest = anAllCISDeductions.copy(
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions1, aCisDeductions2))),
        contractorCISDeductions = Some(aCISSource)
      )

      underTest.eoyCisDeductionsWith(employerRef = "unknown-ref") shouldBe None
    }
  }

  ".customerCisDeductionsWith(employerRef)" should {
    "return customer cis deductions with give employerRef when exists" in {
      val deductions = aCisDeductions.copy(employerRef = "known-employer-ref")
      val underTest = anAllCISDeductions.copy(customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      underTest.customerCisDeductionsWith(employerRef = "known-employer-ref") shouldBe Some(deductions)
    }

    "return None when customerCISDeductions is None" in {
      val underTest = anAllCISDeductions.copy(customerCISDeductions = None)

      underTest.customerCisDeductionsWith(employerRef = "any-ref") shouldBe None
    }

    "return None when customerCISDeductions with given employerRef do not exist" in {
      val underTest = anAllCISDeductions

      underTest.customerCisDeductionsWith(employerRef = "unknown-ref") shouldBe None
    }
  }

  ".contractorCisDeductionsWith(employerRef)" should {
    "return contractor cis deductions with give employerRef when exists" in {
      val deductions = aCisDeductions.copy(employerRef = "known-employer-ref")
      val underTest = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      underTest.contractorCisDeductionsWith(employerRef = "known-employer-ref") shouldBe Some(deductions)
    }

    "return None when contractorCISDeductions is None" in {
      val underTest = anAllCISDeductions.copy(contractorCISDeductions = None)

      underTest.contractorCisDeductionsWith(employerRef = "any-ref") shouldBe None
    }

    "return None when contractorCISDeductions with given employerRef do not exist" in {
      val underTest = anAllCISDeductions

      underTest.contractorCisDeductionsWith(employerRef = "unknown-ref") shouldBe None
    }
  }
}
