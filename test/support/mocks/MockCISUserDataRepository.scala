/*
 * Copyright 2024 HM Revenue & Customs
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

import models.User
import models.mongo.{CisUserData, DatabaseError}
import org.scalamock.handlers.{CallHandler1, CallHandler3}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import repositories.CisUserDataRepository

import scala.concurrent.Future

trait MockCISUserDataRepository extends MockFactory { _: TestSuite =>

  protected val mockCisUserDataRepository: CisUserDataRepository = mock[CisUserDataRepository]

  def mockFindCYAData(taxYear: Int, employerRef: String, user: User,
                      result: Either[DatabaseError, Option[CisUserData]]): CallHandler3[Int, String,
    User, Future[Either[DatabaseError, Option[CisUserData]]]] = {
    (mockCisUserDataRepository.find(_: Int, _: String, _: User))
      .expects(taxYear, employerRef, user)
      .returning(Future.successful(result))
  }

  def mockCreateOrUpdateCYAData(data: CisUserData,
                                result: Either[DatabaseError, Unit]): CallHandler1[CisUserData, Future[Either[DatabaseError, Unit]]] = {
    (mockCisUserDataRepository.createOrUpdate(_: CisUserData))
      .expects(data)
      .returning(Future.successful(result))
  }

  def mockClear(taxYear: Int, employerRef: String,
                result: Boolean): CallHandler3[Int, String, User, Future[Boolean]] = {
    (mockCisUserDataRepository.clear(_: Int, _: String, _: User))
      .expects(taxYear, employerRef, *)
      .returning(Future.successful(result))
  }
}
