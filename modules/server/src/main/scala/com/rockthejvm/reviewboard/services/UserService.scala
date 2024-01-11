package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.User
import com.rockthejvm.reviewboard.repositories.UserRepository
import zio.*

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait UserService {
  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
}

class UserServiceLive private (repo: UserRepository) extends UserService {

  override def registerUser(email: String, password: String): Task[User] =
    repo.create(
      User(
        id = -1L,
        email = email,
        hashedPassword = UserServiceLive.Hasher.generateHash(password)
      )
    )

  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      existingUser <- repo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"Cannot verify the user ${email}: non existent"))
      result <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.email)
      )
    } yield result

}

object UserServiceLive {
  val layer: URLayer[UserRepository, UserServiceLive] = ZLayer {
    ZIO.serviceWith[UserRepository](repo => new UserServiceLive(repo))
  }

  object Hasher {

    private val PBKDF2_ALGORITHM  = "PBKDF2WithHmacSHA512"
    private val PBKDF2_ITERATIONS = 1000
    private val SALT_BYTE_SIZE    = 24
    private val HASH_BYTE_SIZE    = 24
    private val skf               = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

    private def pbkdf2(message: Array[Char], salt: Array[Byte], iterations: Int, nBytes: Int): Array[Byte] = {
      val keySpec: PBEKeySpec = new PBEKeySpec(message, salt, iterations, nBytes * 8)
      skf.generateSecret(keySpec).getEncoded
    }

    private def toHex(array: Array[Byte]): String =
      array.map(b => "%02X".format(b)).mkString

    private def fromHex(string: String): Array[Byte] = {
      string.sliding(2, 2).toArray.map { hexValue =>
        Integer.parseInt(hexValue, 16).toByte
      }
    }

    // a(i) ^ b(i) for every i
    private def compareBytes(a: Array[Byte], b: Array[Byte]): Boolean = {
      val range = 0 until math.min(a.length, b.length)
      val diff = range.foldLeft(a.length ^ b.length) { case (acc, i) =>
        acc | (a(i) ^ b(i))
      }
      diff == 0
    }

    // string + salt + nIterations PBKDF2
    def generateHash(string: String): String = {
      val rng: SecureRandom = new SecureRandom()
      val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
      rng.nextBytes(salt) // creates 24 random bytes
      val hashBytes = pbkdf2(string.toCharArray, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
      s"$PBKDF2_ITERATIONS:${toHex(salt)}:${toHex(hashBytes)}"
    }

    def validateHash(string: String, hash: String): Boolean = {
      val hashSegments = hash.split(":")
      val nIterations  = hashSegments(0).toInt
      val salt         = fromHex(hashSegments(1))
      val validHash    = fromHex(hashSegments(2))
      val testHash     = pbkdf2(string.toCharArray, salt, nIterations, HASH_BYTE_SIZE)
      compareBytes(testHash, validHash)
    }
  }
}

object UserServiceDemo {
  def main(args: Array[String]): Unit = {
    println(UserServiceLive.Hasher.generateHash("rockthejvm"))
    println(
      UserServiceLive.Hasher.validateHash(
        "rockthejvm",
        "1000:6BDF049DD0D7F8D60583291270E9F5DC39AE82CE81FA79B1:7BC8C1162CC36C0A04394C67787853C4974AE43565251018"
      )
    )
  }
}
