package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.config.{Configs, EmailServiceConfig}
import zio.*

import java.util.Properties
import javax.mail.*
import javax.mail.internet.MimeMessage

trait EmailService {

  def sendEmail(to: String, subject: String, content: String): Task[Unit]

  def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] = {
    val subject = "Rock the JVM: Password Recovery"
    val content = s"""
      <div style="
        border: 1px solid black;
        padding: 20px;
        font-family: sans-serif;
        line-height:2;
        font-size: 20px;
      ">
        <h1>Rock the JVM: Password Recovery</h1>
        <p>Your password recovery token is: <strong>$token</strong></p>
        <p>😘 from Rock the JVM</p>
      </div
    """
    sendEmail(to, subject, content)
  }

}

class EmailServiceLive private (config: EmailServiceConfig) extends EmailService {

  private val propsResource: Task[Properties] = {
    val prop = new Properties
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", "true")
    prop.put("mail.smtp.host", config.host)
    prop.put("mail.smtp.port", config.port)
    prop.put("mail.smtp.ssl.trust", config.host)
    ZIO.succeed(prop)
  }

  private def createSession(prop: Properties): Task[Session] = ZIO.attempt {
    Session.getInstance(
      prop,
      new Authenticator {
        override def getPasswordAuthentication: PasswordAuthentication =
          new PasswordAuthentication(config.user, config.passwd)
      }
    )
  }

  private def createMessage(
      session: Session
  )(from: String, to: String, subject: String, content: String): Task[MimeMessage] = {
    val message = new MimeMessage(session)
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    ZIO.succeed(message)
  }

  override def sendEmail(to: String, subject: String, content: String): Task[Unit] = {
    val messageZIO = for {
      prop    <- propsResource
      session <- createSession(prop)
      message <- createMessage(session)("daniel@rockthejvm.com", to, subject, content)
    } yield message
    messageZIO.map(message => Transport.send(message))
  }

}

object EmailServiceLive {
  val layer: ZLayer[EmailServiceConfig, Nothing, EmailServiceLive] = ZLayer {
    ZIO.serviceWith[EmailServiceConfig](config => new EmailServiceLive(config))
  }
  val configuredLayer: Layer[Config.Error, EmailServiceLive] =
    ZLayer.fromZIO(Configs.makeConfig[EmailServiceConfig]("rockthejvm.email")) >>> layer
}

object EmailServiceDemo extends ZIOAppDefault {

  val program: RIO[EmailService, Unit] = for {
    emailService <- ZIO.service[EmailService]
    _            <- emailService.sendPasswordRecoveryEmail("spiderman@rockthejvm.com", "ABCD1234")
    _            <- Console.printLine("Email sent")
  } yield ()

  override def run: ZIO[Any & ZIOAppArgs with Scope, Any, Any] = program.provide(EmailServiceLive.configuredLayer)
}
