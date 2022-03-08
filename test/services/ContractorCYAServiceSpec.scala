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

package services

import models._
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import support.builders.models.pages.ContractorCYAPageBuilder.aContractorCYAPage
import support.mocks.MockIncomeTaxUserDataConnector
import support.{TaxYearHelper, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier
import utils.InYearUtil

import java.time.Month
import scala.concurrent.ExecutionContext.Implicits.global

class ContractorCYAServiceSpec extends UnitTest
  with MockIncomeTaxUserDataConnector
  with TaxYearHelper {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val underTest = new ContractorCYAService(
    inYearUtil = new InYearUtil(),
    mockIncomeTaxUserDataConnector
  )

  ".pageModelFor" should {
    "return error when in year and incomeTaxUserDataConnector getUserData errors with HttpParserError" in {
      mockGetUserData(aUser.nino, taxYear, Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      await(underTest.pageModelFor(taxYear, Month.MAY, refNumber = "some-ref", user = aUser)) shouldBe Left(HttpParserError(INTERNAL_SERVER_ERROR))
    }

    "return EmptyCisDataError when in year and incomeTaxUserDataConnector returns userData with empty cis" in {
      mockGetUserData(aUser.nino, taxYear, Right(IncomeTaxUserData(None)))

      await(underTest.pageModelFor(taxYear, Month.MAY, refNumber = "some-ref", aUser)) shouldBe Left(EmptyPriorCisDataError)
    }

    "return EmptyInYearDeductionsError when in year and incomeTaxUserDataConnector returns userData with empty Constructor CIS Deductions" in {
      val userDataWithEmptyContractorDeductions = anAllCISDeductions.copy(contractorCISDeductions = None)
      mockGetUserData(aUser.nino, taxYear, Right(IncomeTaxUserData(cis = Some(userDataWithEmptyContractorDeductions))))

      await(underTest.pageModelFor(taxYear, Month.MAY, refNumber = "some-ref", aUser)) shouldBe Left(EmptyInYearDeductionsError)
    }

    "return EmptyInYearDeductionsError when in year and incomeTaxUserDataConnector returns userData with empty Constructor CIS Deductions list" in {
      val userDataWithEmptyContractorCisDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq.empty)))
      mockGetUserData(aUser.nino, taxYear, Right(IncomeTaxUserData(cis = Some(userDataWithEmptyContractorCisDeductions))))

      await(underTest.pageModelFor(taxYear, Month.MAY, refNumber = "some-ref", aUser)) shouldBe Left(EmptyInYearDeductionsError)
    }

    "return EmployerRefNotFoundError when in year and incomeTaxUserDataConnector returns userData but employerRef does not exist" in {
      mockGetUserData(aUser.nino, taxYear, Right(IncomeTaxUserData(cis = Some(anAllCISDeductions))))

      await(underTest.pageModelFor(taxYear, Month.MAY, refNumber = "unknown-ref", aUser)) shouldBe Left(EmployerRefNotFoundError)
    }

    "return DeductionPeriodNotFoundError when in year and incomeTaxUserDataConnector returns userData but deduction period does not exist exist" in {
      val periodData = aPeriodData.copy(deductionPeriod = Month.MAY)
      val cisSource = aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(employerRef = "12345", periodData = Seq(periodData))))
      mockGetUserData(aUser.nino, taxYear, Right(IncomeTaxUserData(cis = Some(anAllCISDeductions.copy(contractorCISDeductions = Some(cisSource))))))

      await(underTest.pageModelFor(taxYear, Month.JULY, refNumber = "12345", aUser)) shouldBe Left(DeductionPeriodNotFoundError)
    }

    "return page with deductions when in year and incomeTaxUserDataConnector getUserData succeeds" in {
      val periodData = aPeriodData.copy(deductionPeriod = Month.JUNE, grossAmountPaid = Some(101), deductionAmount = Some(201), costOfMaterials = Some(301))
      val cisDeductions = aCisDeductions.copy(contractorName = Some("some-contractor"), employerRef = "some-employer-ref", periodData = Seq(aPeriodData, periodData))
      val cisSource = aCISSource.copy(cisDeductions = Seq(aCisDeductions, cisDeductions))
      mockGetUserData(aUser.nino, taxYear, Right(anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(contractorCISDeductions = Some(cisSource))))))

      val expectedPage = aContractorCYAPage.copy(
        taxYear = taxYear,
        isInYear = true,
        contractorName = Some("some-contractor"),
        employerRef = "some-employer-ref",
        month = Month.JUNE,
        labourAmount = Some(101),
        deductionAmount = Some(201),
        costOfMaterials = Some(301)
      )

      await(underTest.pageModelFor(taxYear, Month.JUNE, refNumber = "some-employer-ref", aUser)) shouldBe Right(expectedPage)
    }
  }
}
