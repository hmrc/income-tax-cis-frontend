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

import audit.{AmendCisContractorAudit, AuditService, CreateNewCisContractorAudit}
import connectors.CISConnector
import models._
import models.mongo.CisUserData
import models.submission.CISSubmission
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContractorCYAService @Inject()(cisSessionService: CISSessionService,
                                     auditService: AuditService,
                                     cisConnector: CISConnector)
                                    (implicit val ec: ExecutionContext) {

  def submitCisDeductionCYA(taxYear: Int,
                            employerRef: String,
                            user: User,
                            cisUserData: CisUserData)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {

    cisUserData.toSubmission match {
      case Some(submission) =>
        cisConnector.submit(user.nino, taxYear, submission)(hc.withExtraHeaders(headers = "mtditid" -> user.mtditid)).flatMap {
          case Left(error) => Future.successful(Left(HttpParserError(error.status)))
          case Right(_) =>
            val handleAuditFuture = handleAudit(taxYear, employerRef, user, cisUserData)
            val refreshAndClearFuture = cisSessionService.refreshAndClear(user, employerRef, taxYear)
            for {
              _ <- handleAuditFuture
              result <- refreshAndClearFuture
            } yield result
        }
      case None => Future.successful(Left(InvalidOrUnfinishedSubmission))
    }
  }

  def submitZeroCisDeductionTailor(taxYear: Int,
                            employerRef: String,
                            user: User,
                            cisData: CISSubmission)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {
        cisConnector.submit(user.nino, taxYear, cisData)(hc.withExtraHeaders(headers = "mtditid" -> user.mtditid)).flatMap {
          case Left(error) => Future.successful(Left(HttpParserError(error.status)))
          case Right(_) =>
            val refreshAndClearFuture = cisSessionService.refreshAndClear(user, employerRef, taxYear)
            for {
              result <- refreshAndClearFuture
            } yield result
        }
  }

  private def handleAudit(taxYear: Int, employerRef: String, user: User, cisUserData: CisUserData)
                               (implicit hc: HeaderCarrier): Future[Unit] = {
    if (cisContractorCreated(cisUserData)) {
      submitCisCreateAudit(taxYear, employerRef, user, cisUserData)
      Future.successful(())
    } else {
      cisSessionService.getPriorData(user, taxYear).map {
        case Right(incomeTaxUserData) =>
          submitAmendCisAudit(taxYear, employerRef, user, cisUserData, incomeTaxUserData)
        case _ => ()
      }
    }
  }

  private def submitAmendCisAudit(taxYear: Int, employerRef: String, user: User, cisUserData: CisUserData, incomeTaxUserData: IncomeTaxUserData)
                                 (implicit hc: HeaderCarrier): Unit = {
    val auditModel = AmendCisContractorAudit(taxYear, employerRef, user, cisUserData, incomeTaxUserData)
    auditService.sendAudit[AmendCisContractorAudit](auditModel.toAuditModel)
  }

  private def submitCisCreateAudit(taxYear: Int, employerRef: String, user: User, cisUserData: CisUserData)(implicit hc: HeaderCarrier): Unit = {
    val auditModel = CreateNewCisContractorAudit.mapFrom(taxYear, employerRef, user, cisUserData).get
    auditService.sendAudit[CreateNewCisContractorAudit](auditModel.toAuditModel)
  }

  private def cisContractorCreated(cisUserData: CisUserData): Boolean = {
    cisUserData.submissionId.isEmpty
  }
}
