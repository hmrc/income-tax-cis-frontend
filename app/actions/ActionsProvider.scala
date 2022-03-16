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

import config.{AppConfig, ErrorHandler}
import models.{AuthorisationRequest, UserSessionDataRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.CISSessionService
import utils.InYearUtil
import utils.UrlUtils.decoded

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ActionsProvider @Inject()(authAction: AuthorisedAction,
                                cisSessionService: CISSessionService,
                                errorHandler: ErrorHandler,
                                inYearUtil: InYearUtil,
                                appConfig: AppConfig
                               )(implicit ec: ExecutionContext) {

  def inYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] = authAction.andThen(inYearActionBuilder(taxYear))

  def notInYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] = authAction.andThen(notInYearActionBuilder(taxYear))

  def notInYearWithSessionData(taxYear: Int, contractor: String): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authAction
      .andThen(notInYearActionBuilder(taxYear))
      .andThen(cisUserDataAction(taxYear, contractor))

  private def notInYearActionBuilder(taxYear: Int): ActionFilter[AuthorisationRequest] = new ActionFilter[AuthorisationRequest] {
    override protected def executionContext: ExecutionContext = ec

    override protected def filter[A](request: AuthorisationRequest[A]): Future[Option[Result]] = Future.successful {
      if (inYearUtil.inYear(taxYear)) {
        Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      } else {
        None
      }
    }
  }

  private def inYearActionBuilder(taxYear: Int): ActionFilter[AuthorisationRequest] = new ActionFilter[AuthorisationRequest] {
    override protected def executionContext: ExecutionContext = ec

    override protected def filter[A](request: AuthorisationRequest[A]): Future[Option[Result]] = Future.successful {
      if (!inYearUtil.inYear(taxYear)) {
        Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      } else {
        None
      }
    }
  }

  private def cisUserDataAction(taxYear: Int, contractor: String)
                               (implicit ec: ExecutionContext): ActionRefiner[AuthorisationRequest, UserSessionDataRequest] = {
    new ActionRefiner[AuthorisationRequest, UserSessionDataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def refine[A](input: AuthorisationRequest[A]): Future[Either[Result, UserSessionDataRequest[A]]] = {
        val employerRef = decoded(contractor)

        cisSessionService.getSessionData(taxYear, employerRef, input.user).map {
          case Left(_) => Left(errorHandler.internalServerError()(input))
          case Right(None) => Left(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          case Right(Some(cisUserData)) => Right(UserSessionDataRequest(cisUserData, input.user, input.request))
        }
      }
    }
  }
}