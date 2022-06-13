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

import models.submission.CISSubmission
import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import utils.SubmissionUtil.validateDataAndCreateSubmission

import java.time.Month

case class IncomeTaxUserData(cis: Option[AllCISDeductions] = None) extends Logging {

  def toSubmissionWithoutPeriod(employerRef: String, month: Month, taxYear: Int): Option[CISSubmission] =
    cis.flatMap(_.eoyCisDeductionsWith(employerRef)) match {
      case Some(cisDeductions) =>

        val periodDataForSubmission = cisDeductions.periodData.filterNot(_.deductionPeriod == month).map(_.toSubmissionPeriodData(taxYear))
        lazy val submissionId = cisDeductions.submissionId
        validateDataAndCreateSubmission(periodDataForSubmission, submissionId, cisDeductions.contractorName, employerRef)

      case _ => None
    }

  def contractorPeriodsFor(employerRef: String): Seq[Month] = cis.map(_.contractorPeriodsFor(employerRef)).getOrElse(Seq.empty)

  lazy val hasInYearCisDeductions: Boolean = cis.exists(_.inYearCisDeductions.nonEmpty)

  def inYearCisDeductionsWith(employerRef: String): Option[CisDeductions] =
    cis.flatMap(_.inYearCisDeductionsWith(employerRef))

  def inYearCisDeductionsWith(employerRef: String, month: Month): Option[CisDeductions] =
    inYearCisDeductionsWith(employerRef).find(_.periodDataFor(month).nonEmpty)

  def hasInYearCisDeductionsWith(employerRef: String): Boolean =
    inYearCisDeductionsWith(employerRef).nonEmpty

  def hasInYearCisDeductionsWith(employerRef: String, month: Month): Boolean =
    inYearCisDeductionsWith(employerRef, month).nonEmpty

  def endOfYearCisDeductionsWith(employerRef: String, month: Month): Option[CisDeductions] =
    eoyCisDeductionsWith(employerRef).find(_.periodDataFor(month).nonEmpty)

  def hasEoyCisDeductionsWith(employerRef: String, month: Month): Boolean =
    endOfYearCisDeductionsWith(employerRef, month).nonEmpty

  def hasExclusivelyCustomerEoyCisDeductionsWith(employerRef: String, month: Month): Boolean = {

    val customerCisDeductionForMonth: Option[CisDeductions] = cis.flatMap(_.customerCisDeductionsWith(employerRef)).find(_.periodDataFor(month).nonEmpty)
    val contractorCisDeductionForMonth: Option[CisDeductions] = cis.flatMap(_.contractorCisDeductionsWith(employerRef)).find(_.periodDataFor(month).nonEmpty)

    customerCisDeductionForMonth.isDefined && contractorCisDeductionForMonth.isEmpty
  }

  def inYearPeriodDataWith(employerRef: String): Seq[PeriodData] =
    inYearCisDeductionsWith(employerRef)
      .map(_.periodData)
      .getOrElse(Seq.empty)

  def hasInYearPeriodDataWith(employerRef: String): Boolean = {
    inYearPeriodDataWith(employerRef).nonEmpty
  }

  def eoyCisDeductionsWith(employerRef: String): Option[CisDeductions] =
    cis.flatMap(_.eoyCisDeductionsWith(employerRef))

  def hasEoyCisDeductionsWith(employerRef: String): Boolean =
    eoyCisDeductionsWith(employerRef).nonEmpty

  def customerCisDeductionsWith(employerRef: String): Option[CisDeductions] =
    cis.flatMap(_.customerCisDeductionsWith(employerRef))
}

object IncomeTaxUserData {
  implicit val formats: OFormat[IncomeTaxUserData] = Json.format[IncomeTaxUserData]
}
