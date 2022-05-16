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

import common.SessionValues
import config.AppConfig
import models.AuthorisationRequest
import org.scalamock.scalatest.MockFactory
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.Helpers.status
import play.api.test.FakeRequest
import support.{TaxYearProvider, UnitTest}
import support.builders.models.UserBuilder.aUser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxYearActionSpec extends UnitTest
  with MockFactory with TaxYearProvider {

  private val validTaxYear: Int = validTaxYearList.head
  private val taxYearNotInSession: Int = validTaxYearList.find(_ != validTaxYear).get
  private val invalidTaxYear: Int = validTaxYearList.last + 1

  private val validTaxYears = validTaxYearList.mkString(",")

  private val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("X-Session-ID" -> aUser.sessionId)
  private implicit lazy val mockedConfig: AppConfig = mock[AppConfig]
  private implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] = new AuthorisationRequest[AnyContent](aUser, fakeRequest)

  private def taxYearAction(taxYear: Int): TaxYearAction = new TaxYearAction(taxYear)

  private def redirectUrl(future: Future[Result]): String = {
    await(future).header.headers.getOrElse("Location", "/")
  }

  "TaxYearAction.refine" should {
    val request = fakeRequest.withSession(SessionValues.TAX_YEAR -> validTaxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears)
    "return a Right(request)" when {
      "the tax year is within the list of valid tax years, and the tax year is equal to the session value if the feature switch is on" in {
        lazy val userRequest = AuthorisationRequest(aUser, request)
        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning true

          await(taxYearAction(validTaxYear).refine(userRequest))
        }

        result.isRight shouldBe true
      }

      "the tax year is within the list of valid tax years, and the tax year is equal to the session value if the feature switch is off" in {
        lazy val userRequest = AuthorisationRequest(
          aUser,
          fakeRequest.withSession(SessionValues.TAX_YEAR -> validTaxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears)
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning false

          await(taxYearAction(validTaxYear).refine(userRequest))
        }

        result.isRight shouldBe true
      }
    }

    "return a Left(result)" when {
      "no valid tax years exist in session" which {
        lazy val userRequest = AuthorisationRequest(
          aUser,
          fakeRequest.withSession(SessionValues.TAX_YEAR -> validTaxYear.toString)
        )

        lazy val result = {
          mockedConfig.incomeTaxSubmissionStartUrl _ expects validTaxYear returning
            "controllers.routes.StartPageController.show(validTaxYear).url"

          taxYearAction(validTaxYear).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.get)) shouldBe SEE_OTHER
        }

        "has the start page redirect url" in {
          redirectUrl(result.map(_.left.get)) shouldBe "controllers.routes.StartPageController.show(validTaxYear).url"
        }
      }

      "the tax year is different from that in session and the feature switch is off" which {
        lazy val userRequest = AuthorisationRequest(
          aUser,
          request
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning false
          mockedConfig.incomeTaxSubmissionOverviewUrl _ expects taxYearNotInSession returning
            "controllers.routes.OverviewPageController.show(taxYearNotInSession).url"

          taxYearAction(taxYearNotInSession).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.get)) shouldBe SEE_OTHER
        }

        "has the overview page redirect url" in {
          redirectUrl(result.map(_.left.get)) shouldBe "controllers.routes.OverviewPageController.show(taxYearNotInSession).url"
        }

        "has an updated tax year session value" in {
          await(result.map(_.left.get)).session.get(SessionValues.TAX_YEAR).get shouldBe taxYearNotInSession.toString
        }
      }

      "the tax year is outside list of valid tax years and the feature switch is on" which {
        lazy val userRequest = AuthorisationRequest(
          aUser,
          request
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning true

          taxYearAction(invalidTaxYear).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.get)) shouldBe SEE_OTHER
        }

        "has the tax year error page redirect url" in {
          redirectUrl(result.map(_.left.get)) shouldBe controllers.errors.routes.TaxYearErrorController.show.url
        }
      }
    }
  }
}
