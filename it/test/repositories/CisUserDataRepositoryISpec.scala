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

package repositories

import com.mongodb.MongoTimeoutException
import common.UUID
import models.mongo._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.{MongoException, MongoInternalException, MongoWriteException}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import support.IntegrationTest
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.mongo.MongoUtils
import utils.AesGcmAdCrypto
import utils.PagerDutyHelper.PagerDutyKeys.FAILED_TO_CREATE_UPDATE_CIS_DATA

import java.time.{LocalDateTime, ZoneId}
import scala.concurrent.Future

class CisUserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  private val sessionIdOne = UUID.randomUUID
  private val now = LocalDateTime.now(ZoneId.of("UTC"))
  private val userDataFull = aCisUserData.copy(sessionId = sessionIdOne)
  private val userDataOne = aCisUserData.copy(sessionId = sessionIdOne, taxYear = taxYear, lastUpdated = now)
  private val userOne = anAuthorisationRequest.copy(aUser.copy(userDataOne.mtdItId, None, userDataOne.nino, userDataOne.sessionId, Individual.toString))
  protected implicit val aesGcmAdCrypto: AesGcmAdCrypto = app.injector.instanceOf[AesGcmAdCrypto]
  private val repoWithInvalidEncryption = GuiceApplicationBuilder().configure(config + ("mongodb.encryption.key" -> "key")).build()
    .injector.instanceOf[CisUserDataRepositoryImpl]

  private def count: Long = await(underTest.collection.countDocuments().toFuture())

  private def countFromOtherDatabase: Long = await(underTest.collection.countDocuments().toFuture())

  private val underTest: CisUserDataRepositoryImpl = app.injector.instanceOf[CisUserDataRepositoryImpl]

  class EmptyDatabase {
    await(underTest.collection.drop().toFuture())
    await(underTest.ensureIndexes)
    count mustBe 0
  }

  "update with invalid encryption" should {
    "fail to add data" in new EmptyDatabase {
      countFromOtherDatabase mustBe 0
      val res: Either[DatabaseError, Unit] = await(repoWithInvalidEncryption.createOrUpdate(userDataOne))

      res mustBe Left(EncryptionDecryptionError("Failed encrypting data"))
    }
  }

  "find with invalid encryption" should {
    "fail to find data" in new EmptyDatabase {
      implicit val associatedText: String = userDataOne.mtdItId
      countFromOtherDatabase mustBe 0
      await(repoWithInvalidEncryption.collection.insertOne(userDataOne.encrypted).toFuture())
      countFromOtherDatabase mustBe 1
      await(repoWithInvalidEncryption.find(userDataOne.taxYear, userDataOne.employerRef, userOne.user)) mustBe
        Left(EncryptionDecryptionError("Failed encrypting data"))
    }
  }

  "handleEncryptionDecryptionException" should {
    "handle an exception" in {
      repoWithInvalidEncryption.handleEncryptionDecryptionException(new Exception("fail"), "") mustBe Left(EncryptionDecryptionError("fail"))
    }
  }

  "clear" should {
    "remove a record" in new EmptyDatabase {
      count mustBe 0
      await(underTest.createOrUpdate(userDataOne)) mustBe Right(())
      count mustBe 1

      await(underTest.clear(taxYear, userDataOne.employerRef, userOne.user)) mustBe true
      count mustBe 0
    }
  }

  "createOrUpdate" should {
    "fail to add a document to the collection when a mongo error occurs" in new EmptyDatabase {
      def ensureIndexes: Future[Seq[String]] = {
        val indexes = Seq(IndexModel(ascending("taxYear"), IndexOptions().unique(true).name("fakeIndex")))
        MongoUtils.ensureIndexes(underTest.collection, indexes, replaceIndexes = true)
      }

      await(ensureIndexes)
      count mustBe 0

      await(underTest.createOrUpdate(userDataOne)) mustBe Right(())
      count mustBe 1

      await(underTest.createOrUpdate(userDataOne.copy(sessionId = "1234567890"))).left.toOption.get.message must include("Command failed with error 11000 (DuplicateKey)")
      count mustBe 1
    }

    "create a document in collection when one does not exist" in new EmptyDatabase {
      await(underTest.createOrUpdate(userDataOne)) mustBe Right(())
      count mustBe 1
    }

    "create a document in collection with all fields present" in new EmptyDatabase {
      await(underTest.createOrUpdate(userDataFull)) mustBe Right(())
      count mustBe 1
    }

    "update a document in collection when one already exists" in new EmptyDatabase {
      await(underTest.createOrUpdate(userDataOne)) mustBe Right(())
      count mustBe 1

      private val updatedCisUserData = userDataOne.copy(cis = aCisCYAModel)

      await(underTest.createOrUpdate(updatedCisUserData)) mustBe Right(())
      count mustBe 1
    }

    "create a new document when the same documents exists but the sessionId is different" in new EmptyDatabase {
      await(underTest.createOrUpdate(userDataOne)) mustBe Right(())
      count mustBe 1

      private val newUserData = userDataOne.copy(sessionId = UUID.randomUUID)

      await(underTest.createOrUpdate(newUserData)) mustBe Right(())
      count mustBe 2
    }
  }

  "find" should {
    "get a document and update the TTL" in new EmptyDatabase {
      private val now = LocalDateTime.now(ZoneId.of("UTC"))
      private val data = userDataOne.copy(lastUpdated = now)

      await(underTest.createOrUpdate(data)) mustBe Right(())
      count mustBe 1

      private val findResult: Either[DatabaseError, Option[CisUserData]] = await(underTest.find(data.taxYear, userDataOne.employerRef, userOne.user))

      findResult.map(_.get.copy(lastUpdated = data.lastUpdated)) shouldBe Right(data)
      findResult.map(_.get.lastUpdated.isAfter(data.lastUpdated)) shouldBe Right(true)
    }

    "find a document in collection with all fields present" in new EmptyDatabase {
      await(underTest.createOrUpdate(userDataFull)) mustBe Right(())
      count mustBe 1

      val findResult: Either[DatabaseError, Option[CisUserData]] =
        await(underTest.find(userDataFull.taxYear, userDataOne.employerRef, userOne.user))

      findResult mustBe Right(Some(userDataFull.copy(lastUpdated = findResult.toOption.get.get.lastUpdated)))
    }

    "return None when find operation succeeds but no data is found for the given inputs" in new EmptyDatabase {
      val taxYear = 2021
      await(underTest.find(taxYear, userDataOne.employerRef, userOne.user)) mustBe Right(None)
    }
  }

  "the set indexes" should {
    "enforce uniqueness" in new EmptyDatabase {
      implicit val associatedText: String = userDataOne.mtdItId
      await(underTest.createOrUpdate(userDataOne)) mustBe Right(())
      count mustBe 1

      private val encryptedCISUserData: EncryptedCisUserData = userDataOne.encrypted

      private val caught = intercept[MongoWriteException](await(underTest.collection.insertOne(encryptedCISUserData).toFuture()))

      caught.getMessage must
        include("E11000 duplicate key error collection: income-tax-cis-frontend.cisUserData index: UserDataLookupIndex dup key:")
    }
  }

  "mongoRecover" should {
    Seq(new MongoTimeoutException(""), new MongoInternalException(""), new MongoException("")).foreach { exception =>
      s"recover when the exception is a MongoException or a subclass of MongoException - ${exception.getClass.getSimpleName}" in {
        val result = Future.failed(exception)
          .recover(underTest.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_CIS_DATA, userOne.user))

        await(result) mustBe None
      }
    }

    Seq(new NullPointerException(""), new RuntimeException("")).foreach { exception =>
      s"not recover when the exception is not a subclass of MongoException - ${exception.getClass.getSimpleName}" in {
        val result = Future.failed(exception)
          .recover(underTest.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_CIS_DATA, userOne.user))

        assertThrows[RuntimeException] {
          await(result)
        }
      }
    }
  }
}
