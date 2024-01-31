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

import play.api.http.HeaderNames.LOCATION
import play.api.http.Status.SEE_OTHER
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{header, status}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import support.ControllerUnitTest

class SignOutControllerTest extends ControllerUnitTest with DefaultAwaitTimeout {

  private lazy val mockMessagesControllerComponents: MessagesControllerComponents = Helpers.stubMessagesControllerComponents()

  private val underTest = new SignOutController(mockMessagesControllerComponents, appConfig)

  "SigOutController" should {
    "redirect user to exit survey" when {
      "signOut() is called it" should {
        val request = FakeRequest("GET", "/sign-out")
        val responseF = underTest.signOut(false)(request)

        "return status code 303" in {
          status(responseF) shouldBe SEE_OTHER
        }

        "return a Location header containing the sign out url with feedback url" in {
          header(LOCATION, responseF) shouldBe Some("/sign-out-url?continue=%2FfeedbackUrl")
        }
      }
    }
  }
}
