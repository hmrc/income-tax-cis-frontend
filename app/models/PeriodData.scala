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

package models

import play.api.Logging
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import utils.PagerDutyHelper.PagerDutyKeys.INVALID_PERIOD_DATES
import utils.PagerDutyHelper.pagerDutyLog

import java.time.{LocalDate, Month}

case class PeriodData(deductionPeriod: Month,
                      deductionAmount: Option[BigDecimal],
                      costOfMaterials: Option[BigDecimal],
                      grossAmountPaid: Option[BigDecimal],
                      submissionDate: String,
                      submissionId: Option[String],
                      source: String){

  def toSubmissionPeriodData(taxYear: Int): Option[submission.PeriodData] = {
    deductionAmount.map { deductionAmount =>
      submission.PeriodData(taxYear, deductionPeriod, grossAmountPaid, deductionAmount, costOfMaterials)
    }
  }
  def toZeroSubmissionPeriodData(taxYear: Int): submission.PeriodData = {
    submission.PeriodData(taxYear = taxYear,
      deductionPeriod = deductionPeriod,
      grossAmountPaid = this.grossAmountPaid.map(_ => 0),
      deductionAmount = 0,
      costOfMaterials = this.costOfMaterials.map(_ => 0)
    )
  }
}

object PeriodData extends Logging {

  implicit val monthRead: Reads[PeriodData] =
    ((JsPath \ "deductionFromDate").read[String] and
      (JsPath \ "deductionToDate").read[String] and
      (JsPath \ "deductionAmount").readNullable[BigDecimal] and
      (JsPath \ "costOfMaterials").readNullable[BigDecimal] and
      (JsPath \ "grossAmountPaid").readNullable[BigDecimal] and
      (JsPath \ "submissionDate").read[String] and
      (JsPath \ "submissionId").readNullable[String] and
      (JsPath \ "source").read[String]) (
      (from: String, to: String, deductionAmount: Option[BigDecimal], costOfMaterials: Option[BigDecimal], grossAmountPaid: Option[BigDecimal],
       submissionDate: String, submissionId: Option[String], source: String) =>
        PeriodData(validatePeriodDatesAndReturnMonth(from, to), deductionAmount, costOfMaterials, grossAmountPaid,
          submissionDate, submissionId, source)
    )

  private def validatePeriodDatesAndReturnMonth(fromDate: String, toDate: String): Month = {
    try {
      val parsedFromDate = LocalDate.parse(fromDate)
      lazy val parsedToDate = LocalDate.parse(toDate)

      lazy val validMonths = parsedFromDate.plusMonths(1).getMonth == parsedToDate.getMonth
      lazy val validDay = parsedFromDate.minusDays(1).getDayOfMonth == parsedToDate.getDayOfMonth

      if (validMonths && validDay) {
        parsedToDate.getMonth
      } else {
        pagerDutyLog(INVALID_PERIOD_DATES,
          s"[PeriodData][validatePeriodDatesAndReturnMonth] The retrieved period dates are invalid. fromDate - $fromDate, toDate - $toDate")
        parsedToDate.getMonth
      }
    }
    catch {
      case exception: Exception =>
        pagerDutyLog(INVALID_PERIOD_DATES,
          s"[PeriodData][validatePeriodDatesAndReturnMonth] The retrieved period dates are invalid. ${exception.getMessage}")
        throw new Exception("The retrieved period dates are invalid.")
    }

  }

  implicit val monthWrites: Writes[Month] = Writes { month => JsString(month.toString) }

  implicit val writes: Writes[PeriodData] = Json.writes[PeriodData]
}

