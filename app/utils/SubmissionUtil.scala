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

package utils

import models.submission.{CISSubmission, PeriodData}

object SubmissionUtil {

  private def hasValidPeriodDataForSubmission(periodDataForSubmission: Seq[Option[PeriodData]]): Boolean = {

    val allPeriodsComplete = periodDataForSubmission.forall(_.isDefined)
    lazy val allCompletedPeriods = periodDataForSubmission.flatten
    lazy val noDuplicatePeriods = allCompletedPeriods.map(_.deductionToDate).distinct.size == allCompletedPeriods.size
    lazy val validPeriodAmount = allCompletedPeriods.size <= 12

    allPeriodsComplete && allCompletedPeriods.nonEmpty && noDuplicatePeriods && validPeriodAmount
  }

  def validateDataAndCreateSubmission(data: Seq[Option[PeriodData]],
                                      submissionId: Option[String],
                                      contractorName: Option[String],
                                      employerRef: String): Option[CISSubmission] = {

    if (hasValidPeriodDataForSubmission(data)) {
      submissionId match {
        case Some(submissionId) =>
          Some(CISSubmission(employerRef = None, contractorName = None, periodData = data.flatten, submissionId = Some(submissionId)))
        case None =>
          if (contractorName.isDefined) {
            Some(CISSubmission(employerRef = Some(employerRef), contractorName = Some(contractorName.get), periodData = data.flatten, submissionId = None))
          } else {
            None
          }
      }
    } else {
      None
    }
  }
}
