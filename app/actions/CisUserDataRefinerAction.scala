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

import common.SessionValues.TEMP_EMPLOYER_REF
import config.{AppConfig, ErrorHandler}
import models.{AuthorisationRequest, UserSessionDataRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.CISSessionService

import scala.concurrent.{ExecutionContext, Future}

case class CisUserDataRefinerAction(taxYear: Int,
                                    employerRef: String,
                                    cisSessionService: CISSessionService,
                                    errorHandler: ErrorHandler,
                                    appConfig: AppConfig,
                                   )(implicit ec: ExecutionContext) extends ActionRefiner[AuthorisationRequest, UserSessionDataRequest] {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def refine[A](input: AuthorisationRequest[A]): Future[Either[Result, UserSessionDataRequest[A]]] = {
    val tempEmployerRef = input.session.get(TEMP_EMPLOYER_REF)

    cisSessionService.getSessionData(taxYear, employerRef, input.user, tempEmployerRef).map {
      case Left(_) => Left(errorHandler.internalServerError()(input))
      case Right(None) => Left(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      case Right(Some(cisUserData)) => Right(UserSessionDataRequest(cisUserData, input.user, input.request))
    }
  }
}
