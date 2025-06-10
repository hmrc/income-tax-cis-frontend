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

package config

import play.api.http.Status._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{Request, RequestHeader, Result}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject()(internalServerErrorTemplate: InternalServerErrorTemplate,
                             serviceUnavailableTemplate: ServiceUnavailableTemplate,
                             val messagesApi: MessagesApi,
                             notFoundTemplate: NotFoundTemplate)
                            (implicit appConfig: AppConfig, override val ec: ExecutionContext) extends FrontendErrorHandler with I18nSupport {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: RequestHeader): Future[Html] = {
    implicit val request: Request[_] = rh.withBody(EmptyContent)
    Future.successful(internalServerErrorTemplate())
  }

  override def notFoundTemplate(implicit rh: RequestHeader): Future[Html] = {
    implicit val request: Request[_] = rh.withBody(EmptyContent)
    Future.successful(notFoundTemplate())
  }

  def internalServerError()(implicit request: Request[_]): Result = {
    InternalServerError(internalServerErrorTemplate())
  }

  def handleError(status: Int)(implicit request: Request[_]): Result = {
    status match {
      case SERVICE_UNAVAILABLE => ServiceUnavailable(serviceUnavailableTemplate())
      case _ => InternalServerError(internalServerErrorTemplate())
    }
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    statusCode match {
      case NOT_FOUND =>
        implicit val req: Request[_] = request.withBody(EmptyContent)
        Future.successful(NotFound(notFoundTemplate()))
      case _ => Future.successful(InternalServerError(internalServerErrorTemplate()(request.withBody(body = ""), request2Messages(request), appConfig)))
    }
}
