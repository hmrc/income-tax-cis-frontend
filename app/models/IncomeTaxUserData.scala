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

import play.api.libs.json.{Json, OFormat}

import java.time.Month

case class IncomeTaxUserData(cis: Option[AllCISDeductions] = None) {

  val hasInYearCisDeductions: Boolean = cis.exists(_.inYearCisDeductions.nonEmpty)

  def inYearCisDeductionsWith(employerRef: String): Option[CisDeductions] =
    cis.flatMap(_.inYearCisDeductions.find(_.employerRef == employerRef))

  def hasInYearCisDeductionsWith(employerRef: String): Boolean =
    inYearCisDeductionsWith(employerRef).nonEmpty

  def inYearPeriodDataFor(employerRef: String, month: Month): Option[PeriodData] =
    inYearCisDeductionsWith(employerRef).flatMap(_.periodData.find(_.deductionPeriod == month))

  def hasInYearPeriodDataFor(employerRef: String, month: Month): Boolean =
    inYearPeriodDataFor(employerRef, month).nonEmpty
}

object IncomeTaxUserData {
  implicit val formats: OFormat[IncomeTaxUserData] = Json.format[IncomeTaxUserData]
}
