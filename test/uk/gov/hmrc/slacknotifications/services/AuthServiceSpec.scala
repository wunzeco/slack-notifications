/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.slacknotifications.services

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}
import play.api.Configuration

class AuthServiceSpec extends WordSpec with Matchers {

  "Checking if user is authorised" should {

    "return true if the service is present in the configuration" in {
      val service = Service("foo", "bar")

      val typesafeConfig = ConfigFactory.parseString(
        s"""
          auth {
            enabled = true
            authorizedServices = [
              {
                name = ${service.name}
                password = ${service.password}
              }
            ]
          }
         """
      )

      val configuration = Configuration(typesafeConfig)

      val authService = new AuthService(configuration)

      authService.isAuthorized(service) shouldBe true
    }

    "return true if the service is present in the configuration (app-config-* style)" in {
      val service = Service("foo", "bar")

      val configuration =
        Configuration(
          "auth.enabled"                       -> true,
          "auth.authorizedServices.0.name"     -> service.name,
          "auth.authorizedServices.0.password" -> service.password
        )

      val authService = new AuthService(configuration)

      authService.isAuthorized(service) shouldBe true
    }

    "return false if no matching service is found in config" in {
      val service = Service("foo", "bar")
      val configuration =
        Configuration(
          "auth.enabled"                       -> true,
          "auth.authorizedServices.0.name"     -> service.name,
          "auth.authorizedServices.0.password" -> service.password
        )

      val authService = new AuthService(configuration)

      val anotherServiceNotInConfig = Service("x", "y")

      authService.isAuthorized(anotherServiceNotInConfig) shouldBe false
    }

    "return true if auth not enabled" in {
      val typesafeConfig = ConfigFactory.parseString(
        s"""
          auth {
            enabled = false
            authorizedServices = []
          }
         """
      )

      val authService = new AuthService(Configuration(typesafeConfig))

      authService.isAuthorized(Service("foo", "bar")) shouldBe true
    }

  }

}
