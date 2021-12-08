package uk.gov.hmrc.incometaxcisfrontend.config

import javax.inject.{Inject, Singleton}

import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import uk.gov.hmrc.incometaxcisfrontend.views.html.ErrorTemplate

@Singleton
class ErrorHandler @Inject()(errorTemplate: ErrorTemplate, val messagesApi: MessagesApi)
    extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    errorTemplate(pageTitle, heading, message)
}
