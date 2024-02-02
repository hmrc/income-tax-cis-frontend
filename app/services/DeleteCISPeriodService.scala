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

package services

import audit.{AuditService, DeleteCisPeriodAudit}
import connectors.CISConnector
import connectors.parsers.CISHttpParser.CISResponse
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import models._
import models.nrs.{ContractorDetails, DeleteCisPeriodPayload}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteCISPeriodService @Inject()(cisSessionService: CISSessionService,
                                       auditService: AuditService,
                                       nrsService: NrsService,
                                       cisConnector: CISConnector)(implicit val ec: ExecutionContext) {

  lazy val unfinishedSubmissionResponse: Future[Either[ServiceError, Unit]] = Future.successful(Left(InvalidOrUnfinishedSubmission))

  def removeCisDeduction(taxYear: Int,
                         employerRef: String,
                         user: User,
                         deductionPeriod: Month,
                         incomeTaxUserData: IncomeTaxUserData)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {

    val customerPeriods: Seq[PeriodData] = incomeTaxUserData.customerCisDeductionsWith(employerRef).map(_.periodData).getOrElse(Seq.empty)

    if (customerPeriods.size == 1) {
      customerPeriods.find(_.deductionPeriod == deductionPeriod) match {
        case Some(PeriodData(_, _, _, _, _, Some(submissionId), _)) =>
          handleAPICall(cisConnector.delete(user.nino, taxYear, submissionId)(hc.withExtraHeaders(headers = "mtditid" -> user.mtditid)),
            user, employerRef, taxYear, deductionPeriod, incomeTaxUserData)
        case _ => unfinishedSubmissionResponse
      }
    } else {
      incomeTaxUserData.toSubmissionWithoutPeriod(employerRef, deductionPeriod, taxYear) match {
        case Some(submission) =>
          handleAPICall(cisConnector.submit(user.nino, taxYear, submission)(hc.withExtraHeaders(headers = "mtditid" -> user.mtditid)),
            user, employerRef, taxYear, deductionPeriod, incomeTaxUserData)
        case None => unfinishedSubmissionResponse
      }
    }
  }

  private def handleAPICall(api: Future[CISResponse], user: User, employerRef: String, taxYear: Int,
                            deductionPeriod: Month, incomeTaxUserData: IncomeTaxUserData)
                           (implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {
    api.flatMap {
      case Left(error) => Future.successful(Left(HttpParserError(error.status)))
      case Right(_) =>
        handleAuditing(taxYear, employerRef, user, deductionPeriod, incomeTaxUserData)
        handleNrs(employerRef, user, deductionPeriod, incomeTaxUserData)
        cisSessionService.refreshAndClear(user, employerRef, taxYear, clearCYA = false)
    }
  }

  private def handleAuditing(taxYear: Int,
                             employerRef: String,
                             user: User,
                             deductionPeriod: Month,
                             incomeTaxUserData: IncomeTaxUserData)
                            (implicit hc: HeaderCarrier): Future[AuditResult] = {
    val customerDeductions: CisDeductions = incomeTaxUserData.customerCisDeductionsWith(employerRef).get
    val auditPeriod = customerDeductions.periodData.find(_.deductionPeriod == deductionPeriod).get

    val auditEvent = DeleteCisPeriodAudit(taxYear, user, customerDeductions.contractorName, employerRef, auditPeriod)
    auditService.sendAudit[DeleteCisPeriodAudit](auditEvent.toAuditModel)
  }

  private def handleNrs(employerRef: String,
                        user: User,
                        deductionPeriod: Month,
                        incomeTaxUserData: IncomeTaxUserData)
                       (implicit hc: HeaderCarrier): Future[NrsSubmissionResponse] = {
    val customerDeductions: CisDeductions = incomeTaxUserData.customerCisDeductionsWith(employerRef).get
    val periodData = customerDeductions.periodData.find(_.deductionPeriod == deductionPeriod).get

    val nrsPayload = DeleteCisPeriodPayload(ContractorDetails(customerDeductions.contractorName, employerRef), periodData)
    nrsService.submit(user.nino, nrsPayload, user.mtditid)
  }
}
