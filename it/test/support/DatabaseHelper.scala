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

package support

import models.User
import models.mongo.CisUserData
import org.mongodb.scala.Document
import repositories.CisUserDataRepositoryImpl

trait DatabaseHelper {
  self: IntegrationTest =>

  lazy val cisDatabase: CisUserDataRepositoryImpl = app.injector.instanceOf[CisUserDataRepositoryImpl]

  def dropCISDB(): Unit = {
    await(cisDatabase.collection.deleteMany(filter = Document()).toFuture())
    await(cisDatabase.ensureIndexes)
  }

  def insertCyaData(cya: CisUserData): Unit = {
    await(cisDatabase.createOrUpdate(cya))
  }

  def findCyaData(taxYear: Int, employerRef: String, user: User): Option[CisUserData] = {
    await(cisDatabase.find(taxYear, employerRef, user).map {
      case Left(_) => None
      case Right(value) => value
    })
  }
}
