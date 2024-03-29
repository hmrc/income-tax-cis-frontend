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

import config.ErrorHandler
import models.{AuthorisationRequest, UserPriorDataRequest}
import play.api.mvc.{ActionRefiner, Result}
import services.CISSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

case class UserPriorDataRequestRefinerAction(taxYear: Int,
                                             cisSessionService: CISSessionService,
                                             errorHandler: ErrorHandler)
                                            (implicit ec: ExecutionContext)
  extends ActionRefiner[AuthorisationRequest, UserPriorDataRequest] with FrontendHeaderCarrierProvider {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def refine[A](input: AuthorisationRequest[A]): Future[Either[Result, UserPriorDataRequest[A]]] = {
    cisSessionService.getPriorData(input.user, taxYear)(hc(input.request)).map {
      case Left(error) => Left(errorHandler.handleError(error.status)(input.request))
      case Right(incomeTaxUserData) => Right(UserPriorDataRequest(incomeTaxUserData, input.user, input.request))
    }
  }
}
