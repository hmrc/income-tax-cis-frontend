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

import models.mongo.CisUserData
import models.{ServiceError, User}
import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import services.MaterialsService

import scala.concurrent.Future

trait MockMaterialsService extends MockFactory {

  protected val mockMaterialsService: MaterialsService = mock[MaterialsService]

  def mockSaveQuestion(user: User,
                       cisUserData: CisUserData,
                       questionValue: Boolean,
                       result: Either[ServiceError, CisUserData]): CallHandler3[User, CisUserData, Boolean, Future[Either[ServiceError, CisUserData]]] = {
    (mockMaterialsService.saveQuestion(_: User, _: CisUserData, _: Boolean))
      .expects(user, cisUserData, questionValue)
      .returning(Future.successful(result))
  }

  def mockSaveAmount(user: User,
                     cisUserData: CisUserData,
                     amount: BigDecimal,
                     result: Either[ServiceError, Unit]): CallHandler3[User, CisUserData, BigDecimal, Future[Either[ServiceError, Unit]]] = {
    (mockMaterialsService.saveAmount(_: User, _: CisUserData, _: BigDecimal))
      .expects(user, cisUserData, amount)
      .returning(Future.successful(result))
  }
}
