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

package config

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.i18n._
import play.api.mvc.Result
import support.UnitTest
import utils.ViewTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global


class ErrorHandlerSpec extends UnitTest with GuiceOneAppPerSuite with ViewTest {

  private val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  private val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  private val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]

  private val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val underTest = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig, implicitly[ExecutionContext])

  private val h1Expected = "Page not found"
  private val expectedTitle = s"$h1Expected - $serviceName - $govUkExtension"

  ".handleError" should {
    "return a ServiceUnavailable when passed a SERVICE_UNAVAILABLE 503" in {
      val result: Future[Result] = Future.successful(underTest.handleError(503)(fakeRequest))

      bodyOf(result) should include("Sorry, the service is unavailable")
      status(result) shouldBe SERVICE_UNAVAILABLE
    }

    "return an InternalServerError when passed anything other than a 503" in {
      val result: Future[Result] = Future.successful(underTest.handleError(400)(fakeRequest))

      bodyOf(result) should include("Sorry, there is a problem with the service")
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "the NotFoundTemplate" should {
    "return the notFoundTemplate when an incorrect web address when been entered" which {
      lazy val view = await(underTest.notFoundTemplate)
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "displays the correct page title" in {

        document.title shouldBe expectedTitle
      }
    }
  }
}
