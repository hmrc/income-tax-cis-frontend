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

package models

import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import utils.DateTimeUtil.parseDate

import java.time.Month

case class IncomeTaxUserData(cis: Option[AllCISDeductions] = None) extends Logging {

  val hasInYearCisDeductions: Boolean = cis.exists(_.inYearCisDeductions.nonEmpty)

  def inYearCisDeductionsWith(employerRef: String): Option[CisDeductions] =
    cis.flatMap(_.inYearCisDeductions.find(_.employerRef == employerRef))

  def hasInYearCisDeductionsWith(employerRef: String): Boolean =
    inYearCisDeductionsWith(employerRef).nonEmpty

  def inYearPeriodDataFor(employerRef: String, month: Month): Option[PeriodData] =
    inYearCisDeductionsWith(employerRef).flatMap(_.periodData.find(_.deductionPeriod == month))

  def hasInYearPeriodDataFor(employerRef: String, month: Month): Boolean =
    inYearPeriodDataFor(employerRef, month).nonEmpty

  def inYearPeriodDataWith(employerRef: String): Seq[PeriodData] =
    inYearCisDeductionsWith(employerRef)
      .map(_.periodData)
      .getOrElse(Seq.empty)

  def hasInYearPeriodDataWith(employerRef: String): Boolean = {
    inYearPeriodDataWith(employerRef).nonEmpty
  }

  // TODO: Logging should be moved something else and some refactoring can also be done
  def getCISDeductionsFor(employerRef: String): Option[CisDeductions] = {
    val contractorCISDeductions: Option[CisDeductions] = cis.flatMap(_.contractorCISDeductions.flatMap(_.cisDeductions.find(_.employerRef == employerRef)))
    val customerCISDeductions: Option[CisDeductions] = cis.flatMap(_.customerCISDeductions.flatMap(_.cisDeductions.find(_.employerRef == employerRef)))

    def log(customerLatest: Boolean): Unit = logger.info(s"[IncomeTaxUserData][getCISDeductionsFor] User has both contractor and customer data. " +
      s"The latest data that will be returned will be ${if (customerLatest) "customer" else "contractor"} data.")

    (contractorCISDeductions, customerCISDeductions) match {
      case (Some(contractorCISDeductions), Some(customerCISDeductions)) =>

        val latestContractorSubmissionDate = parseDate(contractorCISDeductions.periodData.maxBy(_.submissionDate).submissionDate)
        val latestCustomerSubmissionDate = parseDate(customerCISDeductions.periodData.maxBy(_.submissionDate).submissionDate)

        Some((latestContractorSubmissionDate, latestCustomerSubmissionDate) match {
          case (Some(contractorSubmission), Some(customerSubmission)) =>
            if (contractorSubmission.isAfter(customerSubmission)) {
              log(false)
              contractorCISDeductions
            } else {
              log(true)
              customerCISDeductions
            }
          case (Some(_), None) =>
            log(false)
            contractorCISDeductions
          case (None, Some(_)) =>
            log(true)
            customerCISDeductions
          case (None, None) =>
            log(true)
            customerCISDeductions
        })

      case (Some(contractorCISDeductions), None) => Some(contractorCISDeductions)
      case (None, Some(customerCISDeductions)) => Some(customerCISDeductions)
      case _ => None
    }
  }
}

object IncomeTaxUserData {
  implicit val formats: OFormat[IncomeTaxUserData] = Json.format[IncomeTaxUserData]
}
