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

import audit.{AuditService, ViewCisPeriodAudit}
import models.UserSessionDataRequest
import play.api.mvc.{ActionFilter, Result}
import sttp.model.Method.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

case class EndOfYearViewCisPeriodAuditAction(taxYear: Int,
                                             auditService: AuditService)
                                            (implicit ec: ExecutionContext) extends ActionFilter[UserSessionDataRequest] with FrontendHeaderCarrierProvider {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def filter[A](input: UserSessionDataRequest[A]): Future[Option[Result]] = Future.successful {
    if (GET.method == input.request.method) {
      val optionalViewAudit = ViewCisPeriodAudit.mapFrom(taxYear = taxYear, user = input.user, cisUserData = input.cisUserData)
      optionalViewAudit.foreach { viewAudit =>
        auditService.sendAudit[ViewCisPeriodAudit](viewAudit.toAuditModel)(hc(input.request), ec, ViewCisPeriodAudit.writes)
      }
    }
    None
  }
}
