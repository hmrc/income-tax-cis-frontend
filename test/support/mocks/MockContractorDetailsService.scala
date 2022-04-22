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

package support.mocks

import models.forms.ContractorDetailsFormData
import models.mongo.{CisUserData, DatabaseError}
import models.{ServiceError, User}
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import services.ContractorDetailsService

import scala.concurrent.Future

trait MockContractorDetailsService extends MockFactory {

  protected val mockContractorDetailsService: ContractorDetailsService = mock[ContractorDetailsService]

  def mockSaveContractorDetails(taxYear: Int,
                                user: User,
                                optCisUserData: Option[CisUserData],
                                formData: ContractorDetailsFormData,
                                result: Either[DatabaseError, Unit]): CallHandler4[Int, User, Option[CisUserData], ContractorDetailsFormData, Future[Either[ServiceError, Unit]]] = {
    (mockContractorDetailsService.saveContractorDetails _)
      .expects(taxYear, user, optCisUserData, formData)
      .returns(Future.successful(result))
  }
}
