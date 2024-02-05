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

import forms.YesNoForm
import models.tailoring.{ExcludeJourneyModel, ExcludedJourneysResponseModel}
import org.scalatest.BeforeAndAfterEach
import play.api.{Application, Environment, Mode}
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, writeableOf_AnyContentAsFormUrlEncoded}
import support.IntegrationTest
import support.builders.models.UserBuilder.aUser
import utils.ViewHelpers

import scala.concurrent.Future

class DeductionsFromPaymentsControllerISpec extends IntegrationTest
  with ViewHelpers
  with BeforeAndAfterEach {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  val csrfContent: (String, String) = "Csrf-Token" -> "nocheck"

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dropCISDB()
  }

  private def url(taxYear: Int): String = {
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/deductions-from-payments"
  }

  ".show" should {
    "redirect to income tax submission overview when tailoring is disabled" in {
      val request = FakeRequest("GET", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(app, request, "{}").get
      }
      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }

    "return OK when tailoring is enabled" in {
      val request = FakeRequest("GET", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        excludeStub(ExcludedJourneysResponseModel(Seq[ExcludeJourneyModel]().empty),"1234567890", taxYearEOY)
        route(appWithTailoring, request, "{}").get
      }

      status(result) shouldBe OK
    }
    "return INTERNAL_SERVER_ERROR when tailoring is enabled and exclude call fails" in {
      val request = FakeRequest("GET", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        stubGetWithHeadersCheck(
          url = s"/income-tax-submission-service/income-tax/nino/1234567890/sources/excluded-journeys/$taxYearEOY",
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

  ".submit" should {
    "redirect to income tax submission overview when tailoring is disabled" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), csrfContent)

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(app, request, "{}").get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }

    "redirect to deductions summary when tailoring is enabled and 'Yes' is selected" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "true")

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        excludeStub(ExcludedJourneysResponseModel(Seq[ExcludeJourneyModel]().empty),"AA123456A", taxYearEOY)
        excludeClearStub("AA123456A", taxYearEOY)
        route(appWithTailoring, request).get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe controllers.routes.DeductionsSummaryController.show(taxYearEOY).url
    }
    "return INTERNAL_SERVER_ERROR when excludeClearFails" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "true")

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        excludeStub(ExcludedJourneysResponseModel(Seq[ExcludeJourneyModel]().empty),"AA123456A", taxYearEOY)
          stubPostWithoutResponseAndRequestBody(
            url = s"/income-tax-submission-service/income-tax/nino/AA123456A/sources/clear-excluded-journeys/$taxYearEOY",
            status = INTERNAL_SERVER_ERROR,
          )
        route(appWithTailoring, request).get
      }

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to income tax submission overview when tailoring is enabled and 'No' is selected" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "false")

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        excludeStub(ExcludedJourneysResponseModel(Seq[ExcludeJourneyModel]().empty),"1234567890", taxYearEOY)
        route(appWithTailoring, request).get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe controllers.routes.TailorCisWarningController.show(taxYearEOY).url
    }
    "return INTERNAL_SERVER_ERROR when excludeGetFails" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "false")

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        stubGetWithHeadersCheck(
          url = s"/income-tax-submission-service/income-tax/nino/1234567890/sources/excluded-journeys/$taxYearEOY",
          status = INTERNAL_SERVER_ERROR,
          responseBody = "",
          sessionHeader = "X-Session-ID" -> aUser.sessionId,
          mtdidHeader = "mtditid" -> aUser.mtditid
        )
        route(appWithTailoring, request).get
      }

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to deductions summary when tailoring is enabled and 'No' is selected and cis is excluded" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "false")

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        excludeStub(ExcludedJourneysResponseModel(Seq[ExcludeJourneyModel](ExcludeJourneyModel("cis", None))),"1234567890", taxYearEOY)
        route(appWithTailoring, request).get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe controllers.routes.DeductionsSummaryController.show(taxYearEOY).url
    }

    "return a badRequest with an invalid form" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), csrfContent)
        .withFormUrlEncodedBody("" -> "false")

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        excludeStub(ExcludedJourneysResponseModel(Seq[ExcludeJourneyModel]().empty),"1234567890", taxYearEOY)
        route(appWithTailoring, request).get
      }

      status(result) shouldBe BAD_REQUEST
    }
  }
}
