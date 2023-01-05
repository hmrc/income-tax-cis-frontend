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

import com.google.inject.Inject
import models.mongo.DataNotUpdatedError
import models.{IncomeTaxUserData, ServiceError, User}

import scala.concurrent.{ExecutionContext, Future}

class ContractorSummaryService @Inject()(cisSessionService: CISSessionService)(implicit ec: ExecutionContext) {

  def saveCYAForNewCisDeduction(taxYear: Int,
                                employerRef: String,
                                priorData: IncomeTaxUserData,
                                user: User): Future[Either[ServiceError, Unit]] = {

    val deductions = priorData.eoyCisDeductionsWith(employerRef).get
    val cya = deductions.toCYA(None, priorData.contractorPeriodsFor(employerRef), hasCompleted = false)
    val submissionId = deductions.submissionId
    val isPriorSubmission = true

    cisSessionService.createOrUpdateCISUserData(user, taxYear, employerRef, submissionId, isPriorSubmission, cya).map {
      case Left(_) => Left(DataNotUpdatedError)
      case Right(_) => Right(())
    }
  }
}
