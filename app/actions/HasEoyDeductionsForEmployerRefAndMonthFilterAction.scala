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

import config.{AppConfig, ErrorHandler}
import models.UserPriorDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import java.time.Month
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class HasEoyDeductionsForEmployerRefAndMonthFilterAction(taxYear: Int,
                                                              employerRef: String,
                                                              monthValue: String,
                                                              errorHandler: ErrorHandler,
                                                              appConfig: AppConfig,
                                                              needsToBeExclusivelyCustomerData: Boolean = false)
                                                             (implicit ec: ExecutionContext) extends ActionFilter[UserPriorDataRequest] {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def filter[A](input: UserPriorDataRequest[A]): Future[Option[Result]] = Future.successful {
    Try(Month.valueOf(monthValue.toUpperCase)) match {
      case Failure(_) => Some(errorHandler.internalServerError()(input))
      case Success(month: Month) =>

        val caughtByFilterAction = if(needsToBeExclusivelyCustomerData){
          !input.incomeTaxUserData.hasExclusivelyCustomerEoyCisDeductionsWith(employerRef, month)
        } else {
          !input.incomeTaxUserData.hasEoyCisDeductionsWith(employerRef, month)
        }

        if(caughtByFilterAction){
          Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        } else {
          None
        }
    }
  }
}
