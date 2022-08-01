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

import models.UserPriorDataRequest
import models.nrs.ViewCisPeriodPayload
import play.api.mvc.{ActionFilter, Result}
import services.NrsService
import sttp.model.Method.GET
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import java.time.Month
import scala.concurrent.{ExecutionContext, Future}

case class PriorViewCisPeriodNrsAction(employerRef: String,
                                       month: String,
                                       nrsService: NrsService)
                                      (implicit ec: ExecutionContext) extends ActionFilter[UserPriorDataRequest] with FrontendHeaderCarrierProvider {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def filter[A](input: UserPriorDataRequest[A]): Future[Option[Result]] = Future.successful {
    implicit val headerCarrier: HeaderCarrier = hc(input.request)
    if (GET.method == input.request.method) {
      input.incomeTaxUserData.inYearCisDeductionsWith(employerRef).foreach { cisDeductions =>
        val deductionMonth = Month.valueOf(month.toUpperCase)
        cisDeductions.periodData.find(item => item.deductionPeriod == deductionMonth).foreach { period =>
          val nrsPayload = ViewCisPeriodPayload(cisDeductions.contractorName, employerRef, period)
          nrsService.submit[ViewCisPeriodPayload](input.user.nino, nrsPayload,
            input.user.mtditid)(hc(input.request), ViewCisPeriodPayload.writes)
        }
      }
    }
    None
  }
}
