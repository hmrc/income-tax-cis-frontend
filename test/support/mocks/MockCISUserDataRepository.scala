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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import repositories.CisUserDataRepository

import scala.concurrent.Future

trait MockCISUserDataRepository { this: TestSuite =>


  protected val mockCisUserDataRepository: CisUserDataRepository =
    org.mockito.Mockito.mock(classOf[CisUserDataRepository])

  def mockFindCYAData(taxYear: Int, employerRef: String, user: User,
                      result: Either[DatabaseError, Option[CisUserData]]): Unit = {
    when(
      mockCisUserDataRepository.find(
        eqTo(taxYear),
        eqTo(employerRef),
        eqTo(user)
      )
    ).thenReturn(Future.successful(result))
  }

  def mockCreateOrUpdateCYAData(data: CisUserData,
                                result: Either[DatabaseError, Unit]): Unit = {
    when(
      mockCisUserDataRepository.createOrUpdate(eqTo(data))
    ).thenReturn(Future.successful(result))
  }

  def mockClear(taxYear: Int, employerRef: String,
                result: Boolean): Unit = {
    when(
      mockCisUserDataRepository.clear(
        eqTo(taxYear),
        eqTo(employerRef),
        any[User]()
      )
    ).thenReturn(Future.successful(result))
  }
}
