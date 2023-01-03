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

package models

import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import utils.DateTimeUtil.parseDate

import java.time.Month

case class AllCISDeductions(customerCISDeductions: Option[CISSource],
                            contractorCISDeductions: Option[CISSource]) extends Logging {

  // TODO: This method could be removed when ContractorDetailsService is refactored
  def allEmployerRefs: Seq[String] =
    (customerCISDeductions.map(_.allEmployerRefs).getOrElse(Seq.empty) ++ contractorCISDeductions.map(_.allEmployerRefs).getOrElse(Seq.empty)).distinct

  def contractorPeriodsFor(employerRef: String): Seq[Month] = {
    val cisDeductions = contractorCISDeductions.flatMap(_.cisDeductions.find(_.employerRef == employerRef))
    cisDeductions.map(_.periodData.map(_.deductionPeriod)).getOrElse(Seq.empty)
  }

  lazy val endOfYearCisDeductions: Seq[CisDeductions] = {
    val _contractorCISDeductions: Seq[CisDeductions] = contractorCISDeductions.map(_.cisDeductions).getOrElse(Seq.empty)
    val _customerCISDeductions: Seq[CisDeductions] = customerCISDeductions.map(_.cisDeductions).getOrElse(Seq.empty)

    makeCISDeductionsListFromCustomerAndContractor(_contractorCISDeductions, _customerCISDeductions).map(_.recalculateFigures)
  }

  lazy val inYearCisDeductions: Seq[CisDeductions] = contractorCISDeductions
    .map(_.cisDeductions)
    .getOrElse(Seq.empty)

  def inYearCisDeductionsWith(employerRef: String): Option[CisDeductions] =
    inYearCisDeductions.find(_.employerRef == employerRef)

  def eoyCisDeductionsWith(employerRef: String): Option[CisDeductions] =
    endOfYearCisDeductions.find(_.employerRef == employerRef)

  def customerCisDeductionsWith(employerRef: String): Option[CisDeductions] =
    customerCISDeductions.flatMap(_.cisDeductionsWith(employerRef))

  def contractorCisDeductionsWith(employerRef: String): Option[CisDeductions] =
    contractorCISDeductions.flatMap(_.cisDeductionsWith(employerRef))

  private def makeCISDeductionsListFromCustomerAndContractor(contractorCISDeductions: Seq[CisDeductions],
                                                             customerCISDeductions: Seq[CisDeductions]): Seq[CisDeductions] = {
    val contractorEmployers = contractorCISDeductions.map(_.employerRef)
    val customerEmployers = customerCISDeductions.map(_.employerRef)
    val employersInBoth = contractorEmployers.filter(contractorEmployer => customerEmployers.contains(contractorEmployer))

    val onlyContractorCISDeductions = contractorCISDeductions.filterNot(cisDeductions => employersInBoth.contains(cisDeductions.employerRef))
    val onlyCustomerCISDeductions = customerCISDeductions.filterNot(cisDeductions => employersInBoth.contains(cisDeductions.employerRef))

    if (employersInBoth.isEmpty) {
      (onlyContractorCISDeductions ++ onlyCustomerCISDeductions).sortBy(_.contractorName)
    } else {
      val _contractorCISDeductions = contractorCISDeductions.filter(cisDeductions => employersInBoth.contains(cisDeductions.employerRef))
      val _customerCISDeductions = customerCISDeductions.filter(cisDeductions => employersInBoth.contains(cisDeductions.employerRef))
      val latestCombinedData = employersInBoth.map { employerRef =>
        latestDataForEmployerRef(employerRef, _contractorCISDeductions, _customerCISDeductions)
      }

      (latestCombinedData ++ onlyContractorCISDeductions ++ onlyCustomerCISDeductions).sortBy(_.contractorName)
    }
  }

  private def latestDataForEmployerRef(employerRef: String,
                                       contractorCISDeductions: Seq[CisDeductions],
                                       customerCISDeductions: Seq[CisDeductions]): CisDeductions = {

    val foundContractorCISDeductions: CisDeductions = contractorCISDeductions.find(_.employerRef == employerRef).get
    val foundCustomerCISDeductions: CisDeductions = customerCISDeductions.find(_.employerRef == employerRef).get
    val contractorSubmissions = foundContractorCISDeductions.periodData
    val customerSubmissions = foundCustomerCISDeductions.periodData

    val deductionPeriodsInBoth = contractorSubmissions.filter(contractorSubmission =>
      customerSubmissions.map(_.deductionPeriod).contains(contractorSubmission.deductionPeriod))

    val onlyContractorDeductionPeriods = contractorSubmissions.filterNot(contractorSubmission =>
      deductionPeriodsInBoth.map(_.deductionPeriod).contains(contractorSubmission.deductionPeriod))
    val onlyCustomerDeductionPeriods = customerSubmissions.filterNot(customerSubmission =>
      deductionPeriodsInBoth.map(_.deductionPeriod).contains(customerSubmission.deductionPeriod))

    val nonDuplicatePeriods = onlyContractorDeductionPeriods ++ onlyCustomerDeductionPeriods

    if (deductionPeriodsInBoth.isEmpty) {
      foundContractorCISDeductions.copy(periodData = nonDuplicatePeriods).withSortedPeriodData
    } else {
      val _contractorDeductionPeriods = contractorSubmissions.filter(contractorSubmission =>
        deductionPeriodsInBoth.map(_.deductionPeriod).contains(contractorSubmission.deductionPeriod))
      val _customerDeductionPeriods = customerSubmissions.filter(customerSubmission =>
        deductionPeriodsInBoth.map(_.deductionPeriod).contains(customerSubmission.deductionPeriod))
      val latestCombinedPeriods = deductionPeriodsInBoth.map { deductionPeriod =>
        latestPeriodData(deductionPeriod, _contractorDeductionPeriods, _customerDeductionPeriods)
      }

      foundContractorCISDeductions.copy(periodData = latestCombinedPeriods ++ nonDuplicatePeriods).withSortedPeriodData
    }
  }

  private def latestPeriodData(periodData: PeriodData,
                               contractorSubmissions: Seq[PeriodData],
                               customerSubmissions: Seq[PeriodData]): PeriodData = {
    val contractorPeriod: PeriodData = contractorSubmissions.find(_.deductionPeriod == periodData.deductionPeriod).get
    val customerPeriod: PeriodData = customerSubmissions.find(_.deductionPeriod == periodData.deductionPeriod).get
    val latestContractorSubmissionDate = parseDate(contractorPeriod.submissionDate)
    val latestCustomerSubmissionDate = parseDate(customerPeriod.submissionDate)

    (latestContractorSubmissionDate, latestCustomerSubmissionDate) match {
      case (Some(contractorSubmission), Some(customerSubmission)) =>
        if (contractorSubmission.isAfter(customerSubmission)) contractorPeriod else customerPeriod
      case (Some(_), None) => contractorPeriod
      case _ => customerPeriod
    }
  }
}

object AllCISDeductions {
  implicit val format: OFormat[AllCISDeductions] = Json.format[AllCISDeductions]
}
