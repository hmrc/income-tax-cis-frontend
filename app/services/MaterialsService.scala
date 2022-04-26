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

import javax.inject.Inject
import models._
import models.mongo.{CYAPeriodData, CisUserData, DataNotUpdatedError}

import scala.concurrent.{ExecutionContext, Future}

class MaterialsService @Inject()(cisSessionService: CISSessionService)
                                (implicit ec: ExecutionContext) {

  def saveQuestion(user: User,
                   cisUserData: CisUserData,
                   question: Boolean): Future[Either[ServiceError, Unit]] = {
    val periodData: CYAPeriodData = if (question) {
      cisUserData.cis.periodData.map(_.copy(costOfMaterialsQuestion = Some(question))).get
    } else {
      cisUserData.cis.periodData.map(_.copy(costOfMaterialsQuestion = Some(question), costOfMaterials = None)).get
    }
    val updatedCYA = cisUserData.cis.copy(periodData = Some(periodData))

    cisSessionService
      .createOrUpdateCISUserData(user, cisUserData.taxYear, cisUserData.employerRef, cisUserData.submissionId, cisUserData.isPriorSubmission, updatedCYA)
      .map {
        case Left(_) => Left(DataNotUpdatedError)
        case Right(_) => Right(())
      }
  }

  def saveAmount(user: User,
                 cisUserData: CisUserData,
                 amount: BigDecimal): Future[Either[ServiceError, Unit]] = {
    val periodData: CYAPeriodData = cisUserData.cis.periodData.map(_.copy(costOfMaterials = Some(amount))).get
    val updatedCYA = cisUserData.cis.copy(periodData = Some(periodData))

    cisSessionService
      .createOrUpdateCISUserData(user, cisUserData.taxYear, cisUserData.employerRef, cisUserData.submissionId, cisUserData.isPriorSubmission, updatedCYA)
      .map {
        case Left(_) => Left(DataNotUpdatedError)
        case Right(_) => Right(())
      }
  }
}
