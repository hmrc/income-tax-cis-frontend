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
import models.pages.DeductionsSummaryPage
import models.pages.DeductionsSummaryPage.mapToInYearPage
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import support.mocks.MockIncomeTaxUserDataConnector
import support.{TaxYearHelper, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier
import utils.InYearUtil

import scala.concurrent.ExecutionContext.Implicits.global

class DeductionsSummaryServiceSpec extends UnitTest
  with MockIncomeTaxUserDataConnector
  with TaxYearHelper {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val underTest = new DeductionsSummaryService(
    inYearUtil = new InYearUtil(),
    mockIncomeTaxUserDataConnector
  )

  ".pageModelFor" should {
    "return page with empty deductions when EOY" in {
      await(underTest.pageModelFor(taxYearEOY, aUser)) shouldBe Right(DeductionsSummaryPage(taxYearEOY, isInYear = false, Seq.empty))
    }

    "return error when in year and incomeTaxUserDataConnector getUserData errors with IncomeTaxUserDataHttpParserError" in {
      mockGetUserData(aUser.nino, taxYear, Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      await(underTest.pageModelFor(taxYear, aUser)) shouldBe Left(IncomeTaxUserDataHttpParserError(INTERNAL_SERVER_ERROR))
    }

    "return EmptyCisDataError when in year and incomeTaxUserDataConnector returns userData with empty cis" in {
      mockGetUserData(aUser.nino, taxYear, Right(IncomeTaxUserData(None)))

      await(underTest.pageModelFor(taxYear, aUser)) shouldBe Left(EmptyCisDataError)
    }

    "return EmptyInYearDeductionsError when in year and incomeTaxUserDataConnector returns userData errors with empty Constructor CIS Deductions" in {
      val userDataWithEmptyContractorDeductions = anAllCISDeductions.copy(contractorCISDeductions = None)
      mockGetUserData(aUser.nino, taxYear, Right(IncomeTaxUserData(cis = Some(userDataWithEmptyContractorDeductions))))

      await(underTest.pageModelFor(taxYear, aUser)) shouldBe Left(EmptyInYearDeductionsError)
    }

    "return EmptyInYearDeductionsError when in year and incomeTaxUserDataConnector returns userData errors with empty Constructor CIS Deductions list" in {
      val userDataWithEmptyContractorCisDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq.empty)))
      mockGetUserData(aUser.nino, taxYear, Right(IncomeTaxUserData(cis = Some(userDataWithEmptyContractorCisDeductions))))

      await(underTest.pageModelFor(taxYear, aUser)) shouldBe Left(EmptyInYearDeductionsError)
    }

    "return page with deductions when in year and incomeTaxUserDataConnector getUserData succeeds" in {
      mockGetUserData(aUser.nino, taxYear, Right(anIncomeTaxUserData))

      await(underTest.pageModelFor(taxYear, aUser)) shouldBe Right(mapToInYearPage(taxYear, anIncomeTaxUserData))
    }
  }
}
