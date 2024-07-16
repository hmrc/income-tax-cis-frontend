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

package helpers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, WireMock}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.{EnrolmentIdentifiers, EnrolmentKeys}
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import support.builders.models.UserBuilder.aUser
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}

trait WireMockHelper {

  protected val wiremockPort = 11111
  protected val wiremockHost = "localhost"

  protected lazy val wmConfig: WireMockConfiguration = wireMockConfig().port(wiremockPort)
  protected lazy val wireMockServer = new WireMockServer(wmConfig)

  protected def startWiremock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  protected def stopWiremock(): Unit = wireMockServer.stop()

  protected def resetWiremock(): Unit = WireMock.reset()

  protected def verifyPost(uri: String, optBody: Option[String] = None): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = optBody match {
      case Some(body) => uriMapping.withRequestBody(equalTo(body))
      case None => uriMapping
    }
    verify(postRequest)
  }

  protected def verifyGet(uri: String): Unit = {
    verify(getRequestedFor(urlEqualTo(uri)))
  }

  protected def stubGet(url: String, status: Int, body: String): StubMapping =
    stubFor(get(urlMatching(url)).willReturn(aResponse().withStatus(status).withBody(body)))

  protected def stubGetWithHeadersCheck(url: String,
                                        status: Int,
                                        responseBody: String,
                                        sessionHeader: (String, String) = "X-Session-ID" -> aUser.sessionId,
                                        mtdidHeader: (String, String) = "mtditid" -> aUser.mtditid): StubMapping =
    stubFor(get(urlMatching(url))
      .withHeader(sessionHeader._1, equalTo(sessionHeader._2))
      .withHeader(mtdidHeader._1, equalTo(mtdidHeader._2))
      .willReturn(aResponse().withStatus(status).withBody(responseBody))
    )

  protected def stubPost(url: String, status: Int, responseBody: String, requestHeaders: Seq[HttpHeader] = Seq.empty): StubMapping = {
    val mappingWithHeaders: MappingBuilder = requestHeaders.foldLeft(post(urlMatching(url))) { (result, nxt) =>
      result.withHeader(nxt.key(), equalTo(nxt.firstValue()))
    }

    stubFor(mappingWithHeaders.willReturn(aResponse().withStatus(status).withBody(responseBody)))
  }

  protected def stubPut(url: String, status: Int, responseBody: String, requestHeaders: Seq[HttpHeader] = Seq.empty): StubMapping = {
    val mappingWithHeaders: MappingBuilder = requestHeaders.foldLeft(put(urlMatching(url))) { (result, nxt) =>
      result.withHeader(nxt.key(), equalTo(nxt.firstValue()))
    }

    stubFor(mappingWithHeaders.willReturn(aResponse().withStatus(status).withBody(responseBody)))
  }

  protected def stubPatch(url: String, status: Int, responseBody: String): StubMapping =
    stubFor(patch(urlMatching(url)).willReturn(aResponse().withStatus(status).withBody(responseBody)))

  protected def stubDelete(url: String, status: Int): StubMapping =
    stubFor(delete(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status)
      )
    )

  protected def stubDeleteWithHeadersCheck(url: String, status: Int, responseBody: String,
                                           sessionHeader: (String, String), mtdidHeader: (String, String)): StubMapping =
    stubFor(delete(urlMatching(url))
      .withHeader(sessionHeader._1, equalTo(sessionHeader._2))
      .withHeader(mtdidHeader._1, equalTo(mtdidHeader._2))
      .willReturn(aResponse().withStatus(status).withBody(responseBody))
    )

  protected def stubPostWithHeadersCheck(url: String, status: Int, requestBody: String, responseBody: String,
                                         sessionHeader: (String, String), mtdidHeader: (String, String)): StubMapping =
    stubFor(post(urlMatching(url))
      .withHeader(sessionHeader._1, equalTo(sessionHeader._2))
      .withHeader(mtdidHeader._1, equalTo(mtdidHeader._2))
      .withRequestBody(equalTo(requestBody))
      .willReturn(aResponse().withStatus(status).withBody(responseBody))
    )

  private val authoriseUri = "/auth/authorise"

  private val mtditEnrolment = Json.obj(
    "key" -> "HMRC-MTD-IT",
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "MTDITID",
        "value" -> "1234567890"
      )
    )
  )

  private val ninoEnrolment = Json.obj(
    "key" -> "HMRC-NI",
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "NINO",
        "value" -> "AA123456A"
      )
    )
  )

  private val asAgentEnrolment = Json.obj(
    "key" -> EnrolmentKeys.Agent,
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> EnrolmentIdentifiers.agentReference,
        "value" -> "XARN1234567"
      )
    )
  )

  private def successfulAuthResponse(affinityGroup: Option[AffinityGroup], confidenceLevel: ConfidenceLevel, enrolments: JsObject*): JsObject = {
    affinityGroup match {
      case Some(group) => Json.obj(
        "affinityGroup" -> group,
        "allEnrolments" -> enrolments,
        "confidenceLevel" -> confidenceLevel
      )
      case _ => Json.obj(
        "allEnrolments" -> enrolments,
        "confidenceLevel" -> confidenceLevel
      )
    }
  }

  protected def authoriseIndividual(withNino: Boolean = true): StubMapping = {
    stubPost(authoriseUri, OK, Json.prettyPrint(successfulAuthResponse(Some(AffinityGroup.Individual), ConfidenceLevel.L250,
      enrolments = Seq(mtditEnrolment) ++ (if (withNino) Seq(ninoEnrolment) else Seq.empty[JsObject]): _*)))
  }

  protected def authoriseIndividualUnauthorized(): StubMapping = {
    stubPost(authoriseUri, UNAUTHORIZED, Json.prettyPrint(
      successfulAuthResponse(Some(AffinityGroup.Individual), ConfidenceLevel.L250, Seq(mtditEnrolment, ninoEnrolment): _*)
    ))
  }

  protected def authoriseAgent(): StubMapping = {
    stubPost(authoriseUri, OK, Json.prettyPrint(
      successfulAuthResponse(Some(AffinityGroup.Agent), ConfidenceLevel.L250, Seq(asAgentEnrolment, mtditEnrolment): _*)
    ))
  }

  protected def authoriseAgentUnauthorized(): StubMapping = {
    stubPost(authoriseUri, UNAUTHORIZED, Json.prettyPrint(
      successfulAuthResponse(Some(AffinityGroup.Agent), ConfidenceLevel.L250, Seq(asAgentEnrolment, mtditEnrolment): _*)
    ))
  }
}
