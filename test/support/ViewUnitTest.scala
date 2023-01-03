/*
 * Copyright 2023 HM Revenue & Customs
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

package support

import config.AppConfig
import models.{AuthorisationRequest, UserPriorDataRequest, UserSessionDataRequest}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.AnyContent
import play.api.test.Injecting
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.MockAppConfig
import uk.gov.hmrc.auth.core.AffinityGroup

trait ViewUnitTest extends UnitTest
  with UserScenarios
  with ViewHelper
  with GuiceOneAppPerSuite
  with Injecting
  with TaxYearProvider
  with FakeRequestHelper {

  protected implicit val mockAppConfig: AppConfig = new MockAppConfig().config()
  protected implicit lazy val messagesApi: MessagesApi = inject[MessagesApi]

  protected lazy val defaultMessages: Messages = messagesApi.preferred(fakeRequest)
  protected lazy val welshMessages: Messages = messagesApi.preferred(Seq(Lang("cy")))

  protected lazy val agentUserRequest: AuthorisationRequest[AnyContent] =
    anAuthorisationRequest.copy[AnyContent](aUser.copy(arn = Some("arn"), affinityGroup = AffinityGroup.Agent.toString))

  protected def getMessages(isWelsh: Boolean): Messages = if (isWelsh) welshMessages else defaultMessages

  protected def getAuthRequest(isAgent: Boolean): AuthorisationRequest[AnyContent] =
    if (isAgent) agentUserRequest else anAuthorisationRequest.copy[AnyContent]()

  protected def getUserSessionDataRequest(isAgent: Boolean): UserSessionDataRequest[AnyContent] =
    if (isAgent) {
      UserSessionDataRequest(aCisUserData, agentUserRequest.user, agentUserRequest.request)
    } else {
      UserSessionDataRequest(aCisUserData, anAuthorisationRequest.user, anAuthorisationRequest.request)
    }

  protected def getUserPriorDataRequest(isAgent: Boolean): UserPriorDataRequest[AnyContent] =
    if (isAgent) {
      UserPriorDataRequest(anIncomeTaxUserData, agentUserRequest.user, agentUserRequest.request)
    } else {
      UserPriorDataRequest(anIncomeTaxUserData, anAuthorisationRequest.user, anAuthorisationRequest.request)
    }
}
