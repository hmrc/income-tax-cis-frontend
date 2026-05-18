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

import models.mongo.CisUserData
import models.{ServiceError, User}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import services.MaterialsService

import scala.concurrent.Future

trait MockMaterialsService { this: TestSuite =>


  protected val mockMaterialsService: MaterialsService =
    org.mockito.Mockito.mock(classOf[MaterialsService])

  def mockSaveQuestion(user: User,
                       cisUserData: CisUserData,
                       questionValue: Boolean,
                       result: Either[ServiceError, CisUserData]): Unit = {
    when(
      mockMaterialsService.saveQuestion(
        eqTo(user),
        eqTo(cisUserData),
        eqTo(questionValue)
      )
    ).thenReturn(Future.successful(result))
  }

  def mockSaveAmount(user: User,
                     cisUserData: CisUserData,
                     amount: BigDecimal,
                     result: Either[ServiceError, Unit]): Unit = {
    when(
      mockMaterialsService.saveAmount(
        eqTo(user),
        eqTo(cisUserData),
        eqTo(amount)
      )
    ).thenReturn(Future.successful(result))
  }
}
