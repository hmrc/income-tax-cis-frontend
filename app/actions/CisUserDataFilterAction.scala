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

import config.AppConfig
import controllers.routes.DeductionPeriodController
import models.UserSessionDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import scala.concurrent.{ExecutionContext, Future}

case class CisUserDataFilterAction(taxYear: Int,
                                   employerRef: String,
                                   appConfig: AppConfig,
                                   needsPeriodData: Boolean,
                                   redirectIfPrior: Boolean
                                  )(implicit ec: ExecutionContext) extends ActionFilter[UserSessionDataRequest] {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def filter[A](input: UserSessionDataRequest[A]): Future[Option[Result]] = {
    val data = input.cisUserData
    val result =
      if (data.isPriorSubmission && redirectIfPrior) {
        Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
      else if (data.hasPeriodData || !needsPeriodData) {
        None
      }
      else { // needs period data but doesn't have any
        Some(Redirect(DeductionPeriodController.show(taxYear, employerRef)))
      }
    Future.successful(result)
  }
}
