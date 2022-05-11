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

import config.AppConfig
import models.UserPriorDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import scala.concurrent.{ExecutionContext, Future}

case class HasEOYDataWithEmployerRefFilterAction(taxYear: Int,
                                                 employerRef: String,
                                                 appConfig: AppConfig
                                                )(implicit ec: ExecutionContext) extends ActionFilter[UserPriorDataRequest] {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def filter[A](input: UserPriorDataRequest[A]): Future[Option[Result]] = Future.successful {
    if (!input.incomeTaxUserData.hasEOYCisDeductionsWith(employerRef)) {
      Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    } else {
      None
    }
  }
}
