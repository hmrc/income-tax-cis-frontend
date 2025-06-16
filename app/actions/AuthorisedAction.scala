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

package actions

import common.{EnrolmentIdentifiers, EnrolmentKeys, SessionValues}
import config.{AppConfig, ErrorHandler}
import controllers.errors.routes.{AgentAuthErrorController, IndividualAuthErrorController, SupportingAgentAuthErrorController, UnauthorisedUserErrorController}
import models.{AuthorisationRequest, MissingAgentClientDetails, User}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import services.{AuthService, SessionDataService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.{EnrolmentHelper, SessionHelper}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthorisedAction @Inject()(val appConfig: AppConfig,
                                 authService: AuthService,
                                 sessionDataService: SessionDataService,
                                 errorHandler: ErrorHandler)
                                (val mcc: MessagesControllerComponents)
  extends ActionBuilder[AuthorisationRequest, AnyContent] with I18nSupport with Logging with SessionHelper {

  override implicit val executionContext: ExecutionContext = mcc.executionContext
  override implicit val messagesApi: MessagesApi = mcc.messagesApi
  override def parser: BodyParser[AnyContent] = mcc.parsers.default

  override def invokeBlock[A](request: Request[A], block: AuthorisationRequest[A] => Future[Result]): Future[Result] = {

    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val req: Request[A] = request

    withSessionId { sessionId =>
      authService.authorised().retrieve(affinityGroup) {
        case Some(AffinityGroup.Agent) => agentAuthentication(block, sessionId)(request, headerCarrier)
        case Some(affinityGroup) => individualAuthentication(block, affinityGroup, sessionId)(request, headerCarrier)
        case _ =>
          logger.warn(s"[AuthorisedAction][invokeBlock] - No Affinity Group returned on Auth response")
          throw AuthorisationException.fromString("[AuthorisedAction][invokeBlock] - No Affinity Group returned on Auth response")
      } recover {
        case _: NoActiveSession =>
          Redirect(appConfig.signInUrl)
        case _: AuthorisationException =>
          logger.warn(s"[AuthorisedAction][invokeBlock] - User failed to authenticate")
          Redirect(UnauthorisedUserErrorController.show)
      }
    }
  }

  def individualAuthentication[A](block: AuthorisationRequest[A] => Future[Result],
                                  affinityGroup: AffinityGroup,
                                  sessionId: String
                                 )(implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {
    authService.authorised().retrieve(allEnrolments and confidenceLevel) {
      case enrolments ~ userConfidence if userConfidence.level >= ConfidenceLevel.L250.level =>
        (
          EnrolmentHelper.getEnrolmentValueOpt(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments),
          EnrolmentHelper.getEnrolmentValueOpt(EnrolmentKeys.nino, EnrolmentIdentifiers.nino, enrolments)
        ) match {
          case (Some(mtdItId), Some(nino)) =>
            block(AuthorisationRequest(
              user = User(mtdItId, None, nino, sessionId, affinityGroup.toString),
              request = request
            ))
          case (_, None) =>
            logger.info(s"[AuthorisedAction][individualAuthentication] - User has no NINO. Redirecting to ${appConfig.signInUrl}")
            Future.successful(Redirect(appConfig.signInUrl))
          case (None, _) =>
            logger.info(s"[AuthorisedAction][individualAuthentication] - User has no MTD IT enrolment. Redirecting user to sign up for MTD.")
            Future.successful(Redirect(IndividualAuthErrorController.show))
        }
      case _ =>
        logger.info("[AuthorisedAction][individualAuthentication] User has confidence level below 250, routing user to IV uplift.")
        Future(Redirect(appConfig.incomeTaxSubmissionIvRedirect))
    }
  }

  private[actions] def agentAuthentication[A](block: AuthorisationRequest[A] => Future[Result],
                                              sessionId: String
                                             )(implicit request: Request[A], hc: HeaderCarrier): Future[Result] =
    sessionDataService.getSessionData(sessionId).flatMap { sessionData =>
        authService
          .authorised(EnrolmentHelper.agentAuthPredicate(sessionData.mtditid))
          .retrieve(allEnrolments)(
            enrollments => handleForValidAgent(block, sessionData.mtditid, sessionData.nino, sessionId, enrollments, isSupportingAgent = false)
          )
          .recoverWith(agentRecovery(block, sessionData.mtditid, sessionData.nino, sessionId))
    }.recover {
      case _: MissingAgentClientDetails =>
        Redirect(appConfig.viewAndChangeEnterUtrUrl)
    }

  private def agentRecovery[A](block: AuthorisationRequest[A] => Future[Result],
                               mtdItId: String,
                               nino: String,
                               sessionId: String
                              )(implicit request: Request[A], hc: HeaderCarrier): PartialFunction[Throwable, Future[Result]] = {
    case _: NoActiveSession =>
      Future(Redirect(appConfig.signInUrl))
    case _: AuthorisationException => authService
      .authorised(EnrolmentHelper.secondaryAgentPredicate(mtdItId))
      .retrieve(allEnrolments) {
        enrolments => handleForValidAgent(block, mtdItId, nino, sessionId, enrolments, isSupportingAgent = true)
      }.recover {
        case _: AuthorisationException =>
          logger.info(s"[AuthorisedAction][agentAuthentication] - Agent does not have delegated authority for Client.")
          Redirect(AgentAuthErrorController.show)
        case e =>
          logger.error(s"[AuthorisedAction][agentAuthentication] - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
          errorHandler.internalServerError()
      }
    case e =>
      logger.error(s"[AuthorisedAction][agentAuthentication] - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
      Future.successful(errorHandler.internalServerError())
  }

  private def handleForValidAgent[A](block: AuthorisationRequest[A] => Future[Result],
                                     mtdItId: String,
                                     nino: String,
                                     sessionId: String,
                                     enrolments: Enrolments,
                                     isSupportingAgent: Boolean)
                                    (implicit request: Request[A]): Future[Result] =
    if(isSupportingAgent) {
      logger.warn(s"[AuthorisedAction][agentAuthentication] - Secondary agent unauthorised")
      Future.successful(Redirect(controllers.errors.routes.SupportingAgentAuthErrorController.show))
    } else {
      EnrolmentHelper.getEnrolmentValueOpt(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) match {
        case Some(arn) =>
          block(AuthorisationRequest(
            user = User(mtdItId, Some(arn), nino, AffinityGroup.Agent.toString, sessionId, isSupportingAgent),
            request = request
          ))
        case None =>
          logger.warn(s"[AuthorisedAction][agentAuthentication] - Agent with no HMRC-AS-AGENT enrolment. Rendering unauthorised view.")
          Future.successful(Redirect(controllers.errors.routes.YouNeedAgentServicesController.show))
      }
    }
}
