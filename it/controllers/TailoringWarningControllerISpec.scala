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

package controllers

import controllers.routes.ContractorCYAController
import forms.YesNoForm
import models.RefreshIncomeSourceRequest
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.mvc.Http.Status.ACCEPTED
import support.IntegrationTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.builders.models.submission.CISSubmissionBuilder.aCISSubmission
import utils.ViewHelpers

class TailoringWarningControllerISpec extends IntegrationTest
  with ViewHelpers
  with BeforeAndAfterEach {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dropCISDB()
  }

  private def url(taxYear: Int): String = {
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/remove-all-cis"
  }

  ".show" should {
    "redirect to Deductions Summary when in year" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlGet(fullUrl(url(taxYear)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe controllers.routes.DeductionsSummaryController.show(taxYear).url
    }

    "return OK when EOY" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYearEOY)
        insertCyaData(aCisUserData)
        urlGet(fullUrl(url(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }
  }

  ".submit" should {
    "redirect to Deductions Summary when in year" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlPost(fullUrl(url(taxYear)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = "")
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe controllers.routes.DeductionsSummaryController.show(taxYear).url
    }

    "redirect to Deductions Summary after calling post exclude Cis when data is empty" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(cis = None), aUser.nino, taxYearEOY)
        excludePostStub(aUser.nino, taxYearEOY)
        urlPost(fullUrl(url(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "")
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe controllers.routes.DeductionsSummaryController.show(taxYearEOY).url
    }

    "redirect to Deductions Summary after calling post exclude Cis and remove cis when data is present" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYearEOY)
        stubDeleteWithoutResponseBody(s"/income-tax-cis/income-tax/nino/AA123456A/sources/submissionId\\?taxYear=$taxYearEOY",NO_CONTENT)
        stubPostWithoutResponseBody(s"/income-tax-cis/income-tax/nino/AA123456A/sources?taxYear=$taxYearEOY",OK, Json.toJson(aCisDeductions.toCISSubmission(taxYearEOY)).toString())
        stubPutWithoutResponseBody(s"/income-tax-cis/income-tax/nino/AA123456A/sources\\?taxYear=$taxYearEOY", Json.toJson(RefreshIncomeSourceRequest("cis")).toString, NO_CONTENT)
        auditStubs()
        stubPost(s"/income-tax-nrs-proxy/${aUser.nino}/itsa-personal-income-submission", ACCEPTED, "{}")
        excludePostStub(aUser.nino, taxYearEOY)
        urlPost(fullUrl(url(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "")
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe controllers.routes.DeductionsSummaryController.show(taxYearEOY).url
    }

  }
}
