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

package services

import connectors.CISConnector
import models.{HttpParserError, IncomeTaxUserData, InvalidOrUnfinishedSubmission, PeriodData, ServiceError, User}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteCISPeriodService @Inject()(cisSessionService: CISSessionService,
                                       cisConnector: CISConnector)(implicit val ec: ExecutionContext) {

  //TODO: - Update integration test to check if the item has been removed, when this is implemented.
  def removeCisDeduction(taxYear: Int,
                         employerRef: String,
                         user: User,
                         deductionPeriod: Month,
                         incomeTaxUserData: IncomeTaxUserData)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {

    val customerPeriods: Seq[PeriodData] = incomeTaxUserData.customerCisDeductionsWith(employerRef).map(_.periodData).getOrElse(Seq.empty)

    val isLastCustomerPeriodToRemove: Boolean = customerPeriods.size == 1 && customerPeriods.map(_.deductionPeriod).contains(deductionPeriod)

    if(isLastCustomerPeriodToRemove){
      //TODO Delete orchestration
      Future.successful(Right(()))

    } else {
      val submission = incomeTaxUserData.toSubmissionWithoutPeriod(employerRef,deductionPeriod,taxYear)

      submission match {
        case Some(submission) =>
          cisConnector.submit(user.nino,taxYear,submission)(hc.withExtraHeaders(headers = "mtditid" -> user.mtditid)).flatMap {
            case Left(error) => Future.successful(Left(HttpParserError(error.status)))
            case Right(_) => cisSessionService.refreshAndClear(user,employerRef,taxYear,clearCYA = false)
          }
        case None => Future.successful(Left(InvalidOrUnfinishedSubmission))
      }
    }
  }
}
