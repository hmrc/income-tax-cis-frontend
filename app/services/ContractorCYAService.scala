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

import connectors.IncomeTaxUserDataConnector
import models._
import models.pages.ContractorCYAPage
import models.pages.ContractorCYAPage.mapToInYearPage
import uk.gov.hmrc.http.HeaderCarrier
import utils.InYearUtil

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContractorCYAService @Inject()(inYearUtil: InYearUtil, incomeTaxUserDataConnector: IncomeTaxUserDataConnector)
                                    (implicit ec: ExecutionContext) {

  //scalastyle:off
  def pageModelFor(taxYear: Int, month: Month, refNumber: String, user: User)(implicit hc: HeaderCarrier): Future[Either[ServiceErrors, ContractorCYAPage]] = {
    if (!inYearUtil.inYear(taxYear)) {
      Future.successful(Right(ContractorCYAPage(taxYear = taxYear, isInYear = false, None, refNumber, month, None, None, None)))
    } else {
      incomeTaxUserDataConnector.getUserData(user.nino, taxYear)(hc.withExtraHeaders(headers = "mtditid" -> user.mtditid)).map {
        case Left(error) => Left(HttpParserError(error.status))
        case Right(IncomeTaxUserData(None)) => Left(EmptyPriorCisDataError)
        case Right(incomeTaxUserData) if !incomeTaxUserData.hasInYearCisDeductions => Left(EmptyInYearDeductionsError)
        case Right(incomeTaxUserData) if !incomeTaxUserData.hasInYearCisDeductionsWith(refNumber) => Left(EmployerRefNotFoundError)
        case Right(incomeTaxUserData) if !incomeTaxUserData.hasInYearPeriodDataFor(refNumber, month) => Left(DeductionPeriodNotFoundError)
        case Right(incomeTaxUserData) => Right(mapToInYearPage(taxYear, incomeTaxUserData.inYearCisDeductionsWith(refNumber).get, month))
      }
    }
  }
  //scalastyle:on
}
