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
import play.api.mvc.Results.Redirect
import support.UnitTest
import support.builders.models.UserSessionDataRequestBuilder.aUserSessionDataRequest
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

import scala.concurrent.ExecutionContext

class CisUserDataFilterActionSpec extends UnitTest {

  private val taxYear = 2022
  private val employerRef = "some-employer-ref"
  private val appConfig = new MockAppConfig().config()
  private val executionContext = ExecutionContext.global

  private def createAction(redirectIfPrior: Boolean = false) = CisUserDataFilterAction(
    taxYear = taxYear,
    employerRef = employerRef,
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

  ".filter" should {
    "return a redirect to Deduction Period Page when session data has no PeriodData" in {
      val cisUserDataWithoutPeriodData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None), employerRef = employerRef, taxYear = taxYear)
      val inputRequest = aUserSessionDataRequest.copy(cisUserData = cisUserDataWithoutPeriodData)

      await(underTest.filter(inputRequest)) shouldBe Some(Redirect(DeductionPeriodController.show(taxYear, employerRef)))
    }

    "return UserSessionDataRequest when period data exists and redirectIfPrior is false" in {
      val cisUserData = aCisUserData.copy(employerRef = employerRef, taxYear = taxYear)
      val inputRequest = aUserSessionDataRequest.copy(cisUserData = cisUserData)

      await(underTest.filter(inputRequest)) shouldBe None
    }

    "return a redirect to Income Tax Overview Page when period data is a prior submission and redirectIfPrior is true" in {
      val cisUserData = aCisUserData.copy(employerRef = employerRef, taxYear = taxYear, isPriorSubmission = true)
      val inputRequest = aUserSessionDataRequest.copy(cisUserData = cisUserData)
      val underTest = createAction(redirectIfPrior = true)

      await(underTest.filter(inputRequest)) shouldBe Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }
}
