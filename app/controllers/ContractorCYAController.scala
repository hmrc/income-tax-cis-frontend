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
import akka.util.ByteString.UTF_8
import config.{AppConfig, ErrorHandler}
import models.HttpParserError
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ContractorCYAService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.ContractorCYAView

import java.net.URLDecoder
import java.time.Month
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ContractorCYAController @Inject()(authAction: AuthorisedAction,
                                        pageView: ContractorCYAView,
                                        contractorCYAService: ContractorCYAService,
                                        errorHandler: ErrorHandler)
                                       (implicit mcc: MessagesControllerComponents, ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, month: String, contractor: String): Action[AnyContent] = authAction.async { implicit request =>
    val employerRef = URLDecoder.decode(contractor, UTF_8)

    contractorCYAService.pageModelFor(taxYear, Month.valueOf(month.toUpperCase), employerRef, request.user).map {
      case Left(HttpParserError(status)) => errorHandler.handleError(status)
      case Left(_) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      case Right(pageModel) => Ok(pageView(pageModel))
    }
  }
}
