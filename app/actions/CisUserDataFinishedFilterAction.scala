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

import controllers.routes.{ContractorDetailsController, DeductionPeriodController, LabourPayController}
import models.UserSessionDataRequest
import play.api.mvc.{ActionFilter, Result}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class CisUserDataFinishedFilterAction @Inject()(taxYear: Int)
                                                    (implicit ec: ExecutionContext)
  extends ActionFilter[UserSessionDataRequest] with FrontendHeaderCarrierProvider {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def filter[A](request: UserSessionDataRequest[A]): Future[Option[Result]] = Future.successful {
    val cisUserData = request.cisUserData
    if (cisUserData.isFinished) {
      None
    } else {
      val periodData = cisUserData.cis.periodData.get
      periodData match {
        case _ if !cisUserData.isPriorSubmission => Some(Redirect(ContractorDetailsController.show(cisUserData.taxYear, Some(cisUserData.employerRef))))
        case _ if !periodData.contractorSubmitted => Some(Redirect(DeductionPeriodController.show(cisUserData.taxYear, cisUserData.employerRef)))
        case _ => Some(Redirect(LabourPayController.show(cisUserData.taxYear, periodData.deductionPeriod.toString.toLowerCase, cisUserData.employerRef)))
      }
    }
  }
}
