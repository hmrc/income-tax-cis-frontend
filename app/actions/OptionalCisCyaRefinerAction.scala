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

package actions

import common.SessionValues.TEMP_EMPLOYER_REF
import config.{AppConfig, ErrorHandler}
import controllers.routes.ContractorSummaryController
import models.mongo.{CisCYAModel, CisUserData}
import models.{AuthorisationRequest, UserSessionDataRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.CISSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import java.time.Month
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class OptionalCisCyaRefinerAction(taxYear: Int,
                                       employerRef: String,
                                       monthValue: String,
                                       cisSessionService: CISSessionService,
                                       errorHandler: ErrorHandler,
                                       appConfig: AppConfig)
                                      (implicit ec: ExecutionContext)
  extends ActionRefiner[AuthorisationRequest, UserSessionDataRequest] with FrontendHeaderCarrierProvider {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def refine[A](input: AuthorisationRequest[A]): Future[Either[Result, UserSessionDataRequest[A]]] = {

    val tempEmployerRef = input.session.get(TEMP_EMPLOYER_REF)

    Try(Month.valueOf(monthValue.toUpperCase)) match {
      case Failure(_) => Future.successful(Left(errorHandler.internalServerError()(input)))
      case Success(month: Month) =>
        cisSessionService.checkCyaAndReturnData(taxYear, employerRef, input.user, month, tempEmployerRef)(hc(input.request)).map {
          case Left(_) => Left(errorHandler.internalServerError()(input))
          case Right(None) => Left(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          case Right(Some(data@CisUserData(_, _, _, _, _, _, _, CisCYAModel(_, Some(_), _), _))) =>
            Right(UserSessionDataRequest(data, input.user, input.request))
          case Right(Some(_)) => Left(Redirect(ContractorSummaryController.show(taxYear, employerRef)))
        }
    }
  }
}
