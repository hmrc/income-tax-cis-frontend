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
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.Helpers.{await, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import support.UnitTest
import support.builders.models.UserBuilder.aUser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxYearActionSpec extends UnitTest
  with MockFactory {

  private val validTaxYear: Int = 2022
  private val invalidTaxYear: Int = 3000

  private val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("X-Session-ID" -> aUser.sessionId)
  private implicit lazy val mockedConfig: AppConfig = mock[AppConfig]
  private val mockControllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  private implicit lazy val cc: MessagesApi = mockControllerComponents.messagesApi
  private implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] = new AuthorisationRequest[AnyContent](aUser, fakeRequest)

  private def taxYearAction(taxYear: Int, reset: Boolean = true): TaxYearAction = new TaxYearAction(taxYear, reset)

  private def redirectUrl(awaitable: Future[Result]): String = {
    await(awaitable).header.headers.getOrElse("Location", "/")
  }

  "TaxYearAction.refine" should {
    val request = fakeRequest.withSession(SessionValues.TAX_YEAR -> validTaxYear.toString)
    "return a Right(request)" when {
      "the tax year is within range of allowed years, and matches that in session if the feature switch is on" in {
        lazy val userRequest = AuthorisationRequest(aUser, request)
        lazy val result = {
          mockedConfig.defaultTaxYear _ expects() returning validTaxYear
          mockedConfig.taxYearErrorFeature _ expects() returning true

          await(taxYearAction(validTaxYear).refine(userRequest))
        }

        result.isRight shouldBe true
      }

      "the tax year is equal to the session value if the feature switch is off" in {
        lazy val userRequest = AuthorisationRequest(
          aUser,
          fakeRequest.withSession(SessionValues.TAX_YEAR -> (validTaxYear + 1).toString)
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning false

          await(taxYearAction(validTaxYear + 1).refine(userRequest))
        }

        result.isRight shouldBe true
      }

      "the tax year is different to the session value if the reset variable input is false" in {
        lazy val userRequest = AuthorisationRequest(aUser, request)

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning false

          await(taxYearAction(validTaxYear + 1, reset = false).refine(userRequest))
        }

        result.isRight shouldBe true
      }
    }

    "return a Left(result)" when {
      "the tax year is different from that in session and the feature switch is off" which {
        lazy val userRequest = AuthorisationRequest(
          aUser,
          request
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning false
          mockedConfig.incomeTaxSubmissionOverviewUrl _ expects (validTaxYear + 1) returning
            "controllers.routes.StartPageController.show(validTaxYear + 1).url"

          taxYearAction(validTaxYear + 1).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.get)) shouldBe SEE_OTHER
        }

        "has the start page redirect url" in {
          redirectUrl(result.map(_.left.get)) shouldBe "controllers.routes.StartPageController.show(validTaxYear + 1).url"
        }

        "has an updated tax year session value" in {
          await(result.map(_.left.get)).session.get(SessionValues.TAX_YEAR).get shouldBe (validTaxYear + 1).toString
        }
      }

      "the tax year is outside of the allowed limit while the feature switch is on" which {
        lazy val userRequest = AuthorisationRequest(aUser, request)

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning true
          mockedConfig.defaultTaxYear _ expects() returning invalidTaxYear twice()

          taxYearAction(validTaxYear).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.get)) shouldBe SEE_OTHER
        }

        "has the overview redirect url" in {
          redirectUrl(result.map(_.left.get)) shouldBe controllers.errors.routes.TaxYearErrorController.show.url
        }
      }
    }
  }
}