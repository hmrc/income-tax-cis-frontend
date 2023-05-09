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

package services

import connectors.TailoringDataConnector
import connectors.parsers.ClearExcludedJourneysHttpParser.ClearExcludedJourneysResponse
import connectors.parsers.GetExcludedJourneysHttpParser.ExcludedJourneysResponse
import connectors.parsers.PostExcludedJourneyHttpParser.PostExcludedJourneyResponse
import models._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TailoringService @Inject()(deleteCISPeriodService: DeleteCISPeriodService,
                                 cisSessionService: CISSessionService,
                                 contractorCYAService: ContractorCYAService,
                                 tailoringDataConnector: TailoringDataConnector)
                                (implicit val ec: ExecutionContext) {

  private def tailorCustomerData(taxYear: Int, user: User, incomeTaxUserData: IncomeTaxUserData)(
    implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {

    val customerDeductions: CISSource = incomeTaxUserData.cis.fold(CISSource(None, None, None, Seq[CisDeductions]().empty))(
      _.customerCISDeductions.getOrElse(CISSource(None, None, None, Seq[CisDeductions]().empty))
    )
    val customerResults: Future[Seq[Either[ServiceError, Unit]]] = {
      Future.sequence(customerDeductions.cisDeductions.flatMap(deductions => deductions.periodData.map(periods =>
        deleteCISPeriodService.removeCisDeduction(taxYear, deductions.employerRef, user, periods.deductionPeriod, incomeTaxUserData))))
    }

    customerResults.map { results =>
      if (results.exists(_.isLeft)) {
        Left(FailedTailoringRemoveDeductionError)
      } else {
        Right(())
      }
    }
  }

  private def tailorContractorData(taxYear: Int, user: User, incomeTaxUserData: IncomeTaxUserData)(
    implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {

    val contractorDeductions: CISSource = incomeTaxUserData.cis.fold(CISSource(None, None, None, Seq[CisDeductions]().empty))(
      _.contractorCISDeductions.getOrElse(CISSource(None, None, None, Seq[CisDeductions]().empty))
    )
    val contractorResults: Future[Seq[Either[ServiceError, Unit]]] = {
      Future.sequence(contractorDeductions.cisDeductions.map(deductions =>
        contractorCYAService.submitZeroCisDeductionTailor(taxYear, deductions.employerRef, user, deductions.toCISSubmission(taxYear))))
    }
    contractorResults.map { results =>
      if (results.exists(_.isLeft)) {
        Left(FailedTailoringOverrideDeductionError)
      } else {
        Right(())
      }
    }
  }

  def removeCISData(taxYear: Int, user: User, incomeTaxUserData: IncomeTaxUserData)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {
    tailorCustomerData(taxYear, user, incomeTaxUserData).flatMap {
      case Left(customerError) => Future.successful(Left(customerError))
      case Right(_) => tailorContractorData(taxYear, user, incomeTaxUserData).map {
        case Left(contractorError) => Left(contractorError)
        case Right(_) => Right(())
      }
    }

  }

  def getExcludedJourneys(taxYear: Int, nino: String, mtditid: String)(implicit hc: HeaderCarrier): Future[ExcludedJourneysResponse] = {
    tailoringDataConnector.getExcludedJourneys(taxYear, nino)(hc.withExtraHeaders("mtditid" -> mtditid))
  }

  def clearExcludedJourney(taxYear: Int, nino: String, mtditid: String)
                          (implicit hc: HeaderCarrier): Future[ClearExcludedJourneysResponse] = {
    tailoringDataConnector.clearExcludedJourney(taxYear, nino)(hc.withExtraHeaders("mtditid" -> mtditid))
  }

  def postExcludedJourney(taxYear: Int, nino: String, mtditid: String)
                         (implicit hc: HeaderCarrier): Future[PostExcludedJourneyResponse] = {
    tailoringDataConnector.postExcludedJourney(taxYear, nino)(hc.withExtraHeaders("mtditid" -> mtditid))
  }

}
