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

import com.google.inject.Inject
import models.forms.ContractorDetailsFormData
import models.mongo.{CisCYAModel, CisUserData, DataNotUpdatedError, DatabaseError}
import models.{ServiceError, User}
import repositories.CisUserDataRepository

import scala.concurrent.{ExecutionContext, Future}

class ContractorDetailsService @Inject()(cisSessionService: CISSessionService,
                                         cisUserDataRepository: CisUserDataRepository)
                                        (implicit ec: ExecutionContext) {

  def saveContractorDetails(taxYear: Int,
                            user: User,
                            optCisUserData: Option[CisUserData],
                            formData: ContractorDetailsFormData): Future[Either[ServiceError, Unit]] = {
    optCisUserData match {
      case Some(data) =>
        if (data.employerRef != formData.employerReferenceNumber) {
          cisUserDataRepository.clear(taxYear, data.employerRef, user).flatMap {
            case true => updateExistingDataAndSave(taxYear, user, data, formData)
            case false => Future.successful(Left(DataNotUpdatedError))
          }
        } else {
          updateExistingDataAndSave(taxYear, user, data, formData)
        }
      case None =>
        val newCisCYAModel = CisCYAModel(contractorName = Some(formData.contractorName))
        val employerRef = formData.employerReferenceNumber

        cisSessionService.createOrUpdateCISUserData(user, taxYear, employerRef, None, isPriorSubmission = false, newCisCYAModel).map(handleResponse)
    }
  }

  private def updateExistingDataAndSave(taxYear: Int,
                                user: User,
                                data: CisUserData,
                                formData: ContractorDetailsFormData): Future[Either[ServiceError, Unit]] = {
    val newCisCYAModel = data.cis.copy(contractorName = Some(formData.contractorName))
    val employerRef = formData.employerReferenceNumber

    cisSessionService.createOrUpdateCISUserData(user, taxYear, employerRef, data.submissionId, data.isPriorSubmission, newCisCYAModel).map(handleResponse)
  }

  private def handleResponse(response: Either[DatabaseError, CisUserData]): Either[DatabaseError, Unit] ={
    response match {
      case Left(error) => Left(error)
      case Right(_) => Right(())
    }
  }
}
