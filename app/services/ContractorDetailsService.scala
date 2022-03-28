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
import models.{CisUserIsPriorSubmission, ServiceError, User}
import models.mongo.{CisCYAModel, CisUserData, DatabaseError}
import models.pages.ContractorDetailsViewModel
import repositories.CisUserDataRepository
import utils.Clock

import scala.concurrent.{ExecutionContext, Future}

class ContractorDetailsService @Inject()(cisUserDataRepository: CisUserDataRepository, clock: Clock){

  def checkAccessContractorDetailsPage(taxYear: Int, user: User, employerRef: String)
                                      (implicit executionContext: ExecutionContext): Future[Either[ServiceError, Option[CisUserData]]] = {
    cisUserDataRepository.find(taxYear, employerRef, user).map {
      case Left(error) => Left(error)
      case Right(None) => Right(None)
      case Right(Some(cisUserData)) => if (cisUserData.isPriorSubmission) Left(CisUserIsPriorSubmission) else Right(Some(cisUserData))
    }
  }

  def createOrUpdateContractorDetails(viewModel: ContractorDetailsViewModel, taxYear: Int, user: User, oldRef: Option[String])
                             (implicit ec: ExecutionContext): Future[Either[DatabaseError, Unit]] = {
    cisUserDataRepository.find(taxYear, oldRef.getOrElse(viewModel.employerReferenceNumber), user).flatMap {
      case Left(error) => Future(Left(error))
      case Right(None) => cisUserDataRepository.createOrUpdate(
        CisUserData(
          user.sessionId,
          user.mtditid,
          user.nino,
          taxYear,
          viewModel.employerReferenceNumber,
          None,
          isPriorSubmission = false,
          CisCYAModel(Some(viewModel.contractorName), None),
          clock.now()
        )
      )
      case Right(Some(value)) =>
        if(oldRef.isDefined && !oldRef.contains(viewModel.employerReferenceNumber)) {
          cisUserDataRepository.clear(taxYear, oldRef.get, user)
        }
        cisUserDataRepository.createOrUpdate(
        value.copy(employerRef = viewModel.employerReferenceNumber,
          cis = value.cis.copy(contractorName = Some(viewModel.contractorName))
        ))
    }
  }
}
