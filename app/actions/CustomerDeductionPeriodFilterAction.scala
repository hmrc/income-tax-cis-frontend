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
import models.UserSessionDataRequest
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import java.time.Month
import scala.concurrent.{ExecutionContext, Future}

case class CustomerDeductionPeriodFilterAction(taxYear: Int,
                                               appConfig: AppConfig
                                              )(implicit ec: ExecutionContext) extends ActionFilter[UserSessionDataRequest] with Logging {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def filter[A](input: UserSessionDataRequest[A]): Future[Option[Result]] = Future.successful {
    lazy val overviewRedirect = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))

    if (input.cisUserData.cis.priorPeriodData.map(_.deductionPeriod).length == Month.values().length) {
      logger.info(message = s"[CustomerDeductionPeriodFilterAction] User has no more deduction periods to submit for. Redirecting.")
      Some(overviewRedirect)
    } else if (input.cisUserData.cis.periodData.exists(_.contractorSubmitted)) {
      logger.info(message = s"[CustomerDeductionPeriodFilterAction] User cannot change contractor submitted period. Redirecting.")
      Some(overviewRedirect)
    } else {
      None
    }
  }
}
