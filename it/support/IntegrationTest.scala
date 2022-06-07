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

package support

import actions.AuthorisedAction
import akka.actor.ActorSystem
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.SessionValues
import config.AppConfig
import helpers.{PlaySessionCookieBaker, WireMockHelper, WiremockStubHelpers}
import models.IncomeTaxUserData
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.NO_CONTENT
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import play.api.{Application, Environment, Mode}
import services.AuthService
import support.builders.models.IncomeTaxUserDataBuilder
import support.builders.models.UserBuilder.aUser
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import utils.MockAuthConnector
import views.html.templates.AgentAuthErrorPageView

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait IntegrationTest extends AnyWordSpec
  with Matchers
  with GuiceOneServerPerSuite
  with WireMockHelper
  with WiremockStubHelpers
  with BeforeAndAfterAll
  with TaxYearProvider
  with DatabaseHelper {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> aUser.mtditid)

  implicit val actorSystem: ActorSystem = ActorSystem()

  implicit def wsClient: WSClient = app.injector.instanceOf[WSClient]

  protected def fullUrl(endOfUrl: String): String = s"http://localhost:$port" + endOfUrl

  lazy val appUrl = s"http://localhost:$port/update-and-submit-income-tax-return/construction-industry-scheme-deductions"

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  def config: Map[String, String] = Map(
    "defaultTaxYear" -> taxYear.toString,
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission-frontend.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort.toString,
    "microservice.services.income-tax-cis.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-submission.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.view-and-change.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-nrs-proxy.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.sign-in.url" -> s"/auth-login-stub/gg-sign-in",
    "taxYearErrorFeatureSwitch" -> "false",
    "useEncryption" -> "true"
  )

  def configWithInvalidEncryptionKey: Map[String, String] = Map(
    "defaultTaxYear" -> taxYear.toString,
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission-frontend.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort.toString,
    "microservice.services.income-tax-cis.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-submission.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.view-and-change.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-nrs-proxy.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.sign-in.url" -> s"/auth-login-stub/gg-sign-in",
    "taxYearErrorFeatureSwitch" -> "false",
    "useEncryption" -> "true",
    "mongodb.encryption.key" -> "key"
  )

  def externalConfig: Map[String, String] = Map(
    "defaultTaxYear" -> taxYear.toString,
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission.url" -> s"http://127.0.0.1:$wiremockPort",
    "metrics.enabled" -> "false"
  )

  lazy val agentAuthErrorPage: AgentAuthErrorPageView = app.injector.instanceOf[AgentAuthErrorPageView]

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build

  lazy val appWithFakeExternalCall: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(externalConfig)
    .build

  lazy val appWithInvalidEncryptionKey: Application = GuiceApplicationBuilder()
    .configure(configWithInvalidEncryptionKey)
    .build()

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
    dropCISDB()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  lazy val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]


  val defaultAcceptedConfidenceLevels: Seq[ConfidenceLevel] = Seq(
    ConfidenceLevel.L200,
    ConfidenceLevel.L500
  )

  def authService(stubbedRetrieval: Future[_], acceptedConfidenceLevel: Seq[ConfidenceLevel]): AuthService = new AuthService(
    new MockAuthConnector(stubbedRetrieval, acceptedConfidenceLevel)
  )

  def authAction(
                  stubbedRetrieval: Future[_],
                  acceptedConfidenceLevel: Seq[ConfidenceLevel] = Seq.empty[ConfidenceLevel]
                ): AuthorisedAction = new AuthorisedAction(
    appConfig
  )(
    authService(stubbedRetrieval, if (acceptedConfidenceLevel.nonEmpty) {
      acceptedConfidenceLevel
    } else {
      defaultAcceptedConfidenceLevels
    }),
    mcc
  )

  def successfulRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L200
  )

  def insufficientConfidenceRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L50
  )

  def playSessionCookies(taxYear: Int, validTaxYears: Seq[Int] = validTaxYearList, extraData: Map[String, String] = Map.empty): String = PlaySessionCookieBaker.bakeSessionCookie(Map(
    SessionValues.TAX_YEAR -> taxYear.toString,
    SessionValues.VALID_TAX_YEARS -> validTaxYears.mkString(","),
    SessionKeys.sessionId -> aUser.sessionId,
    SessionValues.CLIENT_NINO -> aUser.nino,
    SessionValues.CLIENT_MTDITID -> aUser.mtditid
  ) ++ extraData)

  def userDataStub(userData: IncomeTaxUserData, nino: String, taxYear: Int): StubMapping = {
    stubGetWithHeadersCheck(
      url = s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear",
      status = OK,
      responseBody = IncomeTaxUserDataBuilder.mapToJsonWrite(userData)(taxYear).toString(),
      sessionHeader = "X-Session-ID" -> aUser.sessionId,
      mtdidHeader = "mtditid" -> aUser.mtditid
    )
  }

  def noUserDataStub(nino: String, taxYear: Int): StubMapping = stubGetWithHeadersCheck(
    url = s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear",
    status = NO_CONTENT,
    responseBody = "{}",
    sessionHeader = "X-Session-ID" -> aUser.sessionId,
    mtdidHeader = "mtditid" -> aUser.mtditid
  )

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
}