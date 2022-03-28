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

import models.mongo.{CisUserData, DatabaseError}
import models.{ServiceError, User}
import models.pages.ContractorDetailsViewModel
import org.scalamock.handlers.{CallHandler4, CallHandler5}
import org.scalamock.scalatest.MockFactory
import services.ContractorDetailsService

import scala.concurrent.{ExecutionContext, Future}

trait MockContractorDetailsService extends MockFactory{

  protected val mockContractorDetailsService: ContractorDetailsService = mock[ContractorDetailsService]

  def mockCheckAccessContractorDetailsPage(taxYear: Int, user: User, result: Future[Either[ServiceError, Option[CisUserData]]])
                                          (implicit executionContext: ExecutionContext): CallHandler4[Int, User, String, ExecutionContext, Future[Either[ServiceError, Option[CisUserData]]]] = {
    (mockContractorDetailsService.checkAccessContractorDetailsPage(_: Int, _: User, _: String)(_: ExecutionContext))
      .expects(taxYear, user, *, executionContext)
      .returns(result)
  }

  def mockCreateOrUpdateContractorDetails(model: ContractorDetailsViewModel, taxYear: Int, user: User, result: Future[Either[DatabaseError, Unit]])
                                     (implicit executionContext: ExecutionContext): CallHandler5[ContractorDetailsViewModel, Int, User, Option[String], ExecutionContext, Future[Either[DatabaseError, Unit]]] = {
    (mockContractorDetailsService.createOrUpdateContractorDetails(_: ContractorDetailsViewModel, _: Int, _: User, _: Option[String])(_: ExecutionContext))
      .expects(model, taxYear, user, None, executionContext)
      .returns(result)
  }

}
