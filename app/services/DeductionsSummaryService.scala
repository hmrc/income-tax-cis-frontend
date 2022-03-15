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
import models.pages.DeductionsSummaryPage
import models.pages.DeductionsSummaryPage.mapToInYearPage
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import utils.InYearUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeductionsSummaryService @Inject()(inYearUtil: InYearUtil, incomeTaxUserDataConnector: IncomeTaxUserDataConnector)
                                        (implicit ec: ExecutionContext) extends Logging {

  def pageModelFor(taxYear: Int, user: User)(implicit hc: HeaderCarrier): Future[Either[ServiceErrors, DeductionsSummaryPage]] = {
    logger.info("TEMP_LOGGING: Entered DeductionsSummaryService")
    if (!inYearUtil.inYear(taxYear)) {
      Future.successful(Right(DeductionsSummaryPage(taxYear = taxYear, isInYear = false, deductions = Seq.empty)))
    } else {
      incomeTaxUserDataConnector.getUserData(user.nino, taxYear)(hc.withExtraHeaders(headers = "mtditid" -> user.mtditid)).map {
        case Left(error) => Left(HttpParserError(error.status))
        case Right(IncomeTaxUserData(None)) => Left(EmptyPriorCisDataError)
        case Right(incomeTaxUserData) if !incomeTaxUserData.hasInYearCisDeductions => Left(EmptyInYearDeductionsError)
        case Right(incomeTaxUserData) => Right(mapToInYearPage(taxYear, incomeTaxUserData))
      }
    }
  }
}
