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

import models.mongo._
import models.{ServiceError, User}

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeductionPeriodService @Inject()(cisSessionService: CISSessionService)(implicit val ec: ExecutionContext) {

  def submitDeductionPeriod(taxYear: Int, employerRef: String, user: User, deductionPeriod: Month,
                            tempEmployerRef: Option[String]): Future[Either[ServiceError, CisUserData]] = {

    cisSessionService.getSessionData(taxYear, employerRef, user, tempEmployerRef).flatMap {
      case Right(Some(cisUserData)) =>

        lazy val default = CYAPeriodData(deductionPeriod = deductionPeriod, contractorSubmitted = false, originallySubmittedPeriod = None)
        val cya = cisUserData.cis

        val periodData = cya.periodData.map(_.copy(deductionPeriod = deductionPeriod)).getOrElse(default)
        val updatedCYA = cya.copy(periodData = Some(periodData))

        cisSessionService.createOrUpdateCISUserData(user, taxYear, employerRef, cisUserData.submissionId, cisUserData.isPriorSubmission, updatedCYA).map {
          case Left(_) => Left(DataNotUpdatedError)
          case Right(value) => Right(value)
        }

      case _ => Future.successful(Left(DataNotFoundError))
    }
  }
}
