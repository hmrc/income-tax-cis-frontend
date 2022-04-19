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
import controllers.routes.DeductionPeriodController
import models.{AuthorisationRequest, UserSessionDataRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.CISSessionService
import utils.UrlUtils

import scala.concurrent.{ExecutionContext, Future}

case class CisUserDataActionRefiner(taxYear: Int,
                                    employerRef: String,
                                    cisSessionService: CISSessionService,
                                    errorHandler: ErrorHandler,
                                    appConfig: AppConfig
                                   )(implicit ec: ExecutionContext) extends ActionRefiner[AuthorisationRequest, UserSessionDataRequest] {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def refine[A](input: AuthorisationRequest[A]): Future[Either[Result, UserSessionDataRequest[A]]] = {
    cisSessionService.getSessionData(taxYear, employerRef, input.user).map {
      case Left(_) => Left(errorHandler.internalServerError()(input))
      case Right(None) => Left(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      case Right(Some(cisUserData)) if !cisUserData.hasPeriodData => Left(Redirect(DeductionPeriodController.show(taxYear, UrlUtils.encode(employerRef))))
      case Right(Some(cisUserData)) if cisUserData.hasPeriodData => Right(UserSessionDataRequest(cisUserData, input.user, input.request))
    }
  }
}

object CisUserDataActionRefiner {

  def apply(taxYear: Int,
            contractor: String,
            cisSessionService: CISSessionService,
            errorHandler: ErrorHandler,
            appConfig: AppConfig)(implicit ec: ExecutionContext): CisUserDataActionRefiner = new CisUserDataActionRefiner(
    taxYear = taxYear,
    employerRef = UrlUtils.decode(contractor),
    cisSessionService = cisSessionService,
    errorHandler = errorHandler,
    appConfig = appConfig
  )
}