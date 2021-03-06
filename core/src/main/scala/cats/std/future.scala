package cats
package std

import cats.syntax.all._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

trait FutureInstances extends FutureInstances1 {

  implicit def futureInstance(implicit ec: ExecutionContext): MonadError[Lambda[(E, A) => Future[A]], Throwable] with CoflatMap[Future] =
    new FutureCoflatMap with MonadError[Lambda[(E, A) => Future[A]], Throwable]{
      def pure[A](x: A): Future[A] = Future.successful(x)

      override def pureEval[A](x: Eval[A]): Future[A] = Future(x.value)

      def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

      def handleError[A](fea: Future[A])(f: Throwable => Future[A]): Future[A] = fea.recoverWith { case t => f(t) }

      def raiseError[A](e: Throwable): Future[A] = Future.failed(e)

      override def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)
    }

  implicit def futureGroup[A: Group](implicit ec: ExecutionContext): Group[Future[A]] =
    new FutureGroup[A]
}

trait FutureInstances1 extends FutureInstances2 {
  implicit def futureMonoid[A: Monoid](implicit ec: ExecutionContext): Monoid[Future[A]] =
    new FutureMonoid[A]
}

trait FutureInstances2 {
  implicit def futureSemigroup[A: Semigroup](implicit ec: ExecutionContext): Semigroup[Future[A]] =
    new FutureSemigroup[A]
}

private[cats] abstract class FutureCoflatMap(implicit ec: ExecutionContext) extends CoflatMap[Future] {
  def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)
  def coflatMap[A, B](fa: Future[A])(f: Future[A] => B): Future[B] = Future(f(fa))
}

private[cats] class FutureSemigroup[A: Semigroup](implicit ec: ExecutionContext) extends Semigroup[Future[A]] {
  def combine(fx: Future[A], fy: Future[A]): Future[A] =
    (fx zip fy).map { case (x, y) => x |+| y }
}

private[cats] class FutureMonoid[A](implicit A: Monoid[A], ec: ExecutionContext) extends FutureSemigroup[A] with Monoid[Future[A]] {
  def empty: Future[A] =
    Future.successful(A.empty)
}

private[cats] class FutureGroup[A](implicit A: Group[A], ec: ExecutionContext) extends FutureMonoid[A] with Group[Future[A]] {
  def inverse(fx: Future[A]): Future[A] =
    fx.map(_.inverse)
  override def remove(fx: Future[A], fy: Future[A]): Future[A] =
    (fx zip fy).map { case (x, y) => x |-| y }
}
