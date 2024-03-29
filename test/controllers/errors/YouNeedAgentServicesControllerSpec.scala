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

package controllers.errors

import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentType, status}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import support.ControllerUnitTest
import views.html.templates.YouNeedAgentServicesView

class YouNeedAgentServicesControllerSpec extends ControllerUnitTest with DefaultAwaitTimeout {

  private implicit lazy val mockMessagesControllerComponents: MessagesControllerComponents = Helpers.stubMessagesControllerComponents()

  private lazy val underTest = new YouNeedAgentServicesController(
    mockMessagesControllerComponents,
    app.injector.instanceOf[YouNeedAgentServicesView],
    appConfig
  )

  "The show method" should {
    "return an OK response when .show() is called" in {
      val fakeRequest = FakeRequest("GET", "/error/you-need-agent-services-account")
      val result = underTest.show()(fakeRequest)

      status(result) shouldBe UNAUTHORIZED
      contentType(result) shouldBe Some("text/html")
    }
  }
}
