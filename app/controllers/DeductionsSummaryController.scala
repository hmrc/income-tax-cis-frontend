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

package controllers

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import models.HttpParserError
import play.api.{Logger, Logging}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DeductionsSummaryService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.DeductionsSummaryView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeductionsSummaryController @Inject()(authAction: AuthorisedAction,
                                            pageView: DeductionsSummaryView,
                                            deductionsSummaryService: DeductionsSummaryService,
                                            errorHandler: ErrorHandler)
                                           (implicit val cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper with Logging {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    logger.info("TEMP_LOGGING: Entered DeductionsSummaryController")
    deductionsSummaryService.pageModelFor(taxYear, request.user).map {
      case Left(HttpParserError(status)) => errorHandler.handleError(status)
      case Left(_) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      case Right(pageModel) => Ok(pageView(pageModel))
    }
  }
}
