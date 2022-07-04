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

package actions

import config.MockAppConfig
import controllers.routes.DeductionPeriodController
import models.UserSessionDataRequest
import models.mongo.DataNotFoundError
import play.api.mvc.Results.{InternalServerError, Redirect}
import support.UnitTest
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockCISSessionService, MockErrorHandler}

import scala.concurrent.ExecutionContext

class CisUserDataRefinerActionSpec extends UnitTest
  with MockCISSessionService
  with MockErrorHandler {

  private val taxYear = 2022
  private val employerRef = "some-employer-ref"
  private val appConfig = new MockAppConfig().config()
  private val executionContext = ExecutionContext.global

  private def createAction(redirectIfPrior: Boolean = false) = CisUserDataRefinerAction(
    taxYear = taxYear,
    employerRef = employerRef,
    cisSessionService = mockCISSessionService,
    errorHandler = mockErrorHandler,
    appConfig = appConfig,
    needsPeriodData = true,
    redirectIfPrior = redirectIfPrior
  )(executionContext)


  private val underTest = createAction()


  ".executionContext" should {
    "return the given execution context" in {
      underTest.executionContext shouldBe executionContext
    }
  }

  ".refine" should {
    "handle InternalServerError when when getting session data result in database error" in {
      mockGetSessionData(taxYear, anAuthorisationRequest.user, employerRef, Left(DataNotFoundError))
      mockInternalError(InternalServerError)

      await(underTest.refine(anAuthorisationRequest)) shouldBe Left(InternalServerError)
    }

    "return a redirect to Income Tax Submission Overview when session data is None" in {
      mockGetSessionData(taxYear, anAuthorisationRequest.user, employerRef, Right(None))

      await(underTest.refine(anAuthorisationRequest)) shouldBe Left(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }

    "return a redirect to Deduction Period Page when session data has no PeriodData" in {
      val cisUserDataWithoutPeriodData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None), employerRef = employerRef, taxYear = taxYear)

      mockGetSessionData(taxYear, anAuthorisationRequest.user, employerRef, Right(Some(cisUserDataWithoutPeriodData)))

      await(underTest.refine(anAuthorisationRequest)) shouldBe Left(Redirect(DeductionPeriodController.show(taxYear, employerRef)))
    }

    "return UserSessionDataRequest when period data exists and redirectIfPrior is false" in {
      val cisUserData = aCisUserData.copy(employerRef = employerRef, taxYear = taxYear)

      mockGetSessionData(taxYear, anAuthorisationRequest.user, employerRef, Right(Some(cisUserData)))

      await(underTest.refine(anAuthorisationRequest)) shouldBe Right(UserSessionDataRequest(cisUserData, anAuthorisationRequest.user, anAuthorisationRequest.request))
    }

    "return a redirect to Income Tax Overview Page when period data is a prior submission and redirectIfPrior is true" in {
      val cisUserData = aCisUserData.copy(employerRef = employerRef, taxYear = taxYear, isPriorSubmission = true)
      val underTest = createAction(redirectIfPrior = true)

      mockGetSessionData(taxYear, anAuthorisationRequest.user, employerRef, Right(Some(cisUserData)))

      await(underTest.refine(anAuthorisationRequest)) shouldBe Left(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }
}
