package org.allenai.pipeline.spark

import org.allenai.pipeline._
import org.allenai.pipeline.s3.S3Config

import com.amazonaws.auth.BasicAWSCredentials

import scala.reflect.ClassTag

import java.io.File
import java.net.URI

/** Created by rodneykinney on 5/24/15.
  */
object CreateRddArtifacts {
  val fromFileUrls: UrlToArtifact = new UrlToArtifact {
    def urlToArtifact[A <: Artifact: ClassTag]: PartialFunction[URI, A] = {
      val c = implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]
      val fn: PartialFunction[URI, A] = {
        case url if c.isAssignableFrom(classOf[PartitionedRddArtifact])
          && "file" == url.getScheme =>
          new PartitionedRddFileArtifact(new File(url)).asInstanceOf[A]
        case url if c.isAssignableFrom(classOf[PartitionedRddArtifact])
          && null == url.getScheme =>
          new PartitionedRddFileArtifact(new File(url.getPath)).asInstanceOf[A]
      }
      fn
    }
  }

  def fromS3Urls(credentials: => BasicAWSCredentials): UrlToArtifact = new UrlToArtifact {
    def urlToArtifact[A <: Artifact: ClassTag]: PartialFunction[URI, A] = {
      val c = implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]
      val fn: PartialFunction[URI, A] = {
        case url if c.isAssignableFrom(classOf[PartitionedRddArtifact])
          && List("s3", "s3n").contains(url.getScheme) =>
          val bucket = url.getHost
          val path = url.getPath.dropWhile(_ == '/')
          new PartitionedRddS3Artifact(S3Config(bucket, credentials), path).asInstanceOf[A]
      }
      fn
    }
  }

  def fromFileOrS3Urls(credentials: => BasicAWSCredentials) =
    UrlToArtifact.chain(fromFileUrls, fromS3Urls(credentials))
}
