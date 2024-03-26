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

import models.tailoring.{ExcludeJourneyModel, ExcludedJourneysResponseModel}
import play.api.http.HeaderNames
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import support.IntegrationTest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import utils.ViewHelpers

import scala.concurrent.Future

class DeductionsSummaryControllerISpec extends IntegrationTest with ViewHelpers{

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private def url(taxYear: Int): String = s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/summary"

  ".show" should {
    "Render in year Deductions Summary page" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        excludeStub(ExcludedJourneysResponseModel(Seq()),aUser.nino, taxYear)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlGet(fullUrl(url(taxYear)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe OK
    }

    "Render end of year Deductions Summary page" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        excludeStub(ExcludedJourneysResponseModel(Seq()),aUser.nino, taxYearEOY)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYearEOY)
        urlGet(fullUrl(url(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }

    "return OK when tailoring is enabled" in {
      val request = FakeRequest("GET", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        excludeStub(ExcludedJourneysResponseModel(Seq[ExcludeJourneyModel]().empty), "1234567890", taxYearEOY)
        route(appWithTailoring, request, "{}").get
      }

      status(result) shouldBe OK
    }

    "return OK when tailoring is enabled and cis is excluded" in {
      val request = FakeRequest("GET", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        excludeStub(ExcludedJourneysResponseModel(Seq[ExcludeJourneyModel](ExcludeJourneyModel("cis", None))), "1234567890", taxYearEOY)
        route(appWithTailoring, request, "{}").get
      }

      status(result) shouldBe OK
    }

    "return in when tailoring is enabled and exclude call fails" in {
      val request = FakeRequest("GET", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        stubGetWithHeadersCheck(
          url = s"/income-tax-submission-service/income-tax/nino/AA123456A/sources/excluded-journeys/$taxYearEOY",
          status = INTERNAL_SERVER_ERROR,
          responseBody = "",
          sessionHeader = "X-Session-ID" -> aUser.sessionId,
          mtdidHeader = "mtditid" -> aUser.mtditid
        )
        route(appWithTailoring, request, "{}").get
      }

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
