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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ContractorSummaryService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.UrlUtils._
import utils.{InYearUtil, SessionHelper}
import views.html.ContractorSummaryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContractorSummaryController @Inject()(authorisedAction: AuthorisedAction,
                                            val contractorSummaryService: ContractorSummaryService,
                                            errorHandler: ErrorHandler,
                                            inYearAction: InYearUtil,
                                            view: ContractorSummaryView)
                                           (implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, contractor: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    val employerRef: String = decode(contractor)

    if (inYearAction.inYear(taxYear)) {
      contractorSummaryService.pageModelFor(taxYear, request.user, employerRef).map {
        case Left(HttpParserError(status)) => errorHandler.handleError(status)
        case Left(_) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        case Right(pageModel) => Ok(view(pageModel))
      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

}
