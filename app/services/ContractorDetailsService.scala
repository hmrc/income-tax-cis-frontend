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
import models.forms.ContractorDetails
import models.mongo.{CisCYAModel, CisUserData, DataNotUpdatedError, DatabaseError}
import models.{HttpParserError, ServiceError, User}
import repositories.CisUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ContractorDetailsService @Inject()(cisSessionService: CISSessionService,
                                         cisUserDataRepository: CisUserDataRepository)
                                        (implicit ec: ExecutionContext) {

  // TODO: This method potentially will be deleted and supplemented with containsEmployerRef
  def getPriorEmployerRefs(taxYear: Int,
                           user: User)(implicit hc: HeaderCarrier): Future[Either[HttpParserError, Seq[String]]] = {
    cisSessionService.getPriorData(user, taxYear).map {
      case Left(error) => Left(error)
      case Right(prior) => Right(prior.allEmployerRefs)
    }
  }

  def saveContractorDetails(taxYear: Int,
                            user: User,
                            optCisUserData: Option[CisUserData],
                            contractorDetails: ContractorDetails): Future[Either[ServiceError, CisUserData]] = {
    optCisUserData match {
      case Some(cisUserData) if cisUserData.employerRef != contractorDetails.employerReferenceNumber =>
        cisUserDataRepository.clear(taxYear, cisUserData.employerRef, user).flatMap {
          case true => updateExistingModel(taxYear, user, cisUserData, contractorDetails)
          case false => Future.successful(Left(DataNotUpdatedError))
        }
      case Some(cisUserData) => updateExistingModel(taxYear, user, cisUserData, contractorDetails)
      case None => createNewModel(taxYear, user, contractorDetails)
    }
  }

  private def createNewModel(taxYear: Int,
                             user: User,
                             contractorDetails: ContractorDetails): Future[Either[DatabaseError, CisUserData]] = {
    val newCisCYAModel = CisCYAModel(contractorName = Some(contractorDetails.contractorName))
    val employerRef = contractorDetails.employerReferenceNumber

    cisSessionService.createOrUpdateCISUserData(user, taxYear, employerRef, None, isPriorSubmission = false, newCisCYAModel)
  }

  private def updateExistingModel(taxYear: Int,
                                  user: User,
                                  data: CisUserData,
                                  contractorDetails: ContractorDetails): Future[Either[ServiceError, CisUserData]] = {
    val newCisCYAModel = data.cis.copy(contractorName = Some(contractorDetails.contractorName))
    val employerRef = contractorDetails.employerReferenceNumber

    cisSessionService.createOrUpdateCISUserData(user, taxYear, employerRef, data.submissionId, data.isPriorSubmission, newCisCYAModel)
  }
}
