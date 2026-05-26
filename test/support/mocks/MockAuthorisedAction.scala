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

package support.mocks

import actions.AuthorisedAction
import common.{EnrolmentIdentifiers, EnrolmentKeys}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers.stubMessagesControllerComponents
import services.AuthService
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import support.stubs.AppConfigStub
import uk.gov.hmrc.auth.core._
import org.mockito.ArgumentMatchers.{eq as eqTo}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.{~, Retrieval}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAuthorisedAction extends MockErrorHandler
  with MockSessionDataService { this: UnitTest =>

  private val mockAppConfig = new AppConfigStub().config()
  private val mockAuthConnector: AuthConnector =
    org.mockito.Mockito.mock(classOf[AuthConnector])
  private val mockAuthService = new AuthService(mockAuthConnector)

  protected val mockAuthorisedAction: AuthorisedAction = new AuthorisedAction(mockAppConfig, mockAuthService, mockSessionDataService, mockErrorHandler)(stubMessagesControllerComponents())

  protected def mockAuthAsAgent(): Unit = {
    val enrolments: Enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, aUser.mtditid)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
    ))

    val agentRetrievals: Some[AffinityGroup] = Some(AffinityGroup.Agent)

    mockGetSessionData(sessionId)(sessionData)

    when(
      mockAuthConnector.authorise[Option[AffinityGroup]](
        any[Predicate](),
        eqTo(Retrievals.affinityGroup)
      )(any[HeaderCarrier](), any[ExecutionContext]())
    ).thenReturn(Future.successful(agentRetrievals))

    when(
      mockAuthConnector.authorise[Enrolments](
        any[Predicate](),
        eqTo(Retrievals.allEnrolments)
      )(any[HeaderCarrier](), any[ExecutionContext]())
    ).thenReturn(Future.successful(enrolments))
  }

  protected def mockAuth(nino: Option[String]): Unit = {
    mockAuthAsIndividual(nino)
  }

  protected def mockAuthAsIndividual(nino: Option[String]): Unit = {
    val enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, aUser.mtditid)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
    ) ++ nino.fold(Seq.empty[Enrolment])(unwrappedNino =>
      Seq(Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, unwrappedNino)), "Activated"))
    ))

    when(
      mockAuthConnector.authorise[Option[AffinityGroup]](
        any[Predicate](),
        eqTo(Retrievals.affinityGroup)
      )(any[HeaderCarrier](), any[ExecutionContext]())
    ).thenReturn(Future.successful(Some(AffinityGroup.Individual)))

    when(
      mockAuthConnector.authorise[Enrolments ~ ConfidenceLevel](
        any[Predicate](),
        eqTo(allEnrolments and confidenceLevel)
      )(any[HeaderCarrier](), any[ExecutionContext]())
    ).thenReturn(Future.successful(enrolments and ConfidenceLevel.L250))
  }

  protected def mockFailToAuthenticate(): Unit = {
    when(
      mockAuthConnector.authorise[Any](
        any[Predicate](),
        any[Retrieval[Any]]()
      )(any[HeaderCarrier](), any[ExecutionContext]())
    ).thenReturn(Future.failed(InsufficientConfidenceLevel()))
  }
}
