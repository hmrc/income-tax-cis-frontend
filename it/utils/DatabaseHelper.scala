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

package utils

import models.AuthorisationRequest
import models.mongo.CisUserData
import repositories.CisUserDataRepositoryImpl

trait DatabaseHelper {
  self: IntegrationTest =>

  lazy val cisDatabase: CisUserDataRepositoryImpl = app.injector.instanceOf[CisUserDataRepositoryImpl]

  //noinspection ScalaStyle
  def dropCISDB(): Unit = {
    await(cisDatabase.collection.drop().toFutureOption())
    await(cisDatabase.ensureIndexes)
  }

  //noinspection ScalaStyle
  def insertCyaData(cya: CisUserData, request: AuthorisationRequest[_]): Unit = {
    await(cisDatabase.createOrUpdate(cya)(request))
  }

  //noinspection ScalaStyle
  def findCyaData(taxYear: Int, request: AuthorisationRequest[_]): Option[CisUserData] = {
    await(cisDatabase.find(taxYear)(request).map {
      case Left(_) => None
      case Right(value) => value
    })
  }
}
