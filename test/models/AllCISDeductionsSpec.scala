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
import support.builders.models.PeriodDataBuilder.aPeriodData

class AllCISDeductionsSpec extends UnitTest {

  ".endOfYearCisDeductions" should {
    "return customer data" in {
      val underTest = AllCISDeductions(
        customerCISDeductions = Some(aCISSource),
        contractorCISDeductions =None)

      underTest.endOfYearCisDeductions shouldBe Seq(aCisDeductions)
    }
    "return contractor data" in {
      val underTest = AllCISDeductions(
        customerCISDeductions = None,
        contractorCISDeductions =  Some(aCISSource))

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
}
