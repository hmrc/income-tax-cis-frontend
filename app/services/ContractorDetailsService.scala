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
import models.mongo.CisUserData.createFrom
import models.mongo.{CisCYAModel, CisUserData, DataNotUpdatedError}
import models.{ServiceError, User}
import repositories.CisUserDataRepository
import utils.Clock

import scala.concurrent.{ExecutionContext, Future}

class ContractorDetailsService @Inject()(cisSessionService: CISSessionService,
                                         cisUserDataRepository: CisUserDataRepository,
                                         clock: Clock)
                                        (implicit ec: ExecutionContext) {

  def saveContractorDetails(taxYear: Int,
                            user: User,
                            optCisUserData: Option[CisUserData],
                            formData: ContractorDetailsFormData): Future[Either[ServiceError, Unit]] = {

    optCisUserData.map(_.employerRef)
      .foreach(employerRef => if (employerRef != formData.employerReferenceNumber) cisUserDataRepository.clear(taxYear, employerRef, user))

    val cisUserData = optCisUserData.getOrElse(createFrom(user, taxYear, formData.employerReferenceNumber, cis = CisCYAModel(), lastUpdated = clock.now()))
    val newCisCYAModel = cisUserData.cis.copy(contractorName = Some(formData.contractorName))
    val employerRef = formData.employerReferenceNumber

    cisSessionService
      .createOrUpdateCISUserData(user, taxYear, employerRef, cisUserData.submissionId, cisUserData.isPriorSubmission, newCisCYAModel)
      .map {
        case Left(_) => Left(DataNotUpdatedError)
        case Right(_) => Right(())
      }
  }
}
