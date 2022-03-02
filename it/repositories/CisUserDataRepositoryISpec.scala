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

import com.mongodb.MongoTimeoutException
import common.UUID
import models.AuthorisationRequest
import models.mongo._
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.{MongoException, MongoInternalException, MongoWriteException}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.mongo.MongoUtils
import utils.PagerDutyHelper.PagerDutyKeys.FAILED_TO_CREATE_UPDATE_CIS_DATA
import utils.{IntegrationTest, SecureGCMCipher}

import scala.concurrent.Future

class CisUserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  private val taxYear = 2022
  private val sessionIdOne = UUID.randomUUID
  private val now = DateTime.now(DateTimeZone.UTC)
  private val userDataFull = aCisUserData.copy(sessionId = sessionIdOne)
  private val userDataOne = aCisUserData.copy(sessionId = sessionIdOne, taxYear = taxYear, lastUpdated = now)
  private val userOne = AuthorisationRequest(models.User(userDataOne.mtdItId, None, userDataOne.nino, userDataOne.sessionId, Individual.toString), fakeRequest)
  private val repoWithInvalidEncryption = appWithInvalidEncryptionKey.injector.instanceOf[CisUserDataRepositoryImpl]
  private implicit val secureGCMCipher: SecureGCMCipher = app.injector.instanceOf[SecureGCMCipher]

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
      val res: Either[DatabaseError, Unit] = await(repoWithInvalidEncryption.createOrUpdate(userDataOne)(userOne))
      res mustBe Left(EncryptionDecryptionError(
        "Key being used is not valid. It could be due to invalid encoding, wrong length or uninitialized for encrypt Invalid AES key length: 2 bytes"))
    }
  }

  "find with invalid encryption" should {
    "fail to find data" in new EmptyDatabase {
      implicit val textAndKey: TextAndKey = TextAndKey(userDataOne.mtdItId, appConfig.encryptionKey)
      countFromOtherDatabase mustBe 0
      await(repoWithInvalidEncryption.collection.insertOne(userDataOne.encrypted).toFuture())
      countFromOtherDatabase mustBe 1
      private val res = await(repoWithInvalidEncryption.find(userDataOne.taxYear)(userOne))
      res mustBe Left(EncryptionDecryptionError(
        "Key being used is not valid. It could be due to invalid encoding, wrong length or uninitialized for decrypt Invalid AES key length: 2 bytes"))
    }
  }

  "handleEncryptionDecryptionException" should {
    "handle an exception" in {
      val res = repoWithInvalidEncryption.handleEncryptionDecryptionException(new Exception("fail"), "")
      res mustBe Left(EncryptionDecryptionError("fail"))
    }
  }

  "clear" should {
    "remove a record" in new EmptyDatabase {
      count mustBe 0
      await(underTest.createOrUpdate(userDataOne)(userOne)) mustBe Right()
      count mustBe 1

      await(underTest.clear(taxYear)(userOne)) mustBe true
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

      private val res = await(underTest.createOrUpdate(userDataOne)(userOne))
      res mustBe Right()
      count mustBe 1

      private val res2 = await(underTest.createOrUpdate(userDataOne.copy(sessionId = "1234567890"))(userOne))
      res2.left.get.message must include("Command failed with error 11000 (DuplicateKey)")
      count mustBe 1
    }

    "create a document in collection when one does not exist" in new EmptyDatabase {
      await(underTest.createOrUpdate(userDataOne)(userOne)) mustBe Right()
      count mustBe 1
    }

    "create a document in collection with all fields present" in new EmptyDatabase {
      await(underTest.createOrUpdate(userDataFull)(userOne)) mustBe Right()
      count mustBe 1
    }

    "update a document in collection when one already exists" in new EmptyDatabase {
      await(underTest.createOrUpdate(userDataOne)(userOne)) mustBe Right()
      count mustBe 1

      private val updatedCisUserData = userDataOne.copy(cis = Some(aCisCYAModel))

      await(underTest.createOrUpdate(updatedCisUserData)(userOne)) mustBe Right()
      count mustBe 1
    }

    "create a new document when the same documents exists but the sessionId is different" in new EmptyDatabase {
      await(underTest.createOrUpdate(userDataOne)(userOne)) mustBe Right()
      count mustBe 1

      private val newUserData = userDataOne.copy(sessionId = UUID.randomUUID)

      await(underTest.createOrUpdate(newUserData)(userOne)) mustBe Right()
      count mustBe 2
    }
  }

  "find" should {
    "get a document and update the TTL" in new EmptyDatabase {
      private val now = DateTime.now(DateTimeZone.UTC)
      private val data = userDataOne.copy(lastUpdated = now)

      await(underTest.createOrUpdate(data)(userOne)) mustBe Right()
      count mustBe 1

      private val findResult = await(underTest.find(data.taxYear)(userOne))

      findResult.right.get.map(_.copy(lastUpdated = data.lastUpdated)) mustBe Some(data)
      findResult.right.get.map(_.lastUpdated.isAfter(data.lastUpdated)) mustBe Some(true)
    }

    "find a document in collection with all fields present" in new EmptyDatabase {
      await(underTest.createOrUpdate(userDataFull)(userOne)) mustBe Right()
      count mustBe 1

      val findResult: Either[DatabaseError, Option[CisUserData]] = {
        await(underTest.find(userDataFull.taxYear)(userOne))
      }

      findResult mustBe Right(Some(userDataFull.copy(lastUpdated = findResult.right.get.get.lastUpdated)))
    }

    "return None when find operation succeeds but no data is found for the given inputs" in new EmptyDatabase {
      val taxYear = 2021
      await(underTest.find(taxYear)(userOne)) mustBe Right(None)
    }
  }

  "the set indexes" should {
    "enforce uniqueness" in new EmptyDatabase {
      implicit val textAndKey: TextAndKey = TextAndKey(userDataOne.mtdItId, appConfig.encryptionKey)
      await(underTest.createOrUpdate(userDataOne)(userOne)) mustBe Right()
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
          .recover(underTest.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_CIS_DATA)(userOne))

        await(result) mustBe None
      }
    }

    Seq(new NullPointerException(""), new RuntimeException("")).foreach { exception =>
      s"not recover when the exception is not a subclass of MongoException - ${exception.getClass.getSimpleName}" in {
        val result = Future.failed(exception)
          .recover(underTest.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_CIS_DATA)(userOne))

        assertThrows[RuntimeException] {
          await(result)
        }
      }
    }
  }
}
