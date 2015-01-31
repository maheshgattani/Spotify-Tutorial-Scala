package controllers

import play.api.mvc._
import scalaj.http._
import spray.json._
import DefaultJsonProtocol._

object Spotify extends Controller {

  val clientId = "3b5d1c5cc6294098af49e41d8e1dee98" // Your client id
  val clientSecret = "67943ddb8bd34593a9001f3abdf3690f" // Your client secret
  val redirectUri = "https://mysterious-atoll-4197.herokuapp.com/callback" // Your redirect uri

  val authUrl = "https://accounts.spotify.com/authorize"
  val accountAuthUrl = "https://accounts.spotify.com/authorize?"
  val tokenUrl = "https://accounts.spotify.com/api/token"
  val meUrl = "https://api.spotify.com/v1/me"

  val stateKey = "spotify_auth_state"

  val getEncodedAuthKey : String = {
    // use https://www.base64encode.org/ to encode clientId:clientSecret
    "M2I1ZDFjNWNjNjI5NDA5OGFmNDllNDFkOGUxZGVlOTg6Njc5NDNkZGI4YmQzNDU5M2E5MDAxZjNhYmRmMzY5MGY="
  }

  def login = Action {
    val state = generateRandomString(16)
    val scope = "user-read-private user-read-email"

    Redirect(authUrl,
      Map(
        "response_type" -> Seq("code"),
        "client_id" -> Seq(clientId),
        "scope" -> Seq(scope),
        "redirect_uri" -> Seq(redirectUri),
        "state" -> Seq(state)
      )
    ).withCookies(Cookie(stateKey, state))
  }

  def callback = Action { implicit request =>
    val code = request.queryString.get("code").getOrElse(Seq("")).head
    var state = request.queryString.get("state")
    val storedState = request.cookies.get(stateKey).get.value

    if (state.isEmpty || state.get.head != storedState) {
      Redirect("/", Map("error" -> Seq("state_mismatch")))
    }
    else {
      val authKey = clientId + ":" + clientSecret
      val authKeyEncoded = getEncodedAuthKey
      val token : HttpResponse[String] =
        Http(tokenUrl)
          .postForm(
            Seq("code" -> code, "redirect_uri" -> redirectUri, "grant_type" -> "authorization_code")
          )
          .headers(Map("Authorization" -> ("Basic " + authKeyEncoded)))
          .asString

      if (token.isSuccess && token.code == 200) {
        val tokenResponse = token.body.parseJson.convertTo[Map[String, Either[Int, String]]]
        val accessToken = tokenResponse.get("access_token").get.right.get
        val refreshToken = tokenResponse.get("refresh_token").get.right.get

        val spotifyUserProfile : HttpResponse[String] =
          Http(meUrl)
            .headers(Map("Authorization" -> ("Bearer " + accessToken)))
            .asString

        val data = spotifyUserProfile.body.parseJson.asJsObject().getFields("display_name", "email", "href", "id").map(_.toString())

        Ok(views.html.loggedin(data(0), data(1), data(2), data(3)))
      }
      else {
        Redirect("/", Map("error" -> Seq("invalid_token"))).discardingCookies(DiscardingCookie(stateKey))
      }
    }
  }

  def refreshToken = Action { implicit request =>
    val authKey = clientId + ":" + clientSecret
    val authKeyEncoded = getEncodedAuthKey
    val refreshToken = request.queryString.get("refresh_token").getOrElse(Seq("")).head
    val token : HttpResponse[String] =
      Http(tokenUrl)
        .postForm(
          Seq("refresh_token" -> refreshToken, "grant_type" -> "refresh_token")
        )
        .headers(Map("Authorization" -> ("Basic " + authKeyEncoded)))
        .asString

    if (token.isSuccess && token.code == 200) {
      val tokenResponse = token.body.parseJson.convertTo[Map[String, Either[Int, String]]]
      val accessToken = tokenResponse.get("access_token").get.right.get
      Ok(Map("access_token" -> accessToken).toJson.toString)
    }
    else {
      Ok
    }
  }

  private def generateRandomString(length: Int) : String = {
    var text = ""
    val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    val range = Range(1, 10)
    range.map { r =>
      text += possible.charAt(Math.floor(Math.random() * possible.length).toInt)
    }

    text
  }
}