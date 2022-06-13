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
import models.mongo.CisUserData
import models.{HttpParserError, InvalidOrUnfinishedSubmission, ServiceError, User}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContractorCYAService @Inject()(cisSessionService: CISSessionService,
                                     cisConnector: CISConnector)(implicit val ec: ExecutionContext) {

  def submitCisDeductionCYA(taxYear: Int,
                            employerRef: String,
                            user: User,
                            cisUserData: CisUserData)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {

    cisUserData.toSubmission match {
      case Some(submission) =>
        cisConnector.submit(user.nino,taxYear,submission)(hc.withExtraHeaders(headers = "mtditid" -> user.mtditid)).flatMap {
        case Left(error) => Future.successful(Left(HttpParserError(error.status)))
        case Right(_) => cisSessionService.refreshAndClear(user,employerRef,taxYear)
      }
      case None => Future.successful(Left(InvalidOrUnfinishedSubmission))
    }
  }
}
