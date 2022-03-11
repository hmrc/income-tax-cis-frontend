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

package repositories

import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.set
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.User
import models.mongo._
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.MongoException
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.PagerDutyHelper.PagerDutyKeys.{FAILED_TO_CREATE_UPDATE_CIS_DATA, FAILED_TO_ClEAR_CIS_DATA, FAILED_TO_FIND_CIS_DATA}
import utils.PagerDutyHelper.{PagerDutyKeys, pagerDutyLog}
import utils.SecureGCMCipher

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class CisUserDataRepositoryImpl @Inject()(mongo: MongoComponent, appConfig: AppConfig)
                                         (implicit secureGCMCipher: SecureGCMCipher, ec: ExecutionContext) extends PlayMongoRepository[EncryptedCisUserData](
  mongoComponent = mongo,
  collectionName = "cisUserData",
  domainFormat = EncryptedCisUserData.formats,
  indexes = CisUserDataIndexes.indexes(appConfig)
) with Repository with CisUserDataRepository with Logging {

  def find(taxYear: Int, employerRef: String, user: User): Future[Either[DatabaseError, Option[CisUserData]]] = {

    lazy val start = "[CisUserDataRepositoryImpl][find]"

    val queryFilter = filter(user.sessionId, user.mtditid, user.nino, taxYear, employerRef)
    val update = set("lastUpdated", toBson(DateTime.now(DateTimeZone.UTC))(MongoJodaFormats.dateTimeWrites))
    val options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)

    val findResult = collection.findOneAndUpdate(queryFilter, update, options).toFutureOption().map(Right(_)).recover {
      case exception: Exception =>
        pagerDutyLog(FAILED_TO_FIND_CIS_DATA, s"$start Failed to find user data. Exception: ${exception.getMessage}")
        Left(MongoError(exception.getMessage))
    }

    findResult.map {
      case Left(error) => Left(error)
      case Right(encryptedData) =>
        Try {
          encryptedData.map { encryptedCisUserData: EncryptedCisUserData =>
            implicit val textAndKey: TextAndKey = TextAndKey(encryptedCisUserData.mtdItId, appConfig.encryptionKey)
            encryptedCisUserData.decrypted
          }
        }.toEither match {
          case Left(exception: Exception) => handleEncryptionDecryptionException(exception, start)
          case Right(decryptedData) => Right(decryptedData)
        }
    }
  }

  def createOrUpdate(cisUserData: CisUserData): Future[Either[DatabaseError, Unit]] = {
    lazy val start = "[CisUserDataRepositoryImpl][update]"

    Try {
      implicit val textAndKey: TextAndKey = TextAndKey(cisUserData.mtdItId, appConfig.encryptionKey)
      cisUserData.encrypted
    }.toEither match {
      case Left(exception: Exception) => Future.successful(handleEncryptionDecryptionException(exception, start))
      case Right(encryptedData) =>

        val queryFilter = filter(encryptedData.sessionId, encryptedData.mtdItId, encryptedData.nino, encryptedData.taxYear, encryptedData.employerRef)
        val replacement = encryptedData
        val options = FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)

        collection.findOneAndReplace(queryFilter, replacement, options).toFutureOption().map {
          case Some(_) => Right(())
          case None =>
            pagerDutyLog(FAILED_TO_CREATE_UPDATE_CIS_DATA, s"$start Failed to update user data.")
            Left(DataNotUpdated)
        }.recover {
          case exception: Exception =>
            pagerDutyLog(FAILED_TO_CREATE_UPDATE_CIS_DATA, s"$start Failed to update user data. Exception: ${exception.getMessage}")
            Left(MongoError(exception.getMessage))
        }
    }
  }

  def clear(taxYear: Int, employerRef: String, user: User): Future[Boolean] =
    collection.deleteOne(filter(user.sessionId, user.mtditid, user.nino, taxYear, employerRef))
      .toFutureOption()
      .recover(mongoRecover("Clear", FAILED_TO_ClEAR_CIS_DATA, user))
      .map(_.exists(_.wasAcknowledged()))

  def mongoRecover[T](operation: String, pagerDutyKey: PagerDutyKeys.Value,
                      user: User): PartialFunction[Throwable, Option[T]] = new PartialFunction[Throwable, Option[T]] {

    override def isDefinedAt(x: Throwable): Boolean = x.isInstanceOf[MongoException]

    override def apply(e: Throwable): Option[T] = {
      pagerDutyLog(
        pagerDutyKey,
        s"[CisUserDataRepositoryImpl][$operation] Failed to create cis user data. Error:${e.getMessage}. SessionId: ${user.sessionId}"
      )
      None
    }
  }
}

trait CisUserDataRepository {
  def createOrUpdate(userData: CisUserData): Future[Either[DatabaseError, Unit]]

  def find(taxYear: Int, employerRef: String, user: User): Future[Either[DatabaseError, Option[CisUserData]]]

  def clear(taxYear: Int, employerRef: String, user: User): Future[Boolean]
}
