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

package controllers

import actions.ActionsProvider
import config.{AppConfig, ErrorHandler}
import featureswitch.core.config.Tailoring
import models.pages.DeductionsSummaryPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.TailoringService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.DeductionsSummaryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeductionsSummaryController @Inject()(actionsProvider: ActionsProvider,
                                            inYearUtil: InYearUtil,
                                            pageView: DeductionsSummaryView,
                                            tailoringService: TailoringService,
                                            errorHandler: ErrorHandler)
                                           (implicit cc: MessagesControllerComponents, appConfig: AppConfig, executionContext: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.priorCisDeductionsData(taxYear).async { implicit request =>
    if (appConfig.isEnabled(Tailoring)){
      tailoringService.getExcludedJourneys(taxYear = taxYear, request.user.nino, request.user.mtditid).map {
        case Left(_) => errorHandler.internalServerError()
        case Right(result) =>
          Ok(pageView(DeductionsSummaryPage(taxYear, inYearUtil.inYear(taxYear), !result.journeys.map(_.journey).contains("cis"), request.incomeTaxUserData)))
      }
    } else{
      Future.successful(Ok(pageView(DeductionsSummaryPage(taxYear, inYearUtil.inYear(taxYear), gateway = false, request.incomeTaxUserData))))
    }
  }
}
